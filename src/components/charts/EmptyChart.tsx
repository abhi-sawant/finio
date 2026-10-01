import { LineChart } from 'lucide-react';

interface Props {
  /** One line on what is missing, e.g. "Add a few days of transactions to see a trend." */
  message?: string;
  className?: string;
}

/** Placeholder for a chart that can't draw yet — a single point is a dot, not a trend. */
export function EmptyChart({
  message = 'Not enough data yet — check back after a few more entries.',
  className = 'h-44 lg:h-64',
}: Props) {
  return (
    <div
      className={`text-muted-foreground flex flex-col items-center justify-center gap-2 text-center text-xs ${className}`}
    >
      <LineChart size={20} aria-hidden="true" />
      <p className="max-w-56">{message}</p>
    </div>
  );
}
