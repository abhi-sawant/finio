<?php
declare(strict_types=1);

namespace Finio\Controllers;

use Finio\Config;
use Finio\Database;
use function Finio\json_ok;
use function Finio\json_error;
use function Finio\request_body;
use function Finio\jwt_create;
use function Finio\generate_token;
use function Finio\send_mail;

class AuthController
{
    // ── POST /auth/register ───────────────────────────────────────────────────
    public function register(array $params): void
    {
        $body = request_body();
        $name  = trim($body['name']  ?? '');
        $email = strtolower(trim($body['email'] ?? ''));
        $pass  = $body['password'] ?? '';

        // Validation
        if (empty($name)) {
            json_error('Name is required.');
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            json_error('A valid email address is required.');
        }
        if (strlen($pass) < 8) {
            json_error('Password must be at least 8 characters.');
        }

        $pdo = Database::connect();

        // Check uniqueness
        $stmt = $pdo->prepare('SELECT id FROM users WHERE email = ?');
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            json_error('An account with this email already exists.', 409);
        }

        // Create user
        $hash  = password_hash($pass, PASSWORD_BCRYPT, ['cost' => 12]);
        $token = generate_token(32);   // 64-char hex

        $stmt = $pdo->prepare(
            'INSERT INTO users (name, email, password_hash, is_verified, verification_token)
             VALUES (?, ?, ?, 0, ?)'
        );
        $stmt->execute([$name, $email, $hash, $token]);

        // Send verification email
        $this->sendVerificationEmail($email, $name, $token);

