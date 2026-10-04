import { useState } from 'react';
import { useNavigate, Link } from 'react-router';
import { Eye, EyeOff, Mail, Lock } from 'lucide-react';
import { api } from '@/services/api';
import { useAuthStore } from '@/store/useAuthStore';
import { getErrorMessage } from '@/utils/errors';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { isValidEmail } from '@/utils/validation';
import { AuthShell } from './AuthShell';

export default function Login() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      toast.error('Please fill in all fields');
      return;
    }
    if (!isValidEmail(email)) {
      toast.error('Enter a valid email');
      return;
    }

    setLoading(true);
    try {
      const result = await api.login(email.trim(), password);
      setAuth(result.token, result.user);
      toast.success('Logged in successfully');
      navigate('/', { replace: true });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Login failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthShell
      heading="Sign in to your account"
      footer={
        <p className="text-muted-foreground">
          Don't have an account?{' '}
          <Link to="/register" className="text-primary font-medium hover:underline">
            Sign up
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

        <div className="relative">
          <Lock className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
          <Input
            type={showPassword ? 'text' : 'password'}
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full pr-11 pl-11"
            autoComplete="current-password"
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

        <div className="text-right">
          <Link to="/forgot-password" className="text-primary text-sm hover:underline">
            Forgot password?
          </Link>
        </div>

        <Button type="submit" disabled={loading} size="lg" className="w-full">
          {loading ? 'Signing in…' : 'Sign in'}
        </Button>
      </form>
    </AuthShell>
  );
}
