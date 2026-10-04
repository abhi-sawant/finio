import { cn } from '@/lib/utils';

/** Key styling, exported so a `leadingAction` key (the biometric button) matches the pad. */
export function pinKeyClass(surface: 'background' | 'card' = 'background') {
  return cn(
    'flex h-14 items-center justify-center rounded-md border text-xl font-semibold transition-all active:scale-95 select-none disabled:opacity-40',
    surface === 'card'
      ? 'bg-secondary text-secondary-foreground border-border hover:bg-secondary/80 active:bg-muted'
      : 'active:bg-muted border-[var(--glass-border)] bg-[var(--glass-strong)] shadow-[inset_0_1px_0_var(--glass-highlight)]',
  );
}
