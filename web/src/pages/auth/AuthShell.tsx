import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';
import { Button } from '@/components/ui/button';

/**
 * Shared frame for every cloud-account screen: the Finio wordmark, a heading, the form, and a
 * way out. The app works fully signed-out, so each of these screens must offer to leave the
 * flow — not only Login.
 */
export function AuthShell({
  heading,
  description,
  children,
  footer,
}: {
  heading: string;
  description?: ReactNode;
  children: ReactNode;
  /** Rendered between the form and the "Continue without an account" escape. */
  footer?: ReactNode;
}) {
  const navigate = useNavigate();
  return (
    <div className="flex min-h-dvh flex-col justify-center px-6 py-12">
      <div className="mx-auto w-full max-w-sm">
        <div className="mb-8 text-center">
          <p className="text-grad-primary text-4xl font-extrabold" aria-hidden="true">
            Finio
          </p>
          <h1 className="text-foreground mt-3 text-xl font-bold">{heading}</h1>
          {description && <div className="text-muted-foreground mt-2 text-sm">{description}</div>}
        </div>

        {children}

        {footer && <div className="mt-6 text-center text-sm">{footer}</div>}

        <Button
          variant="ghost"
          onClick={() => navigate('/', { replace: true })}
          className="text-muted-foreground hover:text-foreground mt-4 w-full"
        >
          Continue without an account
        </Button>
      </div>
    </div>
  );
}
