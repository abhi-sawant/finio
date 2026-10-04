import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { Plus, ChevronDown, ChevronRight } from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { formatCurrency, shouldCompactGroup } from '@/utils/formatters';
import {
  activeAccounts,
  getTotalAccountBalance,
  getTotalCreditOutstanding,
  getTotalDepositValue,
} from '@/utils/calculations';
import { accountDeleteBlockers, isDepositAccount } from '@/utils/deposit';
import type { Account } from '@/types';
import { AccountCard } from '@/components/accounts/AccountCard';
import { HideAmountsToggle } from '@/components/HideAmountsToggle';
import { useConfirm } from '@/components/ui/use-confirm';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import { NoteCard } from '@/components/ui/note-card';
import { noteFigureClass } from '@/components/ui/note-figure';

export default function Accounts() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const accounts = useFinanceStore((s) => s.accounts);
  const transactions = useFinanceStore((s) => s.transactions);
  const deleteAccount = useFinanceStore((s) => s.deleteAccount);
  const setAccountArchived = useFinanceStore((s) => s.setAccountArchived);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);

  const [showArchived, setShowArchived] = useState(false);

  const totalBalance = useMemo(() => getTotalAccountBalance(accounts), [accounts]);
  const creditDue = useMemo(() => getTotalCreditOutstanding(accounts), [accounts]);
  const depositValue = useMemo(() => getTotalDepositValue(accounts), [accounts]);

  const open = useMemo(() => activeAccounts(accounts), [accounts]);
  const regularAccounts = useMemo(
    () => open.filter((a) => a.type !== 'credit' && !isDepositAccount(a)),
    [open],
  );
  const depositAccounts = useMemo(() => open.filter(isDepositAccount), [open]);
  const creditAccounts = useMemo(() => open.filter((a) => a.type === 'credit'), [open]);
  const archivedAccounts = useMemo(() => accounts.filter((a) => a.archivedAt), [accounts]);
  // Open accounts are all visible on this page together, so their balances compact as one
  // group; archived accounts are a separate, collapsed context.
  const openCompact = useMemo(() => shouldCompactGroup(open.map((a) => a.balance)), [open]);
  const archivedCompact = useMemo(
    () => shouldCompactGroup(archivedAccounts.map((a) => a.balance)),
    [archivedAccounts],
  );
  const txCountByAccount = useMemo(() => {
    const counts = new Map<string, number>();
    for (const t of transactions) {
      if (t.accountId) counts.set(t.accountId, (counts.get(t.accountId) ?? 0) + 1);
      if (t.toAccountId) counts.set(t.toAccountId, (counts.get(t.toAccountId) ?? 0) + 1);
    }
    return counts;
  }, [transactions]);

  const handleDelete = async (account: Account) => {
    const blockers = accountDeleteBlockers(accounts, account.id);
    if (blockers.length > 0) {
      toast.error(`Can't delete "${account.name}"`, {
        description: `${blockers.map((b) => `"${b}"`).join(', ')} pay${blockers.length === 1 ? 's' : ''} out to this account. Delete ${blockers.length === 1 ? 'that deposit' : 'those deposits'} first.`,
      });
      return;
    }
    const txCount = transactions.filter(
      (t) => t.accountId === account.id || t.toAccountId === account.id,
    ).length;

    const confirmed = await confirm({
      title: `Delete "${account.name}"?`,
      description:
        txCount > 0
          ? `${txCount} transaction${txCount === 1 ? '' : 's'} on this account will be deleted too, and this cannot be undone. Archive it instead to close the account but keep its history.`
          : 'This cannot be undone.',
      confirmLabel: 'Delete permanently',
    });
    if (confirmed) deleteAccount(account.id);
  };

  const handleToggleArchive = async (account: Account) => {
    if (account.archivedAt) {
      setAccountArchived(account.id, false);
      toast.success(`"${account.name}" reopened`);
      return;
    }

    const confirmed = await confirm({
      title: `Archive "${account.name}"?`,
      description:
        'Its transactions stay in your history, but the account drops out of pickers and running totals. You can reopen it any time.',
      confirmLabel: 'Archive',
      destructive: false,
    });
    if (confirmed) {
      setAccountArchived(account.id, true);
      toast.success(`"${account.name}" archived`, {
        action: { label: 'Undo', onClick: () => setAccountArchived(account.id, false) },
      });
    }
  };

  return (
    <>
      {/* Header */}
      <Header>
        <h1 className="text-2xl font-bold tracking-tight">Accounts</h1>
        <div className="flex gap-2">
          <HideAmountsToggle />
          <HeaderIconButton
            onClick={() => navigate('/add-account')}
            aria-label="Add account"
            tone="primary"
          >
            <Plus />
          </HeaderIconButton>
        </div>
      </Header>
      <Main>
        {/* Summary */}
        <NoteCard>
          <p className="text-muted-foreground text-sm font-medium">Net balance</p>
          <p
            className={`font-money mt-1 leading-[1.05] ${noteFigureClass(formatCurrency(totalBalance, false, hideAmounts))}`}
          >
            {formatCurrency(totalBalance, false, hideAmounts)}
          </p>
          {creditAccounts.length > 0 && (
            <p className="text-muted-foreground mt-1.5 text-xs text-balance">
              <span className="block">
                {formatCurrency(creditDue, false, hideAmounts)} owed on {creditAccounts.length} card
                {creditAccounts.length === 1 ? '' : 's'}
              </span>
              <span className="block">
                {formatCurrency(totalBalance - creditDue, false, hideAmounts)} after dues
              </span>
            </p>
          )}
          {depositAccounts.length > 0 && (
            <p className="text-muted-foreground mt-1 text-xs">
              + {formatCurrency(depositValue, false, hideAmounts)} locked in{' '}
              {depositAccounts.length} deposit{depositAccounts.length === 1 ? '' : 's'}
            </p>
          )}
        </NoteCard>

        {/* Regular Accounts */}
        {regularAccounts.length > 0 && (
          <div>
            <h2 className="mb-3 text-base font-semibold">Accounts</h2>
            <div className="card-elevated divide-border divide-y rounded-md px-4">
              {regularAccounts.map((account) => (
                <AccountCard
                  key={account.id}
                  account={account}
                  forceCompact={openCompact}
                  onClick={() => navigate(`/edit-account/${account.id}`)}
                  onDelete={() => handleDelete(account)}
                  onToggleArchive={() => handleToggleArchive(account)}
                />
              ))}
            </div>
          </div>
        )}

        {/* Fixed & recurring deposits — valued at what they're worth today */}
        {depositAccounts.length > 0 && (
          <div>
            <h2 className="mb-3 text-base font-semibold">Deposits</h2>
            <div className="card-elevated divide-border divide-y rounded-md px-4">
              {depositAccounts.map((account) => (
                <AccountCard
                  key={account.id}
                  account={account}
                  forceCompact={openCompact}
                  onClick={() => navigate(`/edit-account/${account.id}`)}
                  onDelete={() => handleDelete(account)}
                  onToggleArchive={() => handleToggleArchive(account)}
                />
              ))}
            </div>
          </div>
        )}

        {/* Credit Accounts */}
        {creditAccounts.length > 0 && (
          <div>
            <h2 className="mb-3 text-base font-semibold">Credit cards</h2>
            <div className="card-elevated divide-border divide-y rounded-md px-4">
              {creditAccounts.map((account) => (
                <AccountCard
                  key={account.id}
                  account={account}
                  forceCompact={openCompact}
                  onClick={() => navigate(`/edit-account/${account.id}`)}
                  onDelete={() => handleDelete(account)}
                  onToggleArchive={() => handleToggleArchive(account)}
                />
              ))}
            </div>
          </div>
        )}

        {/* Archived accounts — collapsed, since they are closed but still hold history */}
        {archivedAccounts.length > 0 && (
          <div>
            <button
              onClick={() => setShowArchived((v) => !v)}
              className="text-muted-foreground mb-3 flex items-center gap-1 text-sm font-medium"
              aria-expanded={showArchived}
            >
              {showArchived ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
              Archived ({archivedAccounts.length})
            </button>
            {showArchived && (
              <div className="card-elevated divide-border divide-y rounded-md px-4">
                {archivedAccounts.map((account) => (
                  <AccountCard
                    key={account.id}
                    account={account}
                    forceCompact={archivedCompact}
                    transactionCount={txCountByAccount.get(account.id) ?? 0}
                    onClick={() => navigate(`/edit-account/${account.id}`)}
                    onDelete={() => handleDelete(account)}
                    onToggleArchive={() => handleToggleArchive(account)}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {accounts.length === 0 && (
          <div className="py-12 text-center">
            <p className="text-muted-foreground mb-4">No accounts yet</p>
            <button
              onClick={() => navigate('/add-account')}
              className="bg-grad-primary shadow-glow-primary rounded-full px-5 py-2.5 text-sm font-medium text-white"
            >
              Add Account
            </button>
          </div>
        )}
      </Main>
    </>
  );
}
