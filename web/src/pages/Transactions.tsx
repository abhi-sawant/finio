import { useMemo, useState, useRef, useCallback, useEffect, useLayoutEffect } from 'react';
import { useNavigate } from 'react-router';
import { Search, Filter, X, Download, Tag, Tags, Trash2, Plus } from 'lucide-react';
import { useWindowVirtualizer } from '@tanstack/react-virtual';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { cn } from '@/lib/utils';
import { downloadBlob } from '@/services/download';
import { formatCurrency, formatDate, todayKey } from '@/utils/formatters';
import { MAX_NAME_LENGTH, cleanText, isRangeInverted, stripLeading } from '@/utils/validation';
import {
  activeAccounts,
  buildSearchIndex,
  groupTransactionsByDate,
  isCategoryValidForType,
  miscLast,
  transactionMatchesQuery,
  transactionsToCsv,
} from '@/utils/calculations';
import {
  TransactionItem,
  type TransactionRowAction,
} from '@/components/transactions/TransactionItem';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { Button } from '@/components/ui/button';
import { DatePicker } from '@/components/ui/date-picker';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import type { Transaction, TransactionType } from '@/types';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

/** One virtualized item = one day: its date heading plus that day's rows in a single card. */
type DateGroup = { date: string; transactions: Transaction[] };

