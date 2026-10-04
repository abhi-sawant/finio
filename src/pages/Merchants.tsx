import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { ArrowLeft, ChevronDown, ChevronUp, Store, Wand2 } from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import {
  summarizeMerchants,
  type MerchantSummary,
  type MerchantTransactionType,
} from '@/utils/merchants';
import { formatCurrency, formatShortDate, shouldCompactGroup } from '@/utils/formatters';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { TransactionItem } from '@/components/transactions/TransactionItem';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';

const TYPE_CHIPS: { value: MerchantTransactionType; label: string }[] = [
  { value: 'expense', label: 'Spending' },
  { value: 'income', label: 'Income' },
];

function MerchantRow({
  merchant,
  expanded,
  onToggle,
  compact,
  hideAmounts,
}: {
  merchant: MerchantSummary;
  expanded: boolean;
  onToggle: () => void;
  compact: boolean;
  hideAmounts: boolean;
}) {
  const navigate = useNavigate();
  const categories = useFinanceStore((s) => s.categories);
  const accounts = useFinanceStore((s) => s.accounts);

  return (
    <div>
      <button
        onClick={onToggle}
        className="hover:bg-muted/40 flex w-full items-center justify-between px-4 py-3"
        aria-expanded={expanded}
      >
        <div className="min-w-0 text-left">
          <p className="truncate text-sm font-medium">{merchant.displayName}</p>
          <p className="text-muted-foreground text-xs">
            {merchant.transactionCount} transaction{merchant.transactionCount === 1 ? '' : 's'} ·
            last on {formatShortDate(merchant.lastDate)}
          </p>
        </div>
        <div className="flex items-center gap-2 pl-3">
          <p
            className={`text-sm font-semibold ${merchant.type === 'income' ? 'text-positive' : ''}`}
          >
            {formatCurrency(merchant.totalAmount, true, hideAmounts, { forceCompact: compact })}
          </p>
          {expanded ? (
            <ChevronUp size={16} className="text-muted-foreground" />
          ) : (
            <ChevronDown size={16} className="text-muted-foreground" />
          )}
        </div>
      </button>

      {expanded && (
        <div className="bg-muted/30 border-border border-t px-1 pb-1">
          {merchant.transactions.map((t) => (
            <TransactionItem
              key={t.id}
              transaction={t}
              categories={categories}
              accounts={accounts}
              showDate
              dateStyle="short"
              onClick={() => navigate(`/edit-transaction/${t.id}`)}
            />
          ))}
          <button
            onClick={() =>
              navigate('/category-rules', {
                state: { pattern: merchant.displayName, scope: merchant.type },
              })
            }
            className="text-primary hover:bg-accent flex w-full items-center justify-center gap-1.5 rounded-full p-2.5 text-xs font-medium"
          >
            <Wand2 size={13} />
            Create a rule for "{merchant.displayName}"
          </button>
        </div>
      )}
    </div>
  );
}

export default function Merchants() {
  const navigate = useNavigate();
  const transactions = useFinanceStore((s) => s.transactions);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const [type, setType] = useState<MerchantTransactionType>('expense');
  const [expandedKey, setExpandedKey] = useState<string | null>(null);

  const merchants = useMemo(() => summarizeMerchants(transactions, type), [transactions, type]);
  const compact = useMemo(
    () => shouldCompactGroup(merchants.map((m) => m.totalAmount)),
    [merchants],
  );
  const total = useMemo(() => merchants.reduce((sum, m) => sum + m.totalAmount, 0), [merchants]);

  return (
    <>
      <Header innerClassName="lg:max-w-2xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Merchants</h1>
        <HideAmountsToggle />
      </Header>

      <Main className="lg:max-w-2xl">
        <div className="flex gap-2">
          {TYPE_CHIPS.map((chip) => (
            <button
              key={chip.value}
              onClick={() => {
                setType(chip.value);
                setExpandedKey(null);
              }}
              aria-pressed={type === chip.value}
              className={`rounded-full px-4 py-1.5 text-xs font-medium transition-colors ${
                type === chip.value
                  ? 'bg-grad-primary shadow-glow-primary text-white'
                  : 'bg-muted text-muted-foreground'
              }`}
            >
              {chip.label}
            </button>
          ))}
        </div>

        {merchants.length > 0 && (
          <div className="card-elevated rounded-md p-4">
            <div className="mb-1 flex items-center gap-1.5">
              <Store size={12} className="text-primary" />
              <p className="text-muted-foreground text-xs font-medium">
                {merchants.length} merchant{merchants.length === 1 ? '' : 's'}
              </p>
            </div>
            <p className="font-money text-lg">{formatCurrency(total, true, hideAmounts)}</p>
          </div>
        )}

        {merchants.length > 0 && (
          <div className="card-elevated divide-border divide-y overflow-hidden rounded-md">
            {merchants.map((merchant) => (
              <MerchantRow
                key={merchant.key}
                merchant={merchant}
                expanded={expandedKey === merchant.key}
                onToggle={() => setExpandedKey((k) => (k === merchant.key ? null : merchant.key))}
                compact={compact}
                hideAmounts={hideAmounts}
              />
            ))}
          </div>
        )}

        {merchants.length === 0 && (
          <p className="text-muted-foreground py-12 text-center text-sm">
            No noted {type === 'expense' ? 'spending' : 'income'} yet — add a note to a transaction
            to see it show up here.
          </p>
        )}
      </Main>
    </>
  );
}
