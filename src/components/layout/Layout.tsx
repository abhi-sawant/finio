import { Outlet, useLocation, useNavigate } from 'react-router';
import { useEffect, useRef, useState } from 'react';
import { Plus, Repeat, Ellipsis } from 'lucide-react';
import { toast } from 'sonner';
import { cn } from '@/lib/utils';
import { useFinanceStore } from '@/store/useFinanceStore';
import { useAuthStore } from '@/store/useAuthStore';
import { useLongPress } from '@/hooks/useLongPress';
import { formatCurrency } from '@/utils/formatters';
import { autoBackupIfNeeded, autoLocalBackupIfNeeded } from '@/services/backup';
import { refreshNotificationSchedule, runDueNotifications } from '@/services/notifications';
import { Popover, PopoverContent } from '@/components/ui/popover';
import { Sidebar } from './Sidebar';
import { navTabs, moreNavItems } from './navItems';

export function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const isHydrated = useFinanceStore((s) => s.isHydrated);
  const processRecurring = useFinanceStore((s) => s.processRecurring);
  const processMaturities = useFinanceStore((s) => s.processMaturities);
  const captureNetWorthSnapshots = useFinanceStore((s) => s.captureNetWorthSnapshots);
  const isAuthLoaded = useAuthStore((s) => s.isLoaded);
  const templates = useFinanceStore((s) => s.templates);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const addTransaction = useFinanceStore((s) => s.addTransaction);
  const deleteTransaction = useFinanceStore((s) => s.deleteTransaction);
  const bulkDeleteTransactions = useFinanceStore((s) => s.bulkDeleteTransactions);

  const fabRef = useRef<HTMLButtonElement>(null);
  const moreRef = useRef<HTMLButtonElement>(null);
  const [moreOpen, setMoreOpen] = useState(false);
  // The FAB would cover the primary action on these screens (Accounts has its own add button).
  const hideFab = location.pathname === '/accounts' || location.pathname === '/settings';
  const moreActive = moreNavItems.some((i) => location.pathname.startsWith(i.path));
  const [templatesOpen, setTemplatesOpen] = useState(false);
  const { firedRef: fabLongPressFiredRef, handlers: fabLongPressHandlers } = useLongPress(() =>
    setTemplatesOpen(true),
  );

  const handleFabClick = () => {
    if (fabLongPressFiredRef.current) {
      fabLongPressFiredRef.current = false;
      return;
    }
    navigate('/add-transaction');
  };

  const handleUseTemplate = (templateId: string) => {
    const template = templates.find((t) => t.id === templateId);
    if (!template) return;
    const newId = addTransaction({
      type: template.type,
      amount: template.amount,
      accountId: template.accountId,
      ...(template.toAccountId ? { toAccountId: template.toAccountId } : {}),
      categoryId: template.categoryId,
      date: new Date().toISOString(),
      note: template.note,
      labels: template.labels,
      ...(template.splits ? { splits: template.splits } : {}),
    });
    setTemplatesOpen(false);
    toast.success(`Added "${template.name}"`, {
      action: { label: 'Undo', onClick: () => deleteTransaction(newId) },
    });
  };

  // Process recurring rules once on hydration. Posts silently, so the toast doubles as the
  // "recurring inbox" — a lightweight review step that lets a wrong amount or a stale rule be
  // undone before it's noticed anywhere else, without blocking app start on a confirmation.
  useEffect(() => {
    if (!isHydrated) return;
    const generated = processRecurring();
    if (generated.length > 0) {
      const ids = generated.map((t) => t.id);
      toast.success(
        `Added ${generated.length} recurring transaction${generated.length === 1 ? '' : 's'}`,
        { action: { label: 'Undo', onClick: () => bulkDeleteTransactions(ids) } },
      );
    }
  }, [isHydrated, processRecurring, bulkDeleteTransactions]);

  // Pay out any fixed/recurring deposit that has reached maturity. After recurring processing,
  // so an RD's final installment is in before its payout, and before snapshots, so the month it
  // matured in counts the money where it landed. No Undo: maturity is a fact, not a guess.
  useEffect(() => {
    if (!isHydrated) return;
    const posted = processMaturities();
    if (posted.length === 0) return;
    const { accounts, settings } = useFinanceStore.getState();
    for (const tx of posted) {
      if (tx.type !== 'transfer') continue;
      const deposit = accounts.find((a) => a.id === tx.accountId);
      const target = accounts.find((a) => a.id === tx.toAccountId);
      toast.success(
        `${deposit?.type === 'rd' ? 'RD' : 'FD'} "${deposit?.name ?? ''}" matured — ${formatCurrency(tx.amount, false, settings.hideAmounts)} credited to ${target?.name ?? 'its account'}`,
      );
    }
  }, [isHydrated, processMaturities]);

  // Freeze the net worth of any financial month that has closed since the last visit. Silent
  // by design — it records history rather than changing anything the user did. It runs after
  // recurring processing so a month's own generated transactions are counted in its snapshot.
  useEffect(() => {
    if (!isHydrated) return;
    captureNetWorthSnapshots();
  }, [isHydrated, captureNetWorthSnapshots]);

  // Trigger an auto cloud backup (24h cadence) once both stores are ready.
  useEffect(() => {
    if (!isHydrated || !isAuthLoaded) return;
    autoBackupIfNeeded().catch(() => {
      /* silent: handled by toast inside service */
    });
  }, [isHydrated, isAuthLoaded]);

  // Trigger a local auto-backup download (once per day) for non-logged-in users.
  useEffect(() => {
    if (!isHydrated || !isAuthLoaded) return;
    autoLocalBackupIfNeeded();
  }, [isHydrated, isAuthLoaded]);

  // Rebuild the reminder schedule and fire anything already due. Runs after `processRecurring`
  // above, which matters: a bill it has just turned into a real transaction is history, not an
  // upcoming one to warn about. No-ops unless the user has granted notification permission.
  useEffect(() => {
    if (!isHydrated) return;
    refreshNotificationSchedule();
  }, [isHydrated]);

  // Coming back to a backgrounded tab is the other moment a reminder can land. Only the cheap
  // "show what's due" pass runs here — one IndexedDB read when there is nothing to do.
  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState === 'visible') runDueNotifications();
    };
    document.addEventListener('visibilitychange', onVisible);
    return () => document.removeEventListener('visibilitychange', onVisible);
  }, []);

  return (
    <>
      {/* Desktop sidebar (lg+) sits beside the content column; #root itself stays a plain
          flex-col so standalone (non-Layout) routes don't inherit a row direction meant only
          for this layout. */}
      <div className="flex min-h-0 flex-1 flex-col lg:flex-row">
        <Sidebar />

        {/* Content column */}
        <div className="flex min-h-0 min-w-0 flex-1 flex-col">
          <Outlet />
        </div>
      </div>

      {/* FAB — mobile only. Long-press for one-tap add from a saved template. */}
      <Popover open={templatesOpen && !hideFab} onOpenChange={setTemplatesOpen}>
        <button
          ref={fabRef}
          onClick={handleFabClick}
          {...fabLongPressHandlers}
          onContextMenu={(e) => {
            e.preventDefault();
            setTemplatesOpen(true);
          }}
          className={cn(
            'bg-primary text-primary-foreground fixed right-4 z-50 flex h-14 w-14 items-center justify-center rounded-full shadow-[var(--shadow-float)] transition-transform active:scale-95 lg:hidden',
            hideFab && 'hidden',
          )}
          style={{ bottom: 'calc(env(safe-area-inset-bottom, 0px) + 5.5rem)' }}
          aria-label="Add transaction. Long-press for templates."
        >
          <Plus size={26} strokeWidth={2.4} />
        </button>
        <PopoverContent anchor={fabRef} side="top" align="end" className="w-64">
          <p className="text-muted-foreground px-1 pb-1 text-xs font-medium tracking-wide uppercase">
            Templates
          </p>
          {templates.length === 0 ? (
            <p className="text-muted-foreground px-1 py-2 text-xs">
              No saved templates yet. Long-press a transaction and choose "Save as template".
            </p>
          ) : (
            <div className="flex max-h-72 flex-col gap-0.5 overflow-y-auto">
              {templates.map((t) => (
                <button
                  key={t.id}
                  onClick={() => handleUseTemplate(t.id)}
                  className="hover:bg-accent flex items-center gap-2 rounded-md px-2 py-1.5 text-left text-sm"
                >
                  <Repeat size={13} className="text-muted-foreground shrink-0" />
                  <span className="min-w-0 flex-1 truncate">{t.name}</span>
                  <span className="text-muted-foreground shrink-0 text-xs">
                    {formatCurrency(t.amount, true, hideAmounts)}
                  </span>
                </button>
              ))}
            </div>
          )}
        </PopoverContent>
      </Popover>

      {/* Bottom Nav — mobile only */}
      <nav
        className="pb-safe border-border bg-card fixed right-0 left-0 z-40 flex w-full items-center border-t px-2 pt-2 lg:hidden"
        style={{ bottom: 0 }}
      >
        {navTabs.map((tab) => {
          const isActive = location.pathname === tab.path;
          const Icon = tab.icon;
          return (
            <button
              key={tab.path}
              onClick={() => navigate(tab.path)}
              className={cn(
                'relative flex flex-1 flex-col items-center gap-1 rounded-sm px-1 py-1.5 transition-colors',
                isActive ? 'text-primary' : 'text-muted-foreground',
              )}
            >
              <Icon size={20} strokeWidth={isActive ? 2.4 : 2} />
              <span
                className={cn('bg-primary h-1 w-1 rounded-full', !isActive && 'opacity-0')}
                aria-hidden="true"
              />
              <span className="text-[10px] font-medium">{tab.label}</span>
            </button>
          );
        })}
        <Popover open={moreOpen} onOpenChange={setMoreOpen}>
          <button
            ref={moreRef}
            onClick={() => setMoreOpen((o) => !o)}
            aria-label="More"
            className={cn(
              'relative flex flex-1 flex-col items-center gap-1 rounded-sm px-1 py-1.5 transition-colors',
              moreActive ? 'text-primary' : 'text-muted-foreground',
            )}
          >
            <Ellipsis size={20} strokeWidth={moreActive ? 2.4 : 2} />
            <span
              className={cn('bg-primary h-1 w-1 rounded-full', !moreActive && 'opacity-0')}
              aria-hidden="true"
            />
            <span className="text-[10px] font-medium">More</span>
          </button>
          <PopoverContent anchor={moreRef} side="top" align="end" className="w-56">
            <div className="flex flex-col gap-0.5">
              {moreNavItems.map((item) => {
                const Icon = item.icon;
                const isActive = location.pathname.startsWith(item.path);
                return (
                  <button
                    key={item.path}
                    onClick={() => {
                      setMoreOpen(false);
                      navigate(item.path);
                    }}
                    className={cn(
                      'hover:bg-accent flex items-center gap-2.5 rounded-md px-2 py-2 text-left text-sm',
                      isActive && 'text-primary font-medium',
                    )}
                  >
                    <Icon size={16} className="shrink-0" />
                    {item.label}
                  </button>
                );
              })}
            </div>
          </PopoverContent>
        </Popover>
      </nav>
    </>
  );
}