        json_ok(['message' => 'Account created. Please check your email to verify your account.'], 201);
    }

    // ── GET /auth/verify?token=xxx ────────────────────────────────────────────
    public function verify(array $params): void
    {
        $token = trim($_GET['token'] ?? '');

        if (empty($token)) {
            json_error('Verification token is missing.');
        }

        $pdo  = Database::connect();
        $stmt = $pdo->prepare(
            'SELECT id FROM users WHERE verification_token = ? AND is_verified = 0'
        );
        $stmt->execute([$token]);
        $user = $stmt->fetch();

        if (!$user) {
            json_error('Invalid or already-used verification token.', 404);
        }

        $pdo->prepare(
            'UPDATE users SET is_verified = 1, verification_token = NULL WHERE id = ?'
        )->execute([$user['id']]);

        // Return a simple HTML page so the browser shows a success message
        // (the user clicks the link in their email, which opens in a browser)
        header('Content-Type: text/html; charset=utf-8');
        echo <<<HTML
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"><title>Email Verified – Finio</title>
        <style>body{font-family:sans-serif;text-align:center;padding:60px;color:#333}
        h1{color:#22c55e}</style></head>
        <body>
          <h1>✓ Email verified!</h1>
          <p>Your Finio account is now active. You can close this tab and log in from the app.</p>
        </body>
        </html>
        HTML;
        exit;
    }

    // ── POST /auth/login ──────────────────────────────────────────────────────
    public function login(array $params): void
    {
        $body  = request_body();
        $email = strtolower(trim($body['email'] ?? ''));
        $pass  = $body['password'] ?? '';

        if (empty($email) || empty($pass)) {
            json_error('Email and password are required.');
        }

        $pdo  = Database::connect();
        $stmt = $pdo->prepare('SELECT * FROM users WHERE email = ?');
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        // Use a constant-time comparison; also fail gracefully if no user found
        $dummyHash = '$2y$12$invalidsaltXXXXXXXXXXXuInvalidHashPaddingXXXXXXXXXXXX';
        $hash      = $user ? $user['password_hash'] : $dummyHash;

        if (!password_verify($pass, $hash) || !$user) {
            json_error('Invalid email or password.', 401);
        }

        if (!$user['is_verified']) {
            json_error('Please verify your email before logging in.', 403);
        }

        $token = jwt_create((int)$user['id'], $user['email'], $user['name']);

        json_ok([
            'token' => $token,
            'user'  => [
                'id'    => (int)$user['id'],
                'name'  => $user['name'],
                'email' => $user['email'],
            ],
        ]);
    }

    // ── POST /auth/forgot-password ────────────────────────────────────────────
    public function forgotPassword(array $params): void
    {
        $body  = request_body();
        $email = strtolower(trim($body['email'] ?? ''));

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            json_error('A valid email address is required.');
        }

        $pdo  = Database::connect();
        $stmt = $pdo->prepare('SELECT id, name FROM users WHERE email = ?');
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        // Always return success to prevent email enumeration
        if ($user) {
            $token   = generate_token(32);      // 64-char hex
            $hash    = hash('sha256', $token);  // store only the hash in DB
            $expires = date('Y-m-d H:i:s', time() + 3600); // 1 hour

            $pdo->prepare(
                'UPDATE users SET reset_token_hash = ?, reset_token_expires = ? WHERE id = ?'
            )->execute([$hash, $expires, $user['id']]);

            $this->sendPasswordResetEmail($email, $user['name'], $token);
        }

        json_ok(['message' => 'If an account with that email exists, a reset link has been sent.']);
    }

    // ── POST /auth/reset-password ─────────────────────────────────────────────
    public function resetPassword(array $params): void
    {
        $body     = request_body();
        $token    = trim($body['token']    ?? '');
        $newPass  = $body['password'] ?? '';

        if (empty($token)) {
            json_error('Reset token is required.');
        }
        if (strlen($newPass) < 8) {
            json_error('Password must be at least 8 characters.');
        }

        $hash = hash('sha256', $token);
        $pdo  = Database::connect();

        $stmt = $pdo->prepare(
            'SELECT id FROM users
             WHERE reset_token_hash = ?
               AND reset_token_expires > NOW()'
        );
        $stmt->execute([$hash]);
        $user = $stmt->fetch();

        if (!$user) {
            json_error('Reset link is invalid or has expired.', 404);
        }

        $newHash = password_hash($newPass, PASSWORD_BCRYPT, ['cost' => 12]);

        $pdo->prepare(
            'UPDATE users
             SET password_hash = ?, reset_token_hash = NULL, reset_token_expires = NULL
             WHERE id = ?'
        )->execute([$newHash, $user['id']]);

        json_ok(['message' => 'Password has been reset. You can now log in.']);
    }

    // ── Email helpers ─────────────────────────────────────────────────────────

    private function sendVerificationEmail(string $email, string $name, string $token): void
    {
        $url = Config::get('app_url') . '/auth/verify?token=' . urlencode($token);

        $html = <<<HTML
        <p>Hi {$name},</p>
        <p>Welcome to Finio! Please click the button below to verify your email address.</p>
        <p style="text-align:center;margin:32px 0">
          <a href="{$url}"
             style="background:#6366f1;color:#fff;padding:14px 28px;border-radius:8px;
                    text-decoration:none;font-weight:600">
            Verify Email
          </a>
        </p>
        <p>Or copy this link into your browser:<br><a href="{$url}">{$url}</a></p>
        <p>This link does not expire.</p>
        <p>– Finio</p>
        HTML;

        send_mail($email, $name, 'Verify your Finio email', $html);
    }

    private function sendPasswordResetEmail(string $email, string $name, string $token): void
    {
        $url = Config::get('app_url') . '/auth/reset-password?token=' . urlencode($token);

        $html = <<<HTML
        <p>Hi {$name},</p>
        <p>We received a request to reset your Finio password. Click below to set a new one.</p>
        <p style="text-align:center;margin:32px 0">
          <a href="{$url}"
             style="background:#6366f1;color:#fff;padding:14px 28px;border-radius:8px;
                    text-decoration:none;font-weight:600">
            Reset Password
          </a>
        </p>
        <p>Or copy this link into your browser:<br><a href="{$url}">{$url}</a></p>
        <p>This link expires in <strong>1 hour</strong>.</p>
        <p>If you didn't request a reset, you can ignore this email.</p>
        <p>– Finio</p>
        HTML;

        send_mail($email, $name, 'Reset your Finio password', $html);
    }
}
