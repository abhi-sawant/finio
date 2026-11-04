import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';
import { VISUAL_THEMES, useVisualThemeStore } from '@/store/useVisualThemeStore';

function Strip({ colors, label }: { colors: readonly string[]; label: string }) {
  return (
    <span
      role="img"
      aria-label={label}
      className="border-border flex h-5 flex-1 overflow-hidden rounded-[5px] border"
    >
      {colors.map((c, i) => (
        <i key={i} className="flex-1" style={{ background: c }} />
      ))}
    </span>
  );
}

/**
 * The visual-theme chooser: one radio per world, each with its light and dark palette and a line
 * on its personality. A theme changes shape, type, density and motion as well as colour, so a
 * name and a swatch alone would undersell it. Light/dark is the separate "Mode" row above.
 */
export function VisualThemePicker() {
  const visualTheme = useVisualThemeStore((s) => s.visualTheme);
  const setVisualTheme = useVisualThemeStore((s) => s.setVisualTheme);

  return (
    <div className="p-4">
      <p className="text-sm font-medium" id="visual-theme-label">
        Style
      </p>
      <p className="text-muted-foreground mb-3 text-xs">
        How Finio looks and feels — shape, type, density, not just colour
      </p>
      <div role="radiogroup" aria-labelledby="visual-theme-label" className="grid gap-2">
        {VISUAL_THEMES.map((t) => {
          const selected = t.value === visualTheme;
          return (
            <button
              key={t.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => setVisualTheme(t.value)}
              className={cn(
                'flex flex-col gap-2 rounded-md border p-3 text-left transition-colors',
                selected ? 'border-primary bg-accent' : 'border-border hover:bg-muted/60',
              )}
            >
              <span className="flex items-center gap-2">
                <span className="font-heading text-base font-semibold">{t.label}</span>
                {selected && <Check size={15} className="text-primary" aria-hidden />}
              </span>
              <span className="text-muted-foreground text-xs">{t.tagline}</span>
              <span className="flex gap-2">
                <Strip colors={t.swatch} label={`${t.label} light palette`} />
                <Strip colors={t.swatchDark} label={`${t.label} dark palette`} />
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
