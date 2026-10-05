import { memo, useMemo } from 'react';

/**
 * Procedural guilloche rosette — the engraved lathe-work every banknote carries. Strokes in
 * `currentColor`, so callers tint it with a text colour. Purely decorative (`aria-hidden`).
 */
export const Guilloche = memo(function Guilloche({
  className,
  petals = 18,
  rings = 7,
}: {
  className?: string;
  petals?: number;
  rings?: number;
}) {
  const paths = useMemo(() => {
    const out: string[] = [];
    for (let k = 0; k < rings; k++) {
      const R = 46 + k * 7;
      const amp = 9 + (k % 3) * 3;
      let d = '';
      for (let i = 0; i <= 360; i += 3) {
        const t = (i * Math.PI) / 180;
        const r = R + amp * Math.sin(petals * t + k * 0.6);
        d += `${i === 0 ? 'M' : 'L'}${(120 + r * Math.cos(t)).toFixed(1)} ${(120 + r * Math.sin(t)).toFixed(1)}`;
      }
      out.push(`${d}Z`);
    }
    return out;
  }, [petals, rings]);

  return (
    <svg viewBox="0 0 240 240" className={className} aria-hidden focusable="false">
      {paths.map((d, i) => (
        <path
          key={i}
          d={d}
          fill="none"
          stroke="currentColor"
          strokeWidth={0.55}
          opacity={0.9 - i * 0.08}
        />
      ))}
    </svg>
  );
});
