import { useMemo, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router';
import { ArrowLeft, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { MISC_CATEGORY_ID } from '@/data/defaultData';
import { calculateEmi } from '@/utils/loan';
import { previewBackfill } from '@/store/recurring';
import { activeAccounts, isCategoryValidForType, miscLast } from '@/utils/calculations';
import { MAX_NAME_LENGTH, cleanText, stripLeading } from '@/utils/validation';
import { formatCurrency, localDayKey } from '@/utils/formatters';
import { CategoryIcon } from '@/components/categories/CategoryIcon';
import { CategoryGrid } from '@/components/categories/CategoryGrid';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { DatePicker } from '@/components/ui/date-picker';
import { SwitchField } from '@/components/ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useConfirm } from '@/components/ui/use-confirm';
import Header from '@/components/ui/header';
import { HeaderIconButton, HeaderIconSpacer } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

/** Loan/EMI in the default set — the sane default for a new loan's category. */
const DEFAULT_LOAN_CATEGORY_ID = 'cat-27';
const DEFAULT_LOAN_CATEGORY_NAME = 'Loan / EMI';

export default function AddLoan() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const { id } = useParams();
  const accounts = useFinanceStore((s) => s.accounts);
  const categories = useFinanceStore((s) => s.categories);
  const loans = useFinanceStore((s) => s.loans);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const addLoan = useFinanceStore((s) => s.addLoan);
  const updateLoan = useFinanceStore((s) => s.updateLoan);
  const deleteLoan = useFinanceStore((s) => s.deleteLoan);
  const processRecurring = useFinanceStore((s) => s.processRecurring);
  const bulkDeleteTransactions = useFinanceStore((s) => s.bulkDeleteTransactions);

  const existing = id ? loans.find((l) => l.id === id) : null;
  const openAccounts = useMemo(() => activeAccounts(accounts), [accounts]);
  const expenseCategories = useMemo(
    () => miscLast(categories.filter((c) => isCategoryValidForType(c, 'expense'))),
    [categories],
  );

  const [name, setName] = useState(existing?.name ?? '');
  const [principal, setPrincipal] = useState(existing?.principal.toString() ?? '0');
  const [interestRate, setInterestRate] = useState(existing?.interestRate.toString() ?? '');
  const [tenureMonths, setTenureMonths] = useState(existing?.tenureMonths.toString() ?? '');
  const [startDate, setStartDate] = useState(
    existing?.startDate ? localDayKey(existing.startDate) : '',
  );
  const [accountId, setAccountId] = useState(existing?.accountId ?? openAccounts[0]?.id ?? '');
  const [categoryId, setCategoryId] = useState(
    existing?.categoryId ??
      categories.find((c) => c.id === DEFAULT_LOAN_CATEGORY_ID)?.id ??
      categories.find((c) => c.name === DEFAULT_LOAN_CATEGORY_NAME)?.id ??
      MISC_CATEGORY_ID,
  );

  const parsedPrincipal = parseFloat(principal) || 0;
  const parsedRate = parseFloat(interestRate) || 0;
  const parsedTenure = parseInt(tenureMonths, 10) || 0;
  const previewEmi = calculateEmi(parsedPrincipal, parsedRate, parsedTenure);

  // A new loan whose first EMI date is already behind us: by default those EMIs were paid
  // outside Finio; the switch posts them as real expenses instead (same choice FD/RD offer).
  const [logPastEmis, setLogPastEmis] = useState(false);
  const pastEmis = useMemo(() => {
    if (existing || startDate === '' || parsedTenure <= 0 || previewEmi <= 0 || !accountId) {
      return null;
    }
    const preview = previewBackfill(
      {
        id: 'draft',
        type: 'expense',
        amount: previewEmi,
        accountId,
        categoryId,
        note: '',
        labels: [],
        frequency: 'monthly',
        startDate: new Date(`${startDate}T00:00:00`).toISOString(),
        maxOccurrences: parsedTenure,
        occurrenceCount: 0,
        lastRunDate: null,
        createdAt: new Date().toISOString(),
      },
      [accountId],
      new Date(),
    );
    return preview.count > 0 ? preview : null;
  }, [existing, startDate, parsedTenure, previewEmi, accountId, categoryId]);
  const accountName = openAccounts.find((a) => a.id === accountId)?.name ?? 'the account';

  const submitting = useRef(false);

  const handleSubmit = () => {
    if (submitting.current) return;
    if (name.trim() === '') return void toast.error('Enter a loan name');
    if (parsedPrincipal <= 0) return void toast.error('Enter the loan amount');
    if (parsedTenure <= 0) return void toast.error('Enter the tenure in months');
    if (startDate === '') return void toast.error('Select a start date');
    if (accountId === '') return void toast.error('Select an account');
    if (categoryId === '') return void toast.error('Select a category');
    submitting.current = true;

    const data = {
      name: cleanText(name, MAX_NAME_LENGTH),
      principal: parsedPrincipal,
      interestRate: parsedRate,
      tenureMonths: parsedTenure,
      startDate: new Date(`${startDate}T00:00:00`).toISOString(),
      accountId,
      categoryId,
    };

    if (existing) {
      updateLoan(existing.id, data);
      toast.success('Loan updated');
    } else {
      const logPast = !!pastEmis && logPastEmis;
      addLoan(data, { logPastEmis: logPast });
      const posted = logPast ? processRecurring() : [];
      if (posted.length > 0) {
        const ids = posted.map((t) => t.id);
        toast.success(
          `Loan added · posted ${posted.length} past EMI${posted.length === 1 ? '' : 's'}`,
          {
            action: { label: 'Undo', onClick: () => bulkDeleteTransactions(ids) },
          },
        );
      } else {
        toast.success('Loan added');
      }
    }
    navigate(-1);
  };

  const handleDelete = async () => {
    if (!existing) return;
    const confirmed = await confirm({
      title: `Delete "${existing.name}"?`,
      description:
        'Every prepayment logged against it and its auto-generated EMI rule will be removed too. EMI transactions already posted stay in your history. This cannot be undone.',
      confirmLabel: 'Delete',
    });
    if (confirmed) {
      deleteLoan(existing.id);
      navigate(-1);
    }
  };

  return (
    <>
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">{existing ? 'Edit loan' : 'Add loan'}</h1>
        {existing ? (
          <HeaderIconButton onClick={handleDelete} aria-label="Delete loan" tone="destructive">
            <Trash2 />
          </HeaderIconButton>
        ) : (
          <HeaderIconSpacer />
        )}
      </Header>

      <Main className="lg:max-w-xl">
        <div className="card-elevated space-y-4 rounded-md p-4">
          <div>
            <Label
              htmlFor="loanName"
              className="text-muted-foreground mb-1.5 block text-xs font-medium"
            >
              Loan name
            </Label>
            <Input
              id="loanName"
              type="text"
              placeholder="e.g., Home Loan — HDFC"
              value={name}
              maxLength={MAX_NAME_LENGTH}
              onChange={(e) => setName(stripLeading(e.target.value))}
            />
          </div>

          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Principal
            </Label>
            <NumberPad value={principal} onChange={setPrincipal} />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label
                htmlFor="interestRate"
                className="text-muted-foreground mb-1.5 block text-xs font-medium"
              >
                Interest rate (% p.a.)
              </Label>
              <Input
                id="interestRate"
                type="number"
                inputMode="decimal"
                min={0}
                step={0.01}
                placeholder="e.g. 8.5"
                value={interestRate}
                onChange={(e) => setInterestRate(e.target.value)}
              />
            </div>
            <div>
              <Label
                htmlFor="tenureMonths"
                className="text-muted-foreground mb-1.5 block text-xs font-medium"
              >
                Tenure (months)
              </Label>
              <Input
                id="tenureMonths"
                type="number"
                inputMode="numeric"
                min={1}
                placeholder="e.g. 240"
                value={tenureMonths}
                onChange={(e) => setTenureMonths(e.target.value)}
              />
            </div>
          </div>

          {previewEmi > 0 && (
            <div className="border-border flex items-baseline justify-between gap-3 border-t pt-3">
              <p className="text-muted-foreground text-xs font-medium">Estimated EMI</p>
              <p className="font-money text-lg">
                {formatCurrency(previewEmi, false, hideAmounts)}
                <span className="text-muted-foreground font-sans text-xs font-medium">/month</span>
              </p>
            </div>
          )}
        </div>

        <div className="card-elevated space-y-4 rounded-md p-4">
          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              First EMI date
            </Label>
            <DatePicker value={startDate} onChange={setStartDate} placeholder="Pick a date" />
          </div>

          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Pay EMI from
            </Label>
            {openAccounts.length === 0 ? (
              <p className="text-destructive text-xs">Add an account first.</p>
            ) : (
              <Select value={accountId} onValueChange={(v) => setAccountId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {openAccounts.find((a) => a.id === accountId)?.name ?? 'Choose account'}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  {openAccounts.map((a) => (
                    <SelectItem key={a.id} value={a.id}>
                      {a.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          </div>

          <div>
            <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
              Category
            </Label>
            <CategoryGrid className="max-h-40">
              {expenseCategories.map((cat) => {
                const selected = categoryId === cat.id;
                return (
                  <button
                    key={cat.id}
                    data-selected={selected}
                    onClick={() => setCategoryId(cat.id)}
                    className={`flex flex-col items-center gap-1 rounded-sm border p-2 text-center transition-all ${
                      selected
                        ? 'ring-grad-primary border-transparent'
                        : 'border-border bg-card hover:bg-muted'
                    }`}
                    style={selected ? { backgroundColor: `${cat.color}22` } : undefined}
                  >
                    <div
                      className="flex h-7 w-7 items-center justify-center rounded-full"
                      style={{ backgroundColor: cat.color }}
                    >
                      <CategoryIcon icon={cat.icon} size={14} color="white" />
                    </div>
                    <span className="line-clamp-2 text-[10px] leading-tight">{cat.name}</span>
                  </button>
                );
              })}
            </CategoryGrid>
          </div>
        </div>

        {pastEmis && (
          <div className="card-elevated rounded-md p-4">
            <SwitchField
              title="Log past EMIs as transactions"
              description={
                logPastEmis
                  ? `${pastEmis.count} EMI${pastEmis.count === 1 ? '' : 's'} (${formatCurrency(pastEmis.total, false, hideAmounts)}) will be posted from ${accountName} and show in its history.`
                  : `${pastEmis.count} EMI${pastEmis.count === 1 ? '' : 's'} (${formatCurrency(pastEmis.total, false, hideAmounts)}) already paid count towards the loan; ${accountName} is left untouched.`
              }
              checked={logPastEmis}
              onCheckedChange={setLogPastEmis}
            />
          </div>
        )}

        <Button onClick={handleSubmit} size="lg" className="w-full">
          {existing ? 'Update loan' : 'Add loan'}
        </Button>
      </Main>
    </>
  );
}
