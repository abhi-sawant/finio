import { useState, useRef } from 'react';
import { Link, Navigate, useNavigate, useLocation } from 'react-router';
import { Eye, EyeOff, Lock } from 'lucide-react';
import { api } from '@/services/api';
import { getErrorMessage } from '@/utils/errors';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { AuthShell } from './AuthShell';

export default function ResetPassword() {
  const navigate = useNavigate();
  const location = useLocation();

  const email = (location.state as { email?: string })?.email || '';
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const inputRefs = useRef<(HTMLInputElement | null)[]>([]);

  const handleChange = (index: number, value: string) => {
    if (!/^\d*$/.test(value)) return;
    const newOtp = [...otp];
    newOtp[index] = value.slice(-1);
    setOtp(newOtp);

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }
  };

  const handleKeyDown = (index: number, e: React.KeyboardEvent) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const handlePaste = (e: React.ClipboardEvent) => {
    e.preventDefault();
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
    const newOtp = [...otp];
    for (let i = 0; i < text.length; i++) {
      newOtp[i] = text[i];
    }
    setOtp(newOtp);
    const focusIndex = Math.min(text.length, 5);
    inputRefs.current[focusIndex]?.focus();
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const code = otp.join('');
    if (code.length !== 6) {
      toast.error('Please enter the full 6-digit OTP');
      return;
    }
    if (password.length < 8) {
      toast.error('Password must be at least 8 characters');
      return;
    }
    if (password !== confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    setLoading(true);
    try {
      await api.resetPassword(email, code, password);
      toast.success('Password reset successfully! Please sign in.');
      navigate('/login', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Reset failed'));
    } finally {
      setLoading(false);
    }
  };

  // A render-time navigate() is a no-op; <Navigate> performs the redirect.
  if (!email) return <Navigate to="/forgot-password" replace />;

  return (
    <AuthShell
      heading="Reset password"
      description={
        <p>
          Enter the OTP sent to <span className="text-foreground font-medium">{email}</span> and
          your new password.
        </p>
      }
      footer={
        <p className="text-muted-foreground">
          Remember your password?{' '}
          <Link to="/login" className="text-primary font-medium hover:underline">
            Sign in
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <Label className="text-foreground mb-2 block text-sm font-medium">OTP code</Label>
          <div className="flex justify-center gap-2" onPaste={handlePaste}>
            {otp.map((digit, i) => (
              <Input
                key={i}
                ref={(el) => {
                  inputRefs.current[i] = el;
                }}
                type="text"
                inputMode="numeric"
                maxLength={1}
                value={digit}
                onChange={(e) => handleChange(i, e.target.value)}
                onKeyDown={(e) => handleKeyDown(i, e)}
                className="h-14 w-12 text-center text-xl font-bold"
              />
            ))}
          </div>
        </div>

        <div>
          <Label className="text-foreground mb-2 block text-sm font-medium">New password</Label>
          <div className="relative">
            <Lock className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
            <Input
              type={showPassword ? 'text' : 'password'}
              placeholder="Min 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full pr-11 pl-11"
              autoComplete="new-password"
            />
            <Button
              type="button"
              variant="ghost"
              size="icon"
              onClick={() => setShowPassword(!showPassword)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
              className="text-muted-foreground absolute top-1/2 right-1 -translate-y-1/2 hover:bg-transparent"
            >
              {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
            </Button>
          </div>
        </div>

        <div>
          <Label
            htmlFor="reset-confirm-password"
            className="text-foreground mb-2 block text-sm font-medium"
          >
            Confirm new password
          </Label>
          <div className="relative">
            <Lock className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
            <Input
              id="reset-confirm-password"
              type={showPassword ? 'text' : 'password'}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full pr-4 pl-11"
              autoComplete="new-password"
              aria-invalid={confirmPassword !== '' && confirmPassword !== password}
              aria-describedby={
                confirmPassword !== '' && confirmPassword !== password
                  ? 'reset-confirm-error'
                  : undefined
              }
            />
          </div>
          {confirmPassword !== '' && confirmPassword !== password && (
            <p id="reset-confirm-error" className="text-destructive mt-1 text-xs">
              Passwords do not match
            </p>
          )}
        </div>

        <Button type="submit" disabled={loading} size="lg" className="w-full">
          {loading ? 'Resetting…' : 'Reset password'}
        </Button>
      </form>
    </AuthShell>
  );
}
