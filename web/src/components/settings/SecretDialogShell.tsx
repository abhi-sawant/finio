import type { ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { cn } from '@/lib/utils';

/**
 * Shared chrome for the PIN dialog (AppLockSection) and the backup-passphrase dialogs
 * (BackupSection) — both are "current → enter → confirm" secret-entry phase machines that
 * differ only in what captures the value (PinPad vs a text Input) and in their verification
 * logic, so only the surrounding Dialog/Header/error-row markup is shared here rather than
 * forcing the two state machines together.
 */
export function SecretDialogShell({
  open,
  onOpenChange,
  title,
  description,
  className,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description: string;
  className?: string;
  children: ReactNode;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent
        // Anchored by its top edge, not centred: the PIN/passphrase phases differ in height,
        // and a centred (−50%) dialog would jump vertically on every phase change.
        className={cn(
          'bg-card top-[max(1rem,12dvh)] mx-auto w-11/12 translate-y-0 sm:max-w-sm',
          className,
        )}
      >
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>
        {children}
      </DialogContent>
    </Dialog>
  );
}

/** The `role="alert"` error row repeated across every phase of both dialogs. */
export function SecretDialogError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="text-destructive flex items-center gap-1.5 text-xs font-medium">
      <AlertTriangle size={13} aria-hidden="true" />
      {message}
    </p>
  );
}
