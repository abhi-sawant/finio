import { useState } from 'react';
import { useNavigate, Link } from 'react-router';
import { Eye, EyeOff, Mail, Lock, User } from 'lucide-react';
import { api } from '@/services/api';
import { getErrorMessage } from '@/utils/errors';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { MAX_NAME_LENGTH, cleanText, isValidEmail, stripLeading } from '@/utils/validation';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';

export default function Register() {
  const navigate = useNavigate();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreed, setAgreed] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !email || !password) {
      toast.error('Please fill in all fields');
      return;
    }
    if (!isValidEmail(email)) {
      toast.error('Enter a valid email');
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
    if (!agreed) {
      toast.error('Please confirm your age and agree to the Terms of Service and Privacy Policy');
      return;
    }

    setLoading(true);
    try {
      await api.register(cleanText(name, MAX_NAME_LENGTH), email.trim(), password);
      toast.success('Account created! Check your email for the OTP.');
      navigate('/verify-otp', { state: { email: email.trim() } });
    } catch (err) {
      toast.error(getErrorMessage(err, 'Registration failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen flex-col justify-center px-6 py-12">
      <div className="mx-auto w-full max-w-sm">
        <div className="mb-8 text-center">
          <h1 className="text-grad-primary text-4xl font-extrabold">Finio</h1>
          <p className="text-muted-foreground mt-2">Create your account</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          <div className="relative">
            <User className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
            <Input
              type="text"
              placeholder="Name"
              value={name}
              maxLength={MAX_NAME_LENGTH}
              onChange={(e) => setName(stripLeading(e.target.value))}
              className="w-full pr-4 pl-11"
              autoComplete="name"
            />
          </div>

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
              placeholder="Password (min 8 characters)"
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

          <div className="relative">
            <Lock className="text-muted-foreground absolute top-1/2 left-3 z-10 h-5 w-5 -translate-y-1/2" />
            <Input
              type={showPassword ? 'text' : 'password'}
              placeholder="Confirm password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              className="w-full pr-4 pl-11"
              autoComplete="new-password"
              aria-invalid={confirmPassword !== '' && confirmPassword !== password}
            />
            {confirmPassword !== '' && confirmPassword !== password && (
              <p className="text-destructive mt-1 text-xs">Passwords do not match</p>
            )}
          </div>

          <Label htmlFor="agree-terms" className="items-start gap-2 font-normal">
            <Checkbox
              id="agree-terms"
              checked={agreed}
              onCheckedChange={(checked) => setAgreed(checked === true)}
              className="mt-0.5"
            />
            <span className="text-muted-foreground text-sm">
              I confirm I am at least 16 years old and agree to the{' '}
              <Link to="/terms" target="_blank" className="text-primary hover:underline">
                Terms of Service
              </Link>{' '}
              and{' '}
              <Link to="/privacy" target="_blank" className="text-primary hover:underline">
                Privacy Policy
              </Link>
            </span>
          </Label>

          <Button
            type="submit"
            disabled={loading || !agreed}
            className="bg-grad-primary shadow-glow-primary h-auto w-full rounded-sm py-3 font-semibold text-white disabled:opacity-50"
          >
            {loading ? 'Creating account...' : 'Sign Up'}
          </Button>
        </form>

        <p className="text-muted-foreground mt-6 text-center text-sm">
          Already have an account?{' '}
          <Link to="/login" className="text-primary font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
