import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface CategoryGridProps {
  children: ReactNode;
  /** Extra classes for the scroll container (e.g. a different max height). */
  className?: string;
}

/**
 * The shared 4-column category tile grid. Fixed max height with a visible thin scrollbar and a
 * flat "Scroll for N more" hint while tiles are hidden below the fold. Mark the selected tile
 * with `data-selected="true"` and it is scrolled into view on mount.
 */
export function CategoryGrid({ children, className }: CategoryGridProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [hidden, setHidden] = useState(0);

  const measure = useCallback(() => {
    const el = ref.current;
    if (!el) return;
    const bottom = el.scrollTop + el.clientHeight;
    let count = 0;
    for (const child of Array.from(el.children) as HTMLElement[]) {
      if (child.offsetTop + child.offsetHeight / 2 > bottom + 1) count += 1;
    }
    setHidden(count);
  }, []);

  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    const selected = el.querySelector<HTMLElement>('[data-selected="true"]');
    if (selected) {
      // Scroll only the grid itself, never the page.
      el.scrollTop = Math.max(
        0,
        selected.offsetTop - (el.clientHeight - selected.offsetHeight) / 2,
      );
    }
    measure();
  }, [measure]);

  useEffect(() => {
    const el = ref.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, [measure]);

  // Tile count can change (type switch, rules) without a resize.
  useEffect(() => {
    measure();
  });

  return (
    <div>
      <div
        ref={ref}
        onScroll={measure}
        className={cn('relative grid max-h-54 grid-cols-4 gap-2 overflow-y-auto pr-1', className)}
      >
        {children}
      </div>
      {hidden > 0 && (
        <p className="text-muted-foreground mt-1.5 text-center text-[11px]">
          Scroll for {hidden} more
        </p>
      )}
    </div>
  );
}
