import { useState, useRef } from 'react';
import { Navigate, useNavigate, useLocation } from 'react-router';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/useAuthStore';
import { getErrorMessage } from '@/utils/errors';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { AuthShell } from './AuthShell';

export default function VerifyOtp() {
  const navigate = useNavigate();
  const location = useLocation();
  const setAuth = useAuthStore((s) => s.setAuth);

  const email = (location.state as { email?: string })?.email || '';
  const [otp, setOtp] = useState(['', '', '', '', '', '']);
  const [loading, setLoading] = useState(false);
  const [resending, setResending] = useState(false);
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

    setLoading(true);
    try {
      const result = await api.verifyOtp(email, code);
      setAuth(result.token, result.user);
      toast.success('Email verified successfully!');
      navigate('/', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Verification failed'));
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    setResending(true);
    try {
      await api.resendOtp(email);
      toast.success('New OTP sent to your email');
    } catch (err) {
      toast.error(getErrorMessage(err, 'Failed to resend OTP'));
    } finally {
      setResending(false);
    }
  };

  // A render-time navigate() is a no-op; <Navigate> performs the redirect.
  if (!email) return <Navigate to="/register" replace />;

  return (
    <AuthShell
      heading="Verify your email"
      description={
        <>
          <p>Enter the 6-digit code sent to</p>
          <p className="text-foreground font-medium">{email}</p>
        </>
      }
      footer={
        <Button
          variant="ghost"
          onClick={handleResend}
          disabled={resending}
          className="text-primary h-auto p-0 text-sm hover:bg-transparent hover:underline disabled:opacity-50"
        >
          {resending ? 'Sending…' : "Didn't receive the code? Resend"}
        </Button>
      }
    >
      <form onSubmit={handleSubmit} className="space-y-6">
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

        <Button type="submit" disabled={loading} size="lg" className="w-full">
          {loading ? 'Verifying…' : 'Verify'}
        </Button>
      </form>
    </AuthShell>
  );
}
