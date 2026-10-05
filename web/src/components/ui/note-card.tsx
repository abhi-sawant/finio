import { useRef } from 'react';
import { cn } from '@/lib/utils';
import { Guilloche } from './guilloche';

// Security fibres, scattered once with a fixed seed so they never move between renders. They are
// invisible on the daylight note and fluoresce in dark mode — the "note under a UV lamp".
const FIBRE_INKS = ['#7ff3ff', '#ff8fd0', '#fff27a'];
const FIBRES = (() => {
  let seed = 7;
  const rand = () => ((seed = (seed * 16807) % 2147483647) - 1) / 2147483646;
  return Array.from({ length: 26 }, (_, i) => {
    // right third only — the watermark side — so no fibre reaches a figure, even at 320px
    const x = 272 + rand() * 124;
    const y = rand() * 200;
    const a = rand() * Math.PI * 2;
    const len = 5 + rand() * 9;
    const bend = (rand() - 0.5) * 8;
    const x2 = x + Math.cos(a) * len;
    const y2 = y + Math.sin(a) * len;
    const cx = (x + x2) / 2 - Math.sin(a) * bend;
    const cy = (y + y2) / 2 + Math.cos(a) * bend;
    return {
      d: `M${x.toFixed(1)} ${y.toFixed(1)}Q${cx.toFixed(1)} ${cy.toFixed(1)} ${x2.toFixed(1)} ${y2.toFixed(1)}`,
      ink: FIBRE_INKS[i % FIBRE_INKS.length],
    };
  });
})();

const MICROPRINT = 'FINIO · KEPT ON THIS DEVICE · NOT A BANK · YOUR DATA STAYS YOURS · '.repeat(6);

/**
 * The Mudra hero surface: a banknote. Guilloche rosette, a windowed security thread whose hue
 * shifts as the pointer crosses the note (the way a real thread turns green → blue when tilted),
 * and a microprint band that repeats the privacy promise along the bottom edge.
 *
 * Content keeps clear of the thread via `pr-20`; callers lay out the inside freely.
 */
export function NoteCard({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  const onMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const el = ref.current;
    if (!el || e.pointerType === 'touch') return;
    const r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    el.style.setProperty('--shift', `${Math.round(x * 140)}deg`);
    el.style.setProperty('--rx', `${((0.5 - y) * 4).toFixed(2)}deg`);
    el.style.setProperty('--ry', `${((x - 0.5) * 6).toFixed(2)}deg`);
  };
  const onLeave = () => {
    ref.current?.style.setProperty('--rx', '0deg');
    ref.current?.style.setProperty('--ry', '0deg');
  };

  return (
    <div
      className={cn('note-card-stage', className)}
      onPointerMove={onMove}
      onPointerLeave={onLeave}
    >
      <div ref={ref} className="note-card">
        <svg
          className="note-card-fibres"
          viewBox="0 0 400 200"
          preserveAspectRatio="none"
          aria-hidden
          focusable="false"
        >
          {FIBRES.map((f, i) => (
            <path
              key={i}
              d={f.d}
              stroke={f.ink}
              color={f.ink}
              strokeWidth={1.2}
              vectorEffect="non-scaling-stroke"
              fill="none"
              strokeLinecap="round"
            />
          ))}
        </svg>
        <Guilloche className="note-card-rosette" />
        <div className="note-card-thread" aria-hidden />
        <div className="relative z-1 pr-20 lg:max-w-lg lg:pr-0">{children}</div>
        <div className="note-card-microprint" aria-hidden>
          {MICROPRINT}
        </div>
      </div>
    </div>
  );
}
