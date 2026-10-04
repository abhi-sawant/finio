import { useCallback, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { ArrowLeft, CircleCheck } from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import {
  buildAmortizationSchedule,
  groupScheduleByYear,
  loanStatus,
  type LoanScheduleInput,
} from '@/utils/loan';
import { formatCurrency, formatShortDate } from '@/utils/formatters';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { NoteCard } from '@/components/ui/note-card';
import { noteFigureClass } from '@/components/ui/note-figure';
import { ScheduleYearSection } from '@/components/loans/ScheduleYearSection';
import { PrepayCalculator } from '@/components/loans/PrepayCalculator';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

export default function LoanSchedule() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams();
  const loan = useFinanceStore((s) => s.loans.find((l) => l.id === id));
  const loanPrepayments = useFinanceStore((s) => s.loanPrepayments);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  // The exact shape Loans.tsx builds, so this schedule always agrees with the loan card.
  const input = useMemo<LoanScheduleInput | null>(() => {
    if (!loan) return null;
    return {
      principal: loan.principal,
      interestRate: loan.interestRate,
      tenureMonths: loan.tenureMonths,
      startDate: loan.startDate,
      prepayments: loanPrepayments
        .filter((p) => p.loanId === loan.id)
        .map((p) => ({ amount: p.amount, date: p.date })),
    };
  }, [loan, loanPrepayments]);

  const schedule = useMemo(() => (input ? buildAmortizationSchedule(input) : []), [input]);
  const status = useMemo(() => (input ? loanStatus(input) : null), [input]);
  const groups = useMemo(() => groupScheduleByYear(schedule), [schedule]);

  const nextDueRow = status ? (schedule[status.paidInstallments] ?? null) : null;
  const defaultYear = nextDueRow
    ? new Date(nextDueRow.date).getFullYear()
    : (groups[groups.length - 1]?.year ?? null);

  const [openYears, setOpenYears] = useState<Set<number> | null>(null);
  const expandedYears = openYears ?? new Set(defaultYear !== null ? [defaultYear] : []);
  const toggleYear = useCallback(
    (year: number) =>
      setOpenYears((prev) => {
        const next = new Set(prev ?? (defaultYear !== null ? [defaultYear] : []));
        if (next.has(year)) next.delete(year);
        else next.add(year);
        return next;
      }),
    [defaultYear],
  );

  const goBack = () => {
    // A deep link has no in-app history to return to.
    if (location.key === 'default') navigate('/loans');
    else navigate(-1);
  };

  const header = (
    <Header innerClassName="lg:max-w-2xl">
      <HeaderIconButton onClick={goBack} aria-label="Back">
        <ArrowLeft />
      </HeaderIconButton>
      <h1 className="text-base font-semibold">Repayment schedule</h1>
      <HideAmountsToggle />
    </Header>
  );

  if (!loan || !input || !status) {
    return header;
  }

  const isPaidOff = !!loan.closedAt || status.isPaidOff;
  const repaid = Math.max(0, loan.principal - status.outstandingBalance);
  const repaidPct = loan.principal > 0 ? Math.min(100, (repaid / loan.principal) * 100) : 0;
  const outstandingText = formatCurrency(
    isPaidOff ? 0 : status.outstandingBalance,
    false,
    hideAmounts,
  );

  const stats: { label: string; value: string }[] = [
    { label: 'Total interest', value: formatCurrency(status.totalInterest, false, hideAmounts) },
    {
      label: 'Interest paid so far',
      value: formatCurrency(status.totalInterestPaid, false, hideAmounts),
    },
    {
      label: 'Next EMI',
      value: isPaidOff
        ? 'Paid off'
        : status.nextDueDate
          ? formatShortDate(status.nextDueDate)
          : '—',
    },
    {
      // A loan marked paid off early closed on that day, not on the schedule's last EMI.
      label: loan.closedAt ? 'Closed on' : 'Payoff date',
      value: loan.closedAt
        ? formatShortDate(loan.closedAt)
        : status.payoffDate
          ? formatShortDate(status.payoffDate)
          : '—',
    },
  ];

  return (
    <>
      {header}

      <Main className="lg:max-w-2xl">
        <NoteCard>
          <p className="text-muted-foreground truncate text-sm font-medium">{loan.name}</p>
          {isPaidOff ? (
            <p className="font-money text-positive mt-1 flex items-center gap-2 text-[2.25rem] leading-[1.05]">
              <CircleCheck className="size-8 shrink-0" aria-hidden />
              Paid off
            </p>
          ) : (
            <>
              <p className={`font-money mt-1 leading-[1.05] ${noteFigureClass(outstandingText)}`}>
                {outstandingText}
              </p>
              <p className="text-muted-foreground text-xs">Outstanding</p>
            </>
          )}
          <p className="mt-2 text-sm">
            EMI {formatCurrency(status.emi, false, hideAmounts)} · {status.paidInstallments} of{' '}
            {status.totalMonths} paid
          </p>
          <div
            role="progressbar"
            aria-label="Principal repaid"
            aria-valuemin={0}
            aria-valuemax={100}
            aria-valuenow={Math.round(repaidPct)}
            aria-valuetext={`${formatCurrency(repaid, true, hideAmounts)} of ${formatCurrency(loan.principal, true, hideAmounts)} principal repaid`}
            className="bg-muted mt-3 h-2 overflow-hidden rounded-full"
          >
            <div
              className="h-full rounded-full"
              style={{ width: `${repaidPct}%`, background: 'var(--register)' }}
            />
          </div>
          <p className="text-muted-foreground mt-1.5 text-xs" aria-hidden>
            {formatCurrency(repaid, true, hideAmounts)} of{' '}
            {formatCurrency(loan.principal, true, hideAmounts)} principal repaid
          </p>
        </NoteCard>

        <dl className="card-elevated grid grid-cols-2 gap-x-4 gap-y-3 rounded-md p-4">
          {stats.map((s) => (
            <div key={s.label} className="min-w-0">
              <dt className="text-muted-foreground text-xs font-medium">{s.label}</dt>
              <dd
                className={`mt-0.5 truncate text-sm font-semibold ${s.value === 'Paid off' ? 'text-positive' : ''}`}
              >
                {s.value}
              </dd>
            </div>
          ))}
        </dl>

        <section aria-labelledby="schedule-heading" className="space-y-2">
          <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
            <h2 id="schedule-heading" className="text-base font-semibold">
              Schedule
            </h2>
            <div aria-hidden className="text-muted-foreground flex items-center gap-3 text-xs">
              <span className="flex items-center gap-1.5">
                <span className="bg-primary h-1 w-3 rounded-full" />
                Principal
              </span>
              <span className="flex items-center gap-1.5">
                <span className="bg-warning/60 h-1 w-3 rounded-full" />
                Interest
              </span>
            </div>
          </div>
          {groups.length === 0 ? (
            <p className="text-muted-foreground text-sm">No installments to show.</p>
          ) : (
            <div className="space-y-1">
              {groups.map((g) => (
                <ScheduleYearSection
                  key={g.year}
                  group={g}
                  expanded={expandedYears.has(g.year)}
                  onToggle={toggleYear}
                  paidInstallments={status.paidInstallments}
                  nextDueMonth={isPaidOff ? null : (nextDueRow?.month ?? null)}
                  hideAmounts={hideAmounts}
                />
              ))}
            </div>
          )}
        </section>

        {!isPaidOff && (
          <PrepayCalculator
            loan={input}
            hideAmounts={hideAmounts}
            onRecord={() => navigate('/loans')}
          />
        )}
      </Main>
    </>
  );
}