export default function Transactions() {
  const navigate = useNavigate();
  const transactions = useFinanceStore((s) => s.transactions);
  const categories = useFinanceStore((s) => s.categories);
  const accounts = useFinanceStore((s) => s.accounts);
  const labels = useFinanceStore((s) => s.labels);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const addTransaction = useFinanceStore((s) => s.addTransaction);
  const deleteTransaction = useFinanceStore((s) => s.deleteTransaction);
  const restoreTransaction = useFinanceStore((s) => s.restoreTransaction);
  const bulkDeleteTransactions = useFinanceStore((s) => s.bulkDeleteTransactions);
  const restoreTransactions = useFinanceStore((s) => s.restoreTransactions);
  const bulkRecategorize = useFinanceStore((s) => s.bulkRecategorize);
  const bulkAddLabel = useFinanceStore((s) => s.bulkAddLabel);
  const addTemplate = useFinanceStore((s) => s.addTemplate);

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState<TransactionType | 'all'>('all');
  const [accountFilter, setAccountFilter] = useState<string>('all');
  const [categoryFilter, setCategoryFilter] = useState<string>('all');
  const [labelFilter, setLabelFilter] = useState<string>('all');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [showFilters, setShowFilters] = useState(false);

  const filterableAccounts = useMemo(() => activeAccounts(accounts), [accounts]);

  // Debounce the search query — filtering, sorting and grouping the full list on every
  // keystroke is fine at hundreds of rows but not at thousands.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 200);
    return () => clearTimeout(timer);
  }, [search]);

  const searchIndex = useMemo(
    () => buildSearchIndex(categories, accounts, labels),
    [categories, accounts, labels],
  );

  // Bulk selection mode, entered via a row's long-press menu.
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

  // Expose selection mode to the app shell (Layout hides the FAB while it's on) without a
  // store flag: `<body data-tx-selecting>`.
  useEffect(() => {
    if (!selectionMode) return;
    document.body.dataset.txSelecting = 'true';
    return () => {
      delete document.body.dataset.txSelecting;
    };
  }, [selectionMode]);

  // "Save as template" naming dialog.
  const [templateTx, setTemplateTx] = useState<Transaction | null>(null);
  const [templateName, setTemplateName] = useState('');

  // Bulk recategorize / add-label dialogs.
  const [recategorizeOpen, setRecategorizeOpen] = useState(false);
  const [recategorizeCategoryId, setRecategorizeCategoryId] = useState('');
  const [addLabelOpen, setAddLabelOpen] = useState(false);
  const [addLabelId, setAddLabelId] = useState('');

  // The window scrolls (not <main>), so the virtualizer needs the list's offset from the top
  // of the document — everything above it (search, totals, the filter panel) is its scrollMargin.
  const listRef = useRef<HTMLDivElement>(null);
  const [scrollMargin, setScrollMargin] = useState(0);

  const filtered = useMemo(() => {
    const q = debouncedSearch.trim();
    const fromMs = fromDate ? new Date(fromDate + 'T00:00:00').getTime() : null;
    const toMs = toDate ? new Date(toDate + 'T23:59:59').getTime() : null;

    return transactions.filter((t) => {
      if (typeFilter !== 'all' && t.type !== typeFilter) return false;
      if (
        accountFilter !== 'all' &&
        t.accountId !== accountFilter &&
        t.toAccountId !== accountFilter
      )
        return false;
      if (categoryFilter !== 'all') {
        const inSplits = t.splits?.some((s) => s.categoryId === categoryFilter);
        if (t.categoryId !== categoryFilter && !inSplits) return false;
      }
      if (labelFilter !== 'all' && !t.labels.includes(labelFilter)) return false;
      if (fromMs !== null || toMs !== null) {
        const ts = new Date(t.date).getTime();
        if (fromMs !== null && ts < fromMs) return false;
        if (toMs !== null && ts > toMs) return false;
      }
      return transactionMatchesQuery(t, q, searchIndex);
    });
  }, [
    transactions,
    debouncedSearch,
    typeFilter,
    accountFilter,
    categoryFilter,
    labelFilter,
    searchIndex,
    fromDate,
    toDate,
  ]);

  const { totalIncome, totalExpense } = useMemo(() => {
    let income = 0;
    let expense = 0;
    for (const t of filtered) {
      if (t.type === 'income') income += t.amount;
      else if (t.type === 'expense') expense += t.amount;
    }
    return { totalIncome: income, totalExpense: expense };
  }, [filtered]);

  // Each virtual item is a whole day, so a day's rows can share one glass card. Days are short
  // (a handful of rows), so virtualizing per day keeps the DOM small at any ledger size.
  const groups = useMemo<DateGroup[]>(() => groupTransactionsByDate(filtered), [filtered]);

  const virtualizer = useWindowVirtualizer({
    count: groups.length,
    estimateSize: (index) => 40 + groups[index].transactions.length * 72,
    overscan: 4,
    scrollMargin,
  });

  // Keep scrollMargin in step with whatever sits above the list (filter panel opening, the
  // header growing, fonts loading). Body resizes whenever any of that moves.
  useLayoutEffect(() => {
    const measure = () => {
      const el = listRef.current;
      if (!el) return;
      const top = Math.round(el.getBoundingClientRect().top + window.scrollY);
      setScrollMargin((prev) => (prev === top ? prev : top));
    };
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(document.body);
    return () => ro.disconnect();
  }, [groups.length, showFilters]);

  // What the selection can be recategorized to. Transfers keep their Transfer category, so
  // they're skipped; a mixed expense + income selection can only move to a neutral category.
  const selectionCategorizable = useMemo(() => {
    if (!recategorizeOpen) return { ids: [] as string[], skipped: 0, types: new Set<string>() };
    const ids: string[] = [];
    const types = new Set<string>();
    let skipped = 0;
    for (const t of transactions) {
      if (!selectedIds.has(t.id)) continue;
      if (t.type === 'transfer') {
        skipped++;
        continue;
      }
      ids.push(t.id);
      types.add(t.type);
    }
    return { ids, skipped, types };
  }, [recategorizeOpen, transactions, selectedIds]);

  const recategorizeOptions = useMemo(() => {
    const { types } = selectionCategorizable;
    if (types.size === 0) return [];
    return miscLast(categories).filter((c) =>
      types.size === 1
        ? isCategoryValidForType(c, types.has('income') ? 'income' : 'expense')
        : isCategoryValidForType(c, 'expense') && isCategoryValidForType(c, 'income'),
    );
  }, [selectionCategorizable, categories]);

  const hasActiveFilters =
    typeFilter !== 'all' ||
    accountFilter !== 'all' ||
    categoryFilter !== 'all' ||
    labelFilter !== 'all' ||
    !!fromDate ||
    !!toDate;

  const handleExportCsv = () => {
    if (filtered.length === 0) {
      toast.error('No transactions to export');
      return;
    }
    // Newest first, the same order as the list on screen.
    const sorted = [...filtered].sort(
      (a, b) =>
        new Date(b.date).getTime() - new Date(a.date).getTime() ||
        b.createdAt.localeCompare(a.createdAt),
    );
    const csv = transactionsToCsv(sorted, categories, accounts);
    downloadBlob(`finio-transactions-${todayKey()}.csv`, csv, 'text/csv;charset=utf-8');
    toast.success(`Exported ${filtered.length} transactions`);
  };

  const handleNavigate = useCallback(
    (id: string) => navigate(`/edit-transaction/${id}`),
    [navigate],
  );

  const enterSelectionMode = (tx: Transaction) => {
    setSelectionMode(true);
    setSelectedIds(new Set([tx.id]));
  };

  const exitSelectionMode = () => {
    setSelectionMode(false);
    setSelectedIds(new Set());
  };

  const toggleSelected = (id: string) => {
    setSelectedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const handleRowLongPressAction = (action: TransactionRowAction, tx: Transaction) => {
    if (action === 'select') {
      enterSelectionMode(tx);
      return;
    }
    if (action === 'duplicate') {
      // A duplicate is a fresh, manually-entered transaction — dated today, not linked to
      // whatever recurring rule (if any) generated the original.
      const newId = addTransaction({
        type: tx.type,
        amount: tx.amount,
        accountId: tx.accountId,
        ...(tx.toAccountId ? { toAccountId: tx.toAccountId } : {}),
        categoryId: tx.categoryId,
        date: new Date().toISOString(),
        note: tx.note,
        ...(tx.merchant ? { merchant: tx.merchant } : {}),
        ...(tx.forWhom ? { forWhom: tx.forWhom } : {}),
        labels: tx.labels,
        ...(tx.splits ? { splits: tx.splits } : {}),
      });
      toast.success('Transaction duplicated', {
        action: { label: 'Undo', onClick: () => deleteTransaction(newId) },
      });
      return;
    }
    if (action === 'template') {
      setTemplateName(tx.note || '');
      setTemplateTx(tx);
      return;
    }
    // delete
    const removed = deleteTransaction(tx.id);
    if (!removed) return;
    toast.success('Transaction deleted', {
      action: { label: 'Undo', onClick: () => restoreTransaction(removed) },
    });
  };

  const handleSaveTemplate = () => {
    if (!templateTx) return;
    addTemplate({
      name:
        cleanText(templateName, MAX_NAME_LENGTH) ||
        cleanText(templateTx.note, MAX_NAME_LENGTH) ||
        'Template',
      type: templateTx.type,
      amount: templateTx.amount,
      accountId: templateTx.accountId,
      ...(templateTx.toAccountId ? { toAccountId: templateTx.toAccountId } : {}),
      categoryId: templateTx.categoryId,
      note: templateTx.note,
      labels: templateTx.labels,
      ...(templateTx.splits ? { splits: templateTx.splits } : {}),
    });
    toast.success('Template saved');
    setTemplateTx(null);
    setTemplateName('');
  };

  const handleBulkDelete = () => {
    const ids = Array.from(selectedIds);
    const removed = bulkDeleteTransactions(ids);
    exitSelectionMode();
    if (removed.length === 0) return;
    toast.success(`Deleted ${removed.length} transaction${removed.length === 1 ? '' : 's'}`, {
      action: { label: 'Undo', onClick: () => restoreTransactions(removed) },
    });
  };

  const handleBulkRecategorize = () => {
    if (!recategorizeCategoryId) return;
    const { ids, skipped } = selectionCategorizable;
    if (ids.length === 0) return;
    bulkRecategorize(ids, recategorizeCategoryId);
    const count = ids.length;
    toast.success(
      `Recategorized ${count} transaction${count === 1 ? '' : 's'}` +
        (skipped > 0 ? ` · ${skipped} transfer${skipped === 1 ? '' : 's'} skipped` : ''),
    );
    setRecategorizeOpen(false);
    setRecategorizeCategoryId('');
    exitSelectionMode();
  };

  const handleBulkAddLabel = () => {
    if (!addLabelId) return;
    const count = selectedIds.size;
    bulkAddLabel(Array.from(selectedIds), addLabelId);
    toast.success(`Label added to ${count} transaction${count === 1 ? '' : 's'}`);
    setAddLabelOpen(false);
    setAddLabelId('');
    exitSelectionMode();
  };

  const items = virtualizer.getVirtualItems();

  const clearFilters = () => {
    setTypeFilter('all');
    setAccountFilter('all');
    setCategoryFilter('all');
    setLabelFilter('all');
    setFromDate('');
    setToDate('');
  };

  const clearSearchAndFilters = () => {
    clearFilters();
    setSearch('');
    setDebouncedSearch('');
  };

  return (
    <>
      {/* Header */}
      <Header>
        <h1 className="text-2xl font-bold tracking-tight">Transactions</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton onClick={handleExportCsv} aria-label="Export CSV">
            <Download />
          </HeaderIconButton>
          <HeaderIconButton
            onClick={() => setShowFilters(!showFilters)}
            aria-label="Toggle filters"
            pressed={hasActiveFilters}
          >
            <Filter />
          </HeaderIconButton>
        </div>
      </Header>
      {/* While selecting, leave room for the bulk toolbar so it never covers the last row. */}
      <Main className={cn(selectionMode && 'lg:pb-24')}>
        {/* Search */}
        <div className="relative">
          <Search
            size={16}
            className="text-muted-foreground absolute top-1/2 left-3 z-10 -translate-y-1/2"
          />
          <Input
            type="text"
            placeholder="Search transactions"
            aria-label="Search notes, categories, accounts, labels and amounts"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pr-4 pl-9"
          />
        </div>

        <div className="flex items-center justify-end gap-3">
          <span className="text-muted-foreground text-xs">
            Earned{' '}
            <span className="text-positive font-bold">
              {formatCurrency(totalIncome, false, hideAmounts)}
            </span>
          </span>
          <span className="text-muted-foreground text-xs">
            Spent{' '}
            <span className="text-foreground font-bold">
              {formatCurrency(totalExpense, false, hideAmounts)}
            </span>
          </span>
        </div>

        {/* Filters */}
        {showFilters && (
          <div className="card-elevated space-y-3 rounded-md p-3">
            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">Type</Label>
              <div className="flex flex-wrap gap-2">
                {(['all', 'expense', 'income', 'transfer'] as const).map((type) => (
                  <button
                    key={type}
                    onClick={() => setTypeFilter(type)}
                    aria-pressed={typeFilter === type}
                    className={cn(
                      'rounded-full px-3 py-1.5 text-xs font-medium capitalize transition-colors',
                      typeFilter === type
                        ? 'bg-grad-primary shadow-glow-primary text-white'
                        : 'bg-muted text-muted-foreground',
                    )}
                  >
                    {type}
                  </button>
                ))}
              </div>
            </div>
            <div>
              <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                Account
              </Label>
              <Select value={accountFilter} onValueChange={(v) => setAccountFilter(v ?? 'all')}>
                <SelectTrigger className="w-full">
                  <SelectValue>
                    {accounts.find((a) => a.id === accountFilter)?.name || 'All accounts'}
                  </SelectValue>
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All accounts</SelectItem>
                  {filterableAccounts.map((a) => (
                    <SelectItem key={a.id} value={a.id}>
                      {a.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Category
                </Label>
                <Select value={categoryFilter} onValueChange={(v) => setCategoryFilter(v ?? 'all')}>
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {categories.find((c) => c.id === categoryFilter)?.name || 'All categories'}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All categories</SelectItem>
                    {miscLast(categories).map((c) => (
                      <SelectItem key={c.id} value={c.id}>
                        {c.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Label
                </Label>
                <Select value={labelFilter} onValueChange={(v) => setLabelFilter(v ?? 'all')}>
                  <SelectTrigger className="w-full">
                    <SelectValue>
                      {labels.find((l) => l.id === labelFilter)?.name || 'All labels'}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">All labels</SelectItem>
                    {labels.map((l) => (
                      <SelectItem key={l.id} value={l.id}>
                        {l.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  From
                </Label>
                <DatePicker
                  value={fromDate}
                  onChange={setFromDate}
                  placeholder="Start date"
                  maxDate={toDate || undefined}
                />
              </div>
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">To</Label>
                <DatePicker
                  value={toDate}
                  onChange={setToDate}
                  placeholder="End date"
                  minDate={fromDate || undefined}
                />
              </div>
            </div>
            {isRangeInverted(fromDate, toDate) && (
              <p role="alert" className="text-destructive text-xs">
                The from date is after the to date — no transactions can match.
              </p>
            )}
            {hasActiveFilters && (
              <Button
                variant="ghost"
                onClick={clearFilters}
                className="text-destructive hover:text-destructive flex h-auto items-center gap-1 p-0 text-xs font-medium hover:bg-transparent"
              >
                <X size={12} /> Clear filters
              </Button>
            )}
          </div>
        )}

        {/* Transaction list */}
        {groups.length === 0 ? (
          transactions.length === 0 ? (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <p className="font-medium">No transactions yet</p>
              <p className="text-muted-foreground max-w-xs text-sm">
                Log your first expense or income and it will show up here.
              </p>
              <Button onClick={() => navigate('/add-transaction')}>
                <Plus /> Add transaction
              </Button>
            </div>
          ) : (
            <div className="flex flex-col items-center gap-3 py-12 text-center">
              <p className="font-medium">No matching transactions</p>
              <p className="text-muted-foreground max-w-xs text-sm">
                Nothing matches your search or filters.
              </p>
              <Button variant="outline" onClick={clearSearchAndFilters}>
                <X /> Clear search and filters
              </Button>
            </div>
          )
        ) : (
          <div
            ref={listRef}
            style={{ height: `${virtualizer.getTotalSize()}px` }}
            className="relative w-full"
          >
            {items.map((virtualItem) => {
              const group = groups[virtualItem.index];
              return (
                <section
                  key={virtualItem.key}
                  data-index={virtualItem.index}
                  ref={virtualizer.measureElement}
                  style={{
                    transform: `translateY(${virtualItem.start - virtualizer.options.scrollMargin}px)`,
                  }}
                  className="absolute top-0 left-0 w-full pb-3"
                  aria-label={formatDate(group.date)}
                >
                  <h2 className="text-muted-foreground ps-2 pb-1.5 text-xs font-medium">
                    {formatDate(group.date)}
                  </h2>
                  <div className="card-elevated divide-border divide-y overflow-hidden rounded-md">
                    {group.transactions.map((tx) => (
                      <TransactionItem
                        key={tx.id}
                        transaction={tx}
                        categories={categories}
                        accounts={accounts}
                        labels={labels}
                        selectionMode={selectionMode}
                        selected={selectedIds.has(tx.id)}
                        onLongPressAction={handleRowLongPressAction}
                        onClick={() =>
                          selectionMode ? toggleSelected(tx.id) : handleNavigate(tx.id)
                        }
                      />
                    ))}
                  </div>
                </section>
              );
            })}
          </div>
        )}
      </Main>

      {/* Bulk selection toolbar */}
      {selectionMode && (
        // Mobile: a floating glass pill beside the FAB (left of it, level with it) so the coin
        // can never cover an action. Desktop: a full-width glass bar under the content column.
        <div
          role="toolbar"
          aria-label="Selected transactions"
          className="glass-chrome fixed right-[5.25rem] bottom-[calc(env(safe-area-inset-bottom,0px)+5.5rem)] left-3 z-40 flex h-14 items-center gap-2 rounded-full border border-[var(--glass-border)] pr-1.5 pl-4 shadow-[var(--shadow-float)] lg:right-0 lg:bottom-0 lg:left-60 lg:h-auto lg:rounded-none lg:border-x-0 lg:border-b-0 lg:py-2.5 lg:pr-4 lg:shadow-[0_-12px_30px_-22px_rgb(40_26_110/0.45)]"
        >
          <span className="shrink-0 text-sm font-medium">{selectedIds.size} selected</span>
          <div className="ml-auto flex items-center gap-0.5">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setAddLabelOpen(true)}
              disabled={selectedIds.size === 0}
              className="h-9 w-9 rounded-full"
              aria-label="Add label"
            >
              <Tags size={16} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setRecategorizeOpen(true)}
              disabled={selectedIds.size === 0}
              className="h-9 w-9 rounded-full"
              aria-label="Recategorize"
            >
              <Tag size={16} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={handleBulkDelete}
              disabled={selectedIds.size === 0}
              className="text-destructive hover:bg-destructive/10 h-9 w-9 rounded-full"
              aria-label="Delete selected"
            >
              <Trash2 size={16} />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={exitSelectionMode}
              className="h-9 w-9 rounded-full"
              aria-label="Cancel selection"
            >
              <X size={16} />
            </Button>
          </div>
        </div>
      )}

      {/* Save as template */}
      <Dialog open={!!templateTx} onOpenChange={(open) => !open && setTemplateTx(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Save as template</DialogTitle>
            <DialogDescription>
              Reuse this transaction's account, category, amount and labels from the FAB later.
            </DialogDescription>
          </DialogHeader>
          <Input
            autoFocus
            placeholder="Template name"
            value={templateName}
            maxLength={MAX_NAME_LENGTH}
            onChange={(e) => setTemplateName(stripLeading(e.target.value))}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setTemplateTx(null)}>
              Cancel
            </Button>
            <Button onClick={handleSaveTemplate}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Bulk recategorize */}
      <Dialog open={recategorizeOpen} onOpenChange={setRecategorizeOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              Recategorize {selectionCategorizable.ids.length} transaction
              {selectionCategorizable.ids.length === 1 ? '' : 's'}
            </DialogTitle>
            <DialogDescription>
              {selectionCategorizable.ids.length === 0
                ? 'Transfers keep their Transfer category, so there is nothing here to recategorize.'
                : selectionCategorizable.types.size > 1
                  ? 'Your selection mixes expenses and income, so only categories that fit both are offered.'
                  : 'Every selected transaction moves to this category.'}
            </DialogDescription>
          </DialogHeader>
          {selectionCategorizable.skipped > 0 && (
            <p className="bg-warning/15 text-foreground rounded-md px-3 py-2 text-xs">
              {selectionCategorizable.skipped} transfer
              {selectionCategorizable.skipped === 1 ? '' : 's'} in your selection will be skipped.
            </p>
          )}
          <Select
            value={recategorizeCategoryId}
            onValueChange={(v) => setRecategorizeCategoryId(v ?? '')}
          >
            <SelectTrigger
              className="w-full"
              disabled={recategorizeOptions.length === 0}
              aria-label="Category"
            >
              <SelectValue placeholder="Select category">
                {categories.find((c) => c.id === recategorizeCategoryId)?.name}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {recategorizeOptions.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRecategorizeOpen(false)}>
              Cancel
            </Button>
            <Button
              onClick={handleBulkRecategorize}
              disabled={
                !recategorizeCategoryId ||
                !recategorizeOptions.some((c) => c.id === recategorizeCategoryId)
              }
            >
              Apply
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Bulk add label */}
      <Dialog open={addLabelOpen} onOpenChange={setAddLabelOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              Add label to {selectedIds.size} transaction{selectedIds.size === 1 ? '' : 's'}
            </DialogTitle>
            <DialogDescription>
              Adds the label alongside any labels a transaction already has.
            </DialogDescription>
          </DialogHeader>
          <Select value={addLabelId} onValueChange={(v) => setAddLabelId(v ?? '')}>
            <SelectTrigger className="w-full" aria-label="Label">
              <SelectValue placeholder="Select label">
                {labels.find((l) => l.id === addLabelId)?.name}
              </SelectValue>
            </SelectTrigger>
            <SelectContent>
              {labels.map((l) => (
                <SelectItem key={l.id} value={l.id}>
                  {l.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAddLabelOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleBulkAddLabel} disabled={!addLabelId}>
              Apply
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
