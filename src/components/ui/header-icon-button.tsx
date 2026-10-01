import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';

type HeaderIconButtonProps = Omit<React.ComponentProps<typeof Button>, 'variant' | 'size'> & {
  /** Only the icon colour changes by tone — the shape and fill never do. */
  tone?: 'neutral' | 'primary' | 'destructive';
  /** A toggled-on control (active filters, hidden amounts) fills with the accent. */
  pressed?: boolean;
  'aria-label': string;
};

/**
 * The one icon button every page header uses: a 40px outlined circle with an 18px icon.
 * Back, add, delete, export and the privacy toggle all go through it so they read as one set.
 */
function HeaderIconButton({
  tone = 'neutral',
  pressed,
  className,
  ...props
}: HeaderIconButtonProps) {
  return (
    <Button
      variant="outline"
      size="icon"
      className={cn(
        "bg-card hover:bg-muted size-10 rounded-full [&_svg:not([class*='size-'])]:size-[18px]",
        tone === 'primary' && 'text-primary',
        tone === 'destructive' && 'text-destructive hover:text-destructive',
        pressed && 'bg-primary text-primary-foreground hover:bg-primary/90 border-primary',
        className,
      )}
      {...props}
    />
  );
}

/** Keeps a title centred on pages with a back button but no trailing action. */
function HeaderIconSpacer() {
  return <div className="size-10 shrink-0" aria-hidden="true" />;
}

export { HeaderIconButton, HeaderIconSpacer };
