import { useEffect, useState } from 'react';
import { cn } from '@/lib/utils';

type HeaderProps = React.ComponentPropsWithoutRef<'header'> & {
  /** Classes applied to the centered inner content wrapper (e.g. to narrow it on desktop). */
  innerClassName?: string;
};

/**
 * Sits transparent on the note paper at rest, and frosts over (glass + hairline) only once
 * content has scrolled underneath it — a solid band across the top would hide the paper.
 */
function Header({ children, className, innerClassName, ...props }: HeaderProps) {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 4);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  return (
    <header
      className={cn(
        'sticky top-0 z-5 border-b border-transparent transition-[background-color,border-color] duration-200',
        scrolled && 'glass-chrome border-[var(--glass-border)]',
        className,
      )}
      {...props}
    >
      <div
        className={cn(
          'mx-auto flex w-full max-w-5xl items-center justify-between px-3 py-3 lg:px-8',
          innerClassName,
        )}
      >
        {children}
      </div>
    </header>
  );
}

export default Header;
