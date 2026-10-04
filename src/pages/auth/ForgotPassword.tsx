import { useState } from 'react';
import { useNavigate, Link } from 'react-router';
import { Mail } from 'lucide-react';
import { api } from '@/services/api';
import { getErrorMessage } from '@/utils/errors';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { isValidEmail } from '@/utils/validation';
import { AuthShell } from './AuthShell';

export default function ForgotPassword() {
  const navigate = useNavigate();

  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      toast.error('Please enter your email');
      return;
    }
    if (!isValidEmail(email)) {
      toast.error('Enter a valid email');
      return;
    }

    setLoading(true);
    try {
      await api.forgotPassword(email.trim());
      toast.success('If an account exists, an OTP has been sent.');
      navigate('/reset-password', { state: { email: email.trim() } });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Something went wrong'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      heading="Forgot password"
      description="Enter your email and we'll send you an OTP to reset your password."
      footer={
        <p className="text-muted-foreground">
          Remember your password?{' '}
          <Link to="/login" className="text-primary font-medium hover:underline">
            Sign in
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <div className="relative">
          <Mail className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
          <Input
            type="email"
            placeholder="Email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full pr-4 pl-11"
            autoComplete="email"
            inputMode="email"
            autoCapitalize="none"
            spellCheck={false}
          />
        </div>

        <Button type="submit" disabled={loading} size="lg" className="w-full">
          {loading ? 'Sending…' : 'Send OTP'}
        </Button>
      </form>
    </AuthShell>
  );
}
