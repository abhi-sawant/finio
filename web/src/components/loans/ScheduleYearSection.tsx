import { memo } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { formatCurrency, formatShortDate } from '@/utils/formatters';
import type { ScheduleYearGroup } from '@/utils/loan';
import { cn } from '@/lib/utils';

interface ScheduleYearSectionProps {
  group: ScheduleYearGroup;
  expanded: boolean;
  onToggle: (year: number) => void;
  /** Installments numbered at or below this are paid. */
  paidInstallments: number;
  /** Installment number of the next due EMI, or null when none is left. */
  nextDueMonth: number | null;
  hideAmounts: boolean;
}

/**
 * One calendar year of a loan's repayment schedule: a disclosure header and, only while open,
 * a single hairline-divided list of its EMIs. A collapsed year renders no rows at all, which is
 * what keeps a 20-year schedule cheap.
 */
export const ScheduleYearSection = memo(function ScheduleYearSection({
  group,
  expanded,
  onToggle,
  paidInstallments,
  nextDueMonth,
  hideAmounts,
}: ScheduleYearSectionProps) {
  const count = group.rows.length;
  const panelId = `schedule-year-${group.year}`;

  return (
    <section>
      <button
        type="button"
        onClick={() => onToggle(group.year)}
        aria-expanded={expanded}
        aria-controls={panelId}
        className="hover:bg-muted/50 -mx-2 flex w-[calc(100%+1rem)] items-center justify-between gap-3 rounded-full px-2 py-2 text-left"
      >
        <span className="min-w-0 text-sm">
          <span className="font-semibold">{group.year}</span>
          <span className="text-muted-foreground">
            {' · '}
            {count} EMI{count === 1 ? '' : 's'} ·{' '}
            {formatCurrency(group.totalPaid, false, hideAmounts, { precise: false })} paid
          </span>
        </span>
        <ChevronDown
          size={16}
          aria-hidden
          className={cn(
            'text-muted-foreground shrink-0 transition-transform',
            expanded && 'rotate-180',
          )}
        />
      </button>

      {expanded && (
        <ol id={panelId} className="card-elevated divide-border mt-1 divide-y rounded-md">
          {group.rows.map((row) => {
            const isPaid = row.month <= paidInstallments;
            const isNext = row.month === nextDueMonth;
            const principalShare = row.emi > 0 ? (row.principal / row.emi) * 100 : 0;
            return (
              <li
                key={row.month}
                aria-current={isNext ? 'step' : undefined}
                className={cn(
                  'flex items-center gap-3 px-4 py-3 first:rounded-t-md last:rounded-b-md',
                  isNext && 'bg-accent/40',
                )}
              >
                <div className="min-w-0 flex-1">
                  <p className={cn('flex items-center gap-1.5 text-sm', isPaid && 'opacity-70')}>
                    {isNext && (
                      <span aria-hidden className="bg-primary size-1.5 shrink-0 rounded-full" />
                    )}
                    <span className="truncate">
                      <span className="font-medium">EMI {row.month}</span>
                      <span className="text-muted-foreground"> · {formatShortDate(row.date)}</span>
                    </span>
                  </p>
                  <div
                    aria-hidden
                    className={cn(
                      'bg-muted mt-1.5 flex h-1 w-full max-w-40 overflow-hidden rounded-full',
                      isPaid && 'opacity-60',
                    )}
                  >
                    <div className="bg-primary h-full" style={{ width: `${principalShare}%` }} />
                    <div className="bg-warning/60 h-full flex-1" />
                  </div>
                  <span className="sr-only">
                    Principal {formatCurrency(row.principal, false, hideAmounts)}, interest{' '}
                    {formatCurrency(row.interest, false, hideAmounts)}.
                  </span>
                  {(isPaid || isNext) && (
                    <p
                      className={cn(
                        'mt-1 flex items-center gap-1 text-xs',
                        isPaid ? 'text-muted-foreground' : 'text-primary font-medium',
                      )}
                    >
                      {isPaid ? (
                        <>
                          <Check size={12} aria-hidden className="text-positive" />
                          Paid
                        </>
                      ) : (
                        'Next due'
                      )}
                    </p>
                  )}
                  {row.prepayment > 0 && (
                    <p className="text-positive mt-1 text-xs font-medium">
                      +{formatCurrency(row.prepayment, false, hideAmounts)} prepaid
                    </p>
                  )}
                </div>
                <div className={cn('shrink-0 text-right', isPaid && 'opacity-70')}>
                  <p className="text-sm font-semibold">
                    {formatCurrency(row.emi, false, hideAmounts)}
                  </p>
                  <p className="text-muted-foreground text-xs">
                    Balance{' '}
                    {formatCurrency(row.closingBalance, false, hideAmounts, { precise: false })}
                  </p>
                </div>
              </li>
            );
          })}
        </ol>
      )}
    </section>
  );
});
