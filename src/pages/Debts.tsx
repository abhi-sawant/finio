import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import {
  ArrowLeft,
  ChevronDown,
  HandCoins,
  History,
  Minus,
  Pencil,
  Plus,
  Trash2,
} from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { MISC_CATEGORY_ID } from '@/data/defaultData';
import { COLOR_PALETTE } from '@/data/colorPalette';
import {
  formatCurrency,
  formatDayMonth,
  formatShortDate,
  toLocalDateTimeInputValue,
} from '@/utils/formatters';
import { MAX_NAME_LENGTH, MAX_NOTE_LENGTH, cleanText, stripLeading } from '@/utils/validation';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { PersonIcon } from '@/components/people/PersonIcon';
import { PERSON_ICONS } from '@/components/people/personIcons';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { NumberPad } from '@/components/ui/number-pad';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { useConfirm } from '@/components/ui/use-confirm';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { computePersonBalance, activeAccounts, type PersonBalance } from '@/utils/calculations';
import type { DebtEntry, Person } from '@/types';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

const personColors = COLOR_PALETTE;

export default function Debts() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const people = useFinanceStore((s) => s.people);
  const debtEntries = useFinanceStore((s) => s.debtEntries);
  const accounts = useFinanceStore((s) => s.accounts);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const addPerson = useFinanceStore((s) => s.addPerson);
  const updatePerson = useFinanceStore((s) => s.updatePerson);
  const deletePerson = useFinanceStore((s) => s.deletePerson);
  const addDebtEntry = useFinanceStore((s) => s.addDebtEntry);
  const deleteDebtEntry = useFinanceStore((s) => s.deleteDebtEntry);
  const restoreDebtEntry = useFinanceStore((s) => s.restoreDebtEntry);
  const updateDebtEntry = useFinanceStore((s) => s.updateDebtEntry);
  const addTransaction = useFinanceStore((s) => s.addTransaction);
  const deleteTransaction = useFinanceStore((s) => s.deleteTransaction);

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [icon, setIcon] = useState(PERSON_ICONS[0]);
  const [color, setColor] = useState(personColors[0]);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  // One dialog for logging a new entry and editing an existing one (`editing` set).
  const [entryPerson, setEntryPerson] = useState<{
    person: Person;
    mode: 'lend' | 'borrow';
    editing?: DebtEntry;
  } | null>(null);
  const [entryAmount, setEntryAmount] = useState('');
  const [entryNote, setEntryNote] = useState('');
  const [entryDate, setEntryDate] = useState('');

  const [settlePerson, setSettlePerson] = useState<PersonBalance | null>(null);
  const [settleAmount, setSettleAmount] = useState('');
  const [settleAccountId, setSettleAccountId] = useState('');
  const [settleNote, setSettleNote] = useState('');

  const openAccounts = useMemo(() => activeAccounts(accounts), [accounts]);

  const balances = useMemo(
    () => people.map((p) => computePersonBalance(p, debtEntries)),
    [people, debtEntries],
  );

  // Anyone with an open balance first (bigger balances first), settled-up people trail behind.
  const sortedBalances = useMemo(
    () =>
      [...balances].sort((a, b) => {
        const aSettled = a.balance === 0;
        const bSettled = b.balance === 0;
        if (aSettled !== bSettled) return aSettled ? 1 : -1;
        return Math.abs(b.balance) - Math.abs(a.balance);
      }),
    [balances],
  );

  const resetForm = () => {
    setShowForm(false);
    setEditingId(null);
    setName('');
    setIcon(PERSON_ICONS[0]);
    setColor(personColors[0]);
  };

  const startCreate = () => {
    resetForm();
    setShowForm(true);
  };

  const startEdit = (person: Person) => {
    setEditingId(person.id);
    setName(person.name);
    setIcon(person.icon);
    setColor(person.color);
    setShowForm(true);
  };

  const handleSubmit = () => {
    if (!cleanText(name, MAX_NAME_LENGTH)) {
      toast.error('Enter a name');
      return;
    }

    const data = { name: cleanText(name, MAX_NAME_LENGTH), icon, color };

    if (editingId) {
      updatePerson(editingId, data);
      toast.success('Person updated');
    } else {
      addPerson(data);
      toast.success('Person added');
    }
    resetForm();
  };

  const openEntry = (person: Person, mode: 'lend' | 'borrow') => {
    setEntryPerson({ person, mode });
    setEntryAmount('');
    setEntryNote('');
    setEntryDate(toLocalDateTimeInputValue(new Date()));
  };

  const openEditEntry = (person: Person, entry: DebtEntry) => {
    setEntryPerson({ person, mode: entry.amount < 0 ? 'borrow' : 'lend', editing: entry });
    setEntryAmount(String(Math.abs(entry.amount)));
    setEntryNote(entry.note);
    setEntryDate(toLocalDateTimeInputValue(entry.date));
  };

  const handleEntrySubmit = () => {
    if (!entryPerson) return;
    const parsed = parseFloat(entryAmount);
    if (!parsed || parsed <= 0) {
      toast.error('Enter a valid amount');
      return;
    }
    const when = new Date(entryDate);
    if (Number.isNaN(when.getTime())) {
      toast.error('Choose a date');
      return;
    }

    const { editing } = entryPerson;
    if (editing) {
      const previous = { amount: editing.amount, date: editing.date, note: editing.note };
      // A settled entry keeps its direction (the store enforces it too); a plain one can flip.
      const negative = editing.settledTransactionId
        ? editing.amount < 0
        : entryPerson.mode === 'borrow';
      updateDebtEntry(editing.id, {
        amount: negative ? -parsed : parsed,
        // The picker is minute-precise: an untouched field keeps the stored timestamp exactly,
        // so a note-only edit never nudges the linked transaction's date.
        date:
          entryDate === toLocalDateTimeInputValue(editing.date) ? editing.date : when.toISOString(),
        note: cleanText(entryNote, MAX_NOTE_LENGTH),
      });
      // Writing the old values back restores the linked transaction through the same sync.
      toast.success('Entry updated', {
        action: { label: 'Undo', onClick: () => updateDebtEntry(editing.id, previous) },
      });
      setEntryPerson(null);
      return;
    }

    addDebtEntry({
      personId: entryPerson.person.id,
      // Lending them money (or something they owe you for) increases what they owe you;
      // borrowing from them increases what you owe them.
      amount: entryPerson.mode === 'borrow' ? -parsed : parsed,
      date: when.toISOString(),
      note: cleanText(entryNote, MAX_NOTE_LENGTH),
    });
    toast.success(entryPerson.mode === 'borrow' ? 'Borrowing logged' : 'Lending logged');
    setEntryPerson(null);
  };

  const openSettle = (status: PersonBalance) => {
    setSettlePerson(status);
    setSettleAmount(String(Math.abs(status.balance)));
    setSettleAccountId(openAccounts[0]?.id ?? '');
    setSettleNote('');
  };

  // Settling more than is owed would flip the relationship (they owed you, now you owe them).
  const settleLimit = settlePerson ? Math.abs(settlePerson.balance) : 0;
  const settleOverLimit = (parseFloat(settleAmount) || 0) > settleLimit + 0.005;

  const handleSettleSubmit = () => {
    if (!settlePerson) return;
    const parsed = parseFloat(settleAmount);
    if (!parsed || parsed <= 0) {
      toast.error('Enter a valid amount');
      return;
    }
    if (settleOverLimit) {
      toast.error(`Only ${formatCurrency(settleLimit, false, hideAmounts)} is outstanding`);
      return;
    }
    if (!settleAccountId) {
      toast.error('Choose an account');
      return;
    }

    const { person, balance } = settlePerson;
    // They owe you → settling means they pay you back, an income into the chosen account.
    // You owe them → settling means you pay them, an expense out of the chosen account.
    const type = balance > 0 ? 'income' : 'expense';
    const note = cleanText(settleNote, MAX_NOTE_LENGTH) || `Settled up with ${person.name}`;
    const date = new Date().toISOString();

    const transactionId = addTransaction({
      type,
      amount: parsed,
      accountId: settleAccountId,
      categoryId: MISC_CATEGORY_ID,
      date,
      note,
      labels: [],
    });
    const entryId = addDebtEntry({
      personId: person.id,
      amount: balance > 0 ? -parsed : parsed,
      date,
      note,
      settledTransactionId: transactionId,
    });

    toast.success(`Settled ${formatCurrency(parsed, false, hideAmounts)} with ${person.name}`, {
      action: {
        label: 'Undo',
        onClick: () => {
          // Transaction first, so deleting the entry has no settlement left to stash.
          deleteTransaction(transactionId);
          deleteDebtEntry(entryId);
        },
      },
    });
    setSettlePerson(null);
  };

  return (
    <>
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Debts & lending</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton
            onClick={() => (showForm ? resetForm() : startCreate())}
            aria-label="Add person"
            tone="primary"
          >
            <Plus />
          </HeaderIconButton>
        </div>
      </Header>

      <Main className="lg:max-w-xl">
        {showForm && (
          <div className="card-elevated space-y-3 rounded-md p-4">
            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">Name</Label>
              <Input
                type="text"
                placeholder="e.g., Rahul"
                value={name}
                maxLength={MAX_NAME_LENGTH}
                onChange={(e) => setName(stripLeading(e.target.value))}
              />
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">Icon</Label>
              <div className="grid grid-cols-6 gap-2">
                {PERSON_ICONS.map((i) => (
                  <button
                    key={i}
                    onClick={() => setIcon(i)}
                    className={`flex h-9 items-center justify-center rounded-full border transition-colors ${
                      icon === i ? 'border-primary bg-primary/10' : 'border-border bg-card'
                    }`}
                    aria-label={`Icon ${i}`}
                    aria-pressed={icon === i}
                  >
                    <PersonIcon icon={i} size={16} />
                  </button>
                ))}
              </div>
            </div>

            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Color
              </Label>
              <div className="flex flex-wrap gap-3">
                {personColors.map((c) => (
                  <button
                    key={c}
                    onClick={() => setColor(c)}
                    className={`h-7 w-7 rounded-full transition-transform ${
                      color === c ? 'ring-primary scale-110 ring-2 ring-offset-2' : ''
                    }`}
                    style={{ backgroundColor: c }}
                    aria-label={`Color ${c}`}
                    aria-pressed={color === c}
                  />
                ))}
              </div>
            </div>

            <div className="flex gap-2">
              <Button onClick={handleSubmit} className="flex-1">
                {editingId ? 'Save changes' : 'Save'}
              </Button>
              <Button variant="secondary" onClick={resetForm}>
                Cancel
              </Button>
            </div>
          </div>
        )}

        {sortedBalances.length === 0 ? (
          <div className="py-12 text-center">
            <HandCoins size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
            <p className="text-muted-foreground mb-4">No one on your ledger yet</p>
            <Button onClick={startCreate} className="rounded-full px-5 py-2.5">
              Add your first person
            </Button>
          </div>
        ) : (
          <div className="space-y-3">
            {sortedBalances.map((status) => (
              <PersonCard
                key={status.person.id}
                status={status}
                entries={debtEntries.filter((e) => e.personId === status.person.id)}
                expanded={expandedId === status.person.id}
                onToggleHistory={() =>
                  setExpandedId((id) => (id === status.person.id ? null : status.person.id))
                }
                onLend={() => openEntry(status.person, 'lend')}
                onBorrow={() => openEntry(status.person, 'borrow')}
                onSettle={() => openSettle(status)}
                onEdit={() => startEdit(status.person)}
                onDelete={async () => {
                  const confirmed = await confirm({
                    title: `Delete "${status.person.name}"?`,
                    description:
                      'Every debt entry logged against this person will be deleted too. This cannot be undone.',
                    confirmLabel: 'Delete person',
                  });
                  if (confirmed) deletePerson(status.person.id);
                }}
                onEditEntry={(entry) => openEditEntry(status.person, entry)}
                onDeleteEntry={(id) => {
                  const removed = deleteDebtEntry(id);
                  if (!removed) return;
                  // A settle-up entry takes the real transaction it created with it.
                  const message = removed.settledTransactionId
                    ? 'Settlement and its transaction removed'
                    : 'Entry removed';
                  toast.success(message, {
                    action: { label: 'Undo', onClick: () => restoreDebtEntry(removed) },
                  });
                }}
              />
            ))}
          </div>
        )}
      </Main>

      {/* Lend / borrow entry dialog */}
      <Dialog
        open={entryPerson !== null}
        onOpenChange={(v) => {
          if (!v) setEntryPerson(null);
        }}
      >
        <DialogContent className="bg-card mx-auto w-11/12">
          <DialogHeader>
            <DialogTitle>
              {entryPerson?.editing
                ? 'Edit entry'
                : `${entryPerson?.mode === 'borrow' ? 'Borrowed from' : 'Lent to'} ${entryPerson?.person.name ?? ''}`}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            {entryPerson?.editing?.settledTransactionId ? (
              <div className="space-y-1">
                <p className="text-sm font-medium">
                  Settled up ·{' '}
                  {entryPerson.editing.amount < 0
                    ? `received from ${entryPerson.person.name}`
                    : `paid to ${entryPerson.person.name}`}
                </p>
                <p className="text-muted-foreground text-xs">
                  Changing the amount or date also updates the linked transaction.
                </p>
              </div>
            ) : (
              entryPerson?.editing && (
                <div
                  role="radiogroup"
                  aria-label="Direction"
                  className="bg-muted flex gap-1 rounded-full p-1"
                >
                  {(
                    [
                      ['lend', 'They owe me', 'text-positive'],
                      ['borrow', 'I owe them', 'text-destructive'],
                    ] as const
                  ).map(([mode, label, tone]) => {
                    const selected = entryPerson.mode === mode;
                    return (
                      <button
                        key={mode}
                        type="button"
                        role="radio"
                        aria-checked={selected}
                        onClick={() => setEntryPerson({ ...entryPerson, mode })}
                        className={`h-8 flex-1 rounded-full text-xs font-medium transition-colors ${
                          selected ? `bg-card shadow-sm ${tone}` : 'text-muted-foreground'
                        }`}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>
              )
            )}
            <NumberPad value={entryAmount} onChange={setEntryAmount} />
            <DateTimePicker value={entryDate} onChange={setEntryDate} />
            <Input
              type="text"
              placeholder="Note (optional)"
              value={entryNote}
              maxLength={MAX_NOTE_LENGTH}
              onChange={(e) => setEntryNote(stripLeading(e.target.value))}
            />
            <div className="flex gap-2">
              <Button onClick={handleEntrySubmit} className="flex-1">
                {entryPerson?.editing ? 'Save changes' : 'Save'}
              </Button>
              <Button variant="secondary" onClick={() => setEntryPerson(null)}>
                Cancel
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>

      {/* Settle up dialog */}
      <Dialog
        open={settlePerson !== null}
        onOpenChange={(v) => {
          if (!v) setSettlePerson(null);
        }}
      >
        <DialogContent className="bg-card mx-auto w-11/12">
          <DialogHeader>
            <DialogTitle>Settle up with {settlePerson?.person.name}</DialogTitle>
          </DialogHeader>
          <div className="space-y-3">
            <p className="text-muted-foreground text-xs">
              {settlePerson && settlePerson.balance > 0
                ? `They owe you ${formatCurrency(settlePerson.balance, false, hideAmounts)}. Record what they paid you back.`
                : `You owe ${formatCurrency(Math.abs(settlePerson?.balance ?? 0), false, hideAmounts)}. Record what you paid them.`}
            </p>
            <NumberPad value={settleAmount} onChange={setSettleAmount} />
            {settleOverLimit && (
              <p className="text-destructive text-xs">
                That's more than the {formatCurrency(settleLimit, false, hideAmounts)} outstanding.
              </p>
            )}

            {openAccounts.length === 0 ? (
              <p className="text-destructive text-xs">
                Add an account first — settling up records a real transaction.
              </p>
            ) : (
              <Select value={settleAccountId} onValueChange={(v) => setSettleAccountId(v ?? '')}>
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {openAccounts.find((a) => a.id === settleAccountId)?.name ?? 'Choose account'}
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

            <Input
              type="text"
              placeholder="Note (optional)"
              value={settleNote}
              maxLength={MAX_NOTE_LENGTH}
              onChange={(e) => setSettleNote(stripLeading(e.target.value))}
            />
            <div className="flex gap-2">
              <Button
                onClick={handleSettleSubmit}
                disabled={openAccounts.length === 0 || settleOverLimit}
                className="flex-1"
              >
                Settle
              </Button>
              <Button variant="secondary" onClick={() => setSettlePerson(null)}>
                Cancel
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </>
  );
}

interface PersonCardProps {
  status: PersonBalance;
  entries: DebtEntry[];
  expanded: boolean;
  onToggleHistory: () => void;
  onLend: () => void;
  onBorrow: () => void;
  onSettle: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onEditEntry: (entry: DebtEntry) => void;
  onDeleteEntry: (id: string) => void;
}

function PersonCard({
  status,
  entries,
  expanded,
  onToggleHistory,
  onLend,
  onBorrow,
  onSettle,
  onEdit,
  onDelete,
  onEditEntry,
  onDeleteEntry,
}: PersonCardProps) {
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const { person, balance, lastActivity } = status;
  const isSettled = balance === 0;
  const theyOweYou = balance > 0;

  return (
    <div className="card-elevated rounded-md p-4">
      <div className="mb-2 flex items-center justify-between">
        <div className="min-w-0">
          <p className="truncate text-sm font-medium">{person.name}</p>
          <p className="text-muted-foreground truncate text-[11px]">
            {lastActivity ? `Last activity ${formatShortDate(lastActivity)}` : 'No activity yet'}
          </p>
        </div>
        <div className="flex shrink-0 items-center">
          <Button
            variant="ghost"
            size="icon"
            onClick={onEdit}
            className="h-7 w-7"
            aria-label={`Edit ${person.name}`}
          >
            <Pencil size={13} className="text-muted-foreground" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={onDelete}
            className="h-7 w-7"
            aria-label={`Delete ${person.name}`}
          >
            <Trash2 size={13} className="text-destructive" />
          </Button>
        </div>
      </div>

      <p
        className={`mb-3 text-sm font-medium ${
          isSettled ? 'text-muted-foreground' : theyOweYou ? 'text-positive' : 'text-destructive'
        }`}
      >
        {isSettled
          ? 'Settled up'
          : theyOweYou
            ? `Owes you ${formatCurrency(balance, false, hideAmounts)}`
            : `You owe ${formatCurrency(Math.abs(balance), false, hideAmounts)}`}
      </p>

      <div className="flex gap-2">
        <Button
          variant="secondary"
          onClick={onLend}
          className="bg-positive/10 text-positive hover:bg-positive/15 flex-1 text-xs"
        >
          <Plus size={13} className="mr-1" /> They owe me
        </Button>
        <Button variant="secondary" onClick={onBorrow} className="flex-1 text-xs">
          <Minus size={13} className="mr-1" /> I owe them
        </Button>
      </div>
      {!isSettled && (
        <Button
          variant="secondary"
          onClick={onSettle}
          className="bg-accent text-accent-foreground hover:bg-accent/80 mt-2 w-full text-xs"
        >
          Settle up
        </Button>
      )}

      <button
        onClick={onToggleHistory}
        className="text-muted-foreground mt-2 flex items-center gap-1 text-[11px] font-medium"
        aria-expanded={expanded}
      >
        <History size={12} />
        History
        <ChevronDown
          size={12}
          className={expanded ? 'rotate-180 transition-transform' : 'transition-transform'}
        />
      </button>

      {expanded && (
        <div className="border-border mt-2 space-y-1.5 border-t pt-2">
          {entries.length === 0 ? (
            <p className="text-muted-foreground text-[11px]">No entries logged yet.</p>
          ) : (
            entries.map((e) => (
              <div key={e.id} className="flex items-center gap-2 text-[11px]">
                <span className="text-muted-foreground w-14 shrink-0">
                  {formatDayMonth(e.date)}
                </span>
                <span className="min-w-0 flex-1 truncate">
                  {e.note ||
                    (e.settledTransactionId
                      ? 'Settled up'
                      : e.amount < 0
                        ? 'You owe more'
                        : 'They owe more')}
                </span>
                <span
                  className={`shrink-0 font-medium ${e.amount < 0 ? 'text-destructive' : 'text-positive'}`}
                >
                  {e.amount < 0 ? '-' : '+'}
                  {formatCurrency(Math.abs(e.amount), true, hideAmounts)}
                </span>
                <button
                  onClick={() => onEditEntry(e)}
                  aria-label="Edit entry"
                  className="text-muted-foreground hover:bg-muted -my-1 shrink-0 rounded-full p-1"
                >
                  <Pencil size={12} />
                </button>
                <button
                  onClick={() => onDeleteEntry(e.id)}
                  aria-label="Delete entry"
                  className="text-muted-foreground hover:bg-muted -my-1 shrink-0 rounded-full p-1"
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
