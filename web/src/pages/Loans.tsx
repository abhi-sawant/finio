import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import {
  ArrowLeft,
  CalendarRange,
  ChevronDown,
  CircleCheck,
  Landmark,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { loanStatus, maxPrepayment, simulatePrepaymentImpact } from '@/utils/loan';
import { formatCurrency, formatShortDate, todayKey } from '@/utils/formatters';
import { MAX_NOTE_LENGTH, cleanText, stripLeading } from '@/utils/validation';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { NumberPad } from '@/components/ui/number-pad';
import { DatePicker } from '@/components/ui/date-picker';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { useConfirm } from '@/components/ui/use-confirm';
import type { Loan, LoanPrepayment } from '@/types';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

interface LoanCardProps {
  loan: Loan;
  prepayments: LoanPrepayment[];
  expanded: boolean;
  onToggle: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onToggleClosed: () => void;
  onAddPrepayment: () => void;
  onDeletePrepayment: (id: string) => void;
}

function LoanCard({
  loan,
  prepayments,
  expanded,
  onToggle,
  onEdit,
  onDelete,
  onToggleClosed,
  onAddPrepayment,
  onDeletePrepayment,
}: LoanCardProps) {
  const navigate = useNavigate();
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const status = useMemo(
    () =>
      loanStatus({
        principal: loan.principal,
        interestRate: loan.interestRate,
        tenureMonths: loan.tenureMonths,
        startDate: loan.startDate,
        prepayments: prepayments.map((p) => ({ amount: p.amount, date: p.date })),
      }),
    [loan, prepayments],
  );
  const isClosed = !!loan.closedAt;
  const progress = status.totalMonths > 0 ? status.paidInstallments / status.totalMonths : 0;

  return (
    <div className={`card-elevated rounded-md p-4 ${isClosed ? 'opacity-70' : ''}`}>
      <div className="mb-2 flex items-center justify-between">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{loan.name}</p>
          <p className="text-muted-foreground truncate text-[11px]">
            {isClosed
              ? 'Paid off'
              : status.isPaidOff
                ? 'All installments due'
                : `EMI ${formatCurrency(status.emi, true, hideAmounts)}/mo · Next ${
                    status.nextDueDate ? formatShortDate(status.nextDueDate) : '—'
                  }`}
          </p>
        </div>
        <div className="flex shrink-0 items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={onEdit}
            className="h-7 w-7"
            aria-label={`Edit ${loan.name}`}
          >
            <Pencil size={13} className="text-muted-foreground" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={onDelete}
            className="h-7 w-7"
            aria-label={`Delete ${loan.name}`}
          >
            <Trash2 size={13} className="text-destructive" />
          </Button>
        </div>
      </div>

      <div className="mb-2">
        <div className="bg-muted h-1.5 overflow-hidden rounded-full">
          <div
            className="bg-grad-primary h-full rounded-full transition-all"
            style={{ width: `${Math.min(progress * 100, 100)}%` }}
          />
        </div>
        <p className="text-muted-foreground mt-1 text-[11px]">
          {status.paidInstallments} of {status.totalMonths} installments ·{' '}
          {formatCurrency(status.outstandingBalance, true, hideAmounts)} outstanding
        </p>
      </div>

      <div className="flex gap-2">
        <Button
          variant="secondary"
          onClick={onAddPrepayment}
          disabled={isClosed}
          className="bg-accent text-accent-foreground hover:bg-accent/80 flex-1 text-xs"
        >
          Add prepayment
        </Button>
        <Button variant="secondary" onClick={onToggleClosed} className="flex-1 text-xs">
          {isClosed ? 'Reopen' : 'Mark paid off'}
        </Button>
      </div>

      <button
        onClick={onToggle}
        className="text-muted-foreground mt-2 flex items-center gap-1 text-[11px] font-medium"
        aria-expanded={expanded}
      >
        Details
        <ChevronDown
          size={12}
          className={expanded ? 'rotate-180 transition-transform' : 'transition-transform'}
        />
      </button>

      {expanded && (
        <div className="border-border mt-2 space-y-2 border-t pt-2 text-[11px]">
          <div className="grid grid-cols-2 gap-2">
            <div>
              <p className="text-muted-foreground">Total interest (life of loan)</p>
              <p className="font-medium">
                {formatCurrency(status.totalInterest, true, hideAmounts)}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Interest paid so far</p>
              <p className="font-medium">
                {formatCurrency(status.totalInterestPaid, true, hideAmounts)}
              </p>
            </div>
            <div>
              <p className="text-muted-foreground">Principal</p>
              <p className="font-medium">{formatCurrency(loan.principal, true, hideAmounts)}</p>
            </div>
            <div>
              <p className="text-muted-foreground">Payoff date</p>
              <p className="font-medium">
                {status.payoffDate ? formatShortDate(status.payoffDate) : '—'}
              </p>
            </div>
          </div>

          <Button
            variant="outline"
            size="sm"
            onClick={() => navigate(`/loan-schedule/${loan.id}`)}
            className="text-xs"
          >
            <CalendarRange aria-hidden />
            Repayment schedule
          </Button>

          <p className="text-muted-foreground pt-1 font-medium">Prepayments</p>
          {prepayments.length === 0 ? (
            <p className="text-muted-foreground">None logged yet.</p>
          ) : (
            prepayments.map((p) => (
              <div key={p.id} className="flex items-center gap-2">
                <span className="text-muted-foreground w-16 shrink-0">
                  {formatShortDate(p.date)}
                </span>
                <span className="min-w-0 flex-1 truncate">{p.note}</span>
                <span className="text-positive shrink-0 font-medium">
                  {formatCurrency(p.amount, true, hideAmounts)}
                </span>
                <button
                  onClick={() => onDeletePrepayment(p.id)}
                  aria-label="Delete prepayment"
                  className="text-muted-foreground shrink-0"
                >
                  <Trash2 size={12} />
                </button>
              </div>
            ))
          )}
        </div>
      )}
    </div>
  );
}

export default function Loans() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const loans = useFinanceStore((s) => s.loans);
  const loanPrepayments = useFinanceStore((s) => s.loanPrepayments);
  const setLoanClosed = useFinanceStore((s) => s.setLoanClosed);
  const deleteLoan = useFinanceStore((s) => s.deleteLoan);
  const addLoanPrepayment = useFinanceStore((s) => s.addLoanPrepayment);
  const deleteLoanPrepayment = useFinanceStore((s) => s.deleteLoanPrepayment);
  const restoreLoanPrepayment = useFinanceStore((s) => s.restoreLoanPrepayment);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [prepayLoan, setPrepayLoan] = useState<Loan | null>(null);
  const [prepayAmount, setPrepayAmount] = useState('0');
  const [prepayDate, setPrepayDate] = useState(todayKey());
  const [prepayNote, setPrepayNote] = useState('');

  const activeLoans = useMemo(() => loans.filter((l) => !l.closedAt), [loans]);
  const closedLoans = useMemo(() => loans.filter((l) => l.closedAt), [loans]);
  const [showClosed, setShowClosed] = useState(false);

  const prepaymentsByLoan = useMemo(() => {
    const map = new Map<string, LoanPrepayment[]>();
    for (const p of loanPrepayments) {
      const bucket = map.get(p.loanId);
      if (bucket) bucket.push(p);
      else map.set(p.loanId, [p]);
    }
    return map;
  }, [loanPrepayments]);

  const totalOutstanding = useMemo(
    () =>
      activeLoans.reduce((sum, loan) => {
        const status = loanStatus({
          principal: loan.principal,
          interestRate: loan.interestRate,
          tenureMonths: loan.tenureMonths,
          startDate: loan.startDate,
          prepayments: (prepaymentsByLoan.get(loan.id) ?? []).map((p) => ({
            amount: p.amount,
            date: p.date,
          })),
        });
        return sum + status.outstandingBalance;
      }, 0),
    [activeLoans, prepaymentsByLoan],
  );

  const openPrepay = (loan: Loan) => {
    setPrepayLoan(loan);
    setPrepayAmount('0');
    setPrepayDate(todayKey());
    setPrepayNote('');
  };

  const prepayLimit = useMemo(() => {
    if (!prepayLoan) return 0;
    return maxPrepayment({
      principal: prepayLoan.principal,
      interestRate: prepayLoan.interestRate,
      tenureMonths: prepayLoan.tenureMonths,
      startDate: prepayLoan.startDate,
      prepayments: (prepaymentsByLoan.get(prepayLoan.id) ?? []).map((p) => ({
        amount: p.amount,
        date: p.date,
      })),
    });
  }, [prepayLoan, prepaymentsByLoan]);
  const prepayOverLimit = (parseFloat(prepayAmount) || 0) > prepayLimit + 0.005;

  const prepayImpact = useMemo(() => {
    if (!prepayLoan) return null;
    const amount = parseFloat(prepayAmount) || 0;
    if (amount <= 0 || !prepayDate) return null;
    return simulatePrepaymentImpact(
      {
        principal: prepayLoan.principal,
        interestRate: prepayLoan.interestRate,
        tenureMonths: prepayLoan.tenureMonths,
        startDate: prepayLoan.startDate,
        prepayments: (prepaymentsByLoan.get(prepayLoan.id) ?? []).map((p) => ({
          amount: p.amount,
          date: p.date,
        })),
      },
      { amount, date: new Date(`${prepayDate}T00:00:00`).toISOString() },
    );
  }, [prepayLoan, prepayAmount, prepayDate, prepaymentsByLoan]);

  const handlePrepaySubmit = () => {
    if (!prepayLoan) return;
    const amount = parseFloat(prepayAmount) || 0;
    if (amount <= 0) {
      toast.error('Enter a valid amount');
      return;
    }
    if (prepayOverLimit) {
      toast.error(
        `Only ${formatCurrency(prepayLimit, false, hideAmounts)} is still owed on this loan`,
      );
      return;
    }
    addLoanPrepayment({
      loanId: prepayLoan.id,
      amount,
      date: new Date(`${prepayDate}T00:00:00`).toISOString(),
      note: cleanText(prepayNote, MAX_NOTE_LENGTH),
    });
    toast.success(`Prepayment recorded on "${prepayLoan.name}"`);
    setPrepayLoan(null);
  };

  const handleDelete = async (loan: Loan) => {
    const confirmed = await confirm({
      title: `Delete "${loan.name}"?`,
      description:
        'Every prepayment logged against it and its auto-generated EMI rule will be removed too. EMI transactions already posted stay in your history. This cannot be undone.',
      confirmLabel: 'Delete loan',
    });
    if (confirmed) deleteLoan(loan.id);
  };

  const handleToggleClosed = (loan: Loan) => {
    if (loan.closedAt) {
      setLoanClosed(loan.id, false);
      toast.success(`"${loan.name}" reopened`);
      return;
    }
    setLoanClosed(loan.id, true);
    toast.success(`"${loan.name}" marked paid off`, {
      action: { label: 'Undo', onClick: () => setLoanClosed(loan.id, false) },
    });
  };

  const renderCard = (loan: Loan) => (
    <LoanCard
      key={loan.id}
      loan={loan}
      prepayments={prepaymentsByLoan.get(loan.id) ?? []}
      expanded={expandedId === loan.id}
      onToggle={() => setExpandedId((k) => (k === loan.id ? null : loan.id))}
      onEdit={() => navigate(`/edit-loan/${loan.id}`)}
      onDelete={() => handleDelete(loan)}
      onToggleClosed={() => handleToggleClosed(loan)}
      onAddPrepayment={() => openPrepay(loan)}
      onDeletePrepayment={(id) => {
        const removed = deleteLoanPrepayment(id);
        if (!removed) return;
        toast.success('Prepayment entry removed', {
          action: { label: 'Undo', onClick: () => restoreLoanPrepayment(removed) },
        });
      }}
    />
  );

  return (
    <>
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Loans</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton
            onClick={() => navigate('/add-loan')}
            aria-label="Add loan"
            tone="primary"
          >
            <Plus />
          </HeaderIconButton>
        </div>
      </Header>

      <Main className="lg:max-w-xl">
        {activeLoans.length > 0 && (
          <div className="card-elevated rounded-md p-4">
            <p className="text-muted-foreground text-xs font-medium">
              Outstanding across {activeLoans.length} loan{activeLoans.length === 1 ? '' : 's'}
            </p>
            <p className="font-money mt-1 text-xl">
              {formatCurrency(totalOutstanding, true, hideAmounts)}
            </p>
          </div>
        )}

        {activeLoans.length === 0 && closedLoans.length > 0 && (
          // Every loan is paid off — say so, rather than leaving the page blank above a
          // collapsed "Paid off" toggle.
          <div className="py-10 text-center">
            <CircleCheck size={28} className="text-positive mx-auto mb-3" aria-hidden />
            <p className="font-medium">All loans paid off</p>
            <p className="text-muted-foreground mt-1 mb-4 text-sm">
              {closedLoans.length} loan{closedLoans.length === 1 ? '' : 's'} closed. Nothing
              outstanding.
            </p>
            <Button onClick={() => navigate('/add-loan')} className="rounded-full px-5 py-2.5">
              Add loan
            </Button>
          </div>
        )}

        <div className="space-y-3">{activeLoans.map(renderCard)}</div>

        {closedLoans.length > 0 && (
          <div>
            <button
              onClick={() => setShowClosed((v) => !v)}
              className="text-muted-foreground mb-2 flex items-center gap-1 text-sm font-medium"
              aria-expanded={showClosed}
            >
              <ChevronDown
                size={14}
                className={showClosed ? 'rotate-180 transition-transform' : 'transition-transform'}
              />
              Paid off ({closedLoans.length})
            </button>
            {showClosed && <div className="space-y-3">{closedLoans.map(renderCard)}</div>}
          </div>
        )}

        {loans.length === 0 && (
          <div className="py-12 text-center">
            <Landmark size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
            <p className="text-muted-foreground mb-4">No loans yet</p>
            <Button onClick={() => navigate('/add-loan')} className="rounded-full px-5 py-2.5">
              Add loan
            </Button>
          </div>
        )}
      </Main>

      <Dialog
        open={prepayLoan !== null}
        onOpenChange={(v) => {
          if (!v) setPrepayLoan(null);
        }}
      >
        <DialogContent className="bg-card mx-auto w-11/12">
          <DialogHeader>
            <DialogTitle className="pr-8 break-words">Prepay "{prepayLoan?.name}"</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <NumberPad value={prepayAmount} onChange={setPrepayAmount} />
            <DatePicker value={prepayDate} onChange={setPrepayDate} placeholder="Pick a date" />
            <Input
              type="text"
              placeholder="Note (optional)"
              value={prepayNote}
              maxLength={MAX_NOTE_LENGTH}
              onChange={(e) => setPrepayNote(stripLeading(e.target.value))}
            />
            <p
              className={
                prepayOverLimit ? 'text-destructive text-xs' : 'text-muted-foreground text-xs'
              }
            >
              Outstanding: {formatCurrency(prepayLimit, false, hideAmounts)} — the most you can
              prepay.
            </p>
            {prepayImpact && !prepayOverLimit && (
              <p className="text-muted-foreground text-xs">
                This would save{' '}
                <span className="text-positive font-medium">
                  {prepayImpact.monthsSaved} month{prepayImpact.monthsSaved === 1 ? '' : 's'}
                </span>{' '}
                and{' '}
                <span className="text-positive font-medium">
                  {formatCurrency(prepayImpact.interestSaved, true, hideAmounts)}
                </span>{' '}
                in interest.
              </p>
            )}
            <div className="flex gap-2">
              <Button
                onClick={handlePrepaySubmit}
                disabled={prepayOverLimit || prepayLimit <= 0}
                className="flex-1"
              >
                Record prepayment
              </Button>
              <Button variant="secondary" onClick={() => setPrepayLoan(null)}>
                Cancel
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}
