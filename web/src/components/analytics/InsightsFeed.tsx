import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import {
  Lightbulb,
  TrendingUp,
  ThumbsUp,
  Repeat,
  AlertTriangle,
  CircleAlert,
  ChevronRight,
} from 'lucide-react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { formatCurrency, formatShortDate } from '@/utils/formatters';
import { buildInsights, type Insight, type SubscriptionCandidate } from '@/utils/insights';
import { normalizeMonthStartDay, periodLabel, periodRange } from '@/utils/period';
import { Button } from '@/components/ui/button';

const KIND_ICON = {
  'category-spike': TrendingUp,
  'category-drop': ThumbsUp,
  subscription: Repeat,
  'budget-over': AlertTriangle,
  'budget-pace': AlertTriangle,
  'savings-rate': Lightbulb,
  'category-share': Lightbulb,
  'negative-balance': CircleAlert,
} as const;

/** Icon tone per severity — green only for genuinely good news, lavender for neutral info. */
const SEVERITY_ICON = {
  warn: 'text-warning',
  info: 'text-primary',
  good: 'text-positive',
} as const;

export function InsightsFeed() {
  const navigate = useNavigate();
  const transactions = useFinanceStore((s) => s.transactions);
  const categories = useFinanceStore((s) => s.categories);
  const labels = useFinanceStore((s) => s.labels);
  const budgets = useFinanceStore((s) => s.budgets);
  const recurring = useFinanceStore((s) => s.recurring);
  const accounts = useFinanceStore((s) => s.accounts);
  const hideAmounts = useFinanceStore((s) => s.settings.hideAmounts);
  const monthStartDay = normalizeMonthStartDay(useFinanceStore((s) => s.settings.monthStartDay));
  const addRecurring = useFinanceStore((s) => s.addRecurring);
  const deleteRecurring = useFinanceStore((s) => s.deleteRecurring);

  // Insights the user has acted on or waved away stay gone for this visit only — they're
  // derived from the ledger, so persisting a dismissal would mean persisting a fact about
  // data that may no longer exist.
  const [dismissed, setDismissed] = useState<string[]>([]);

  const insights = useMemo(
    () =>
      buildInsights(
        { transactions, categories, labels, budgets, recurring, accounts, monthStartDay },
        { formatAmount: (value) => formatCurrency(value, true, hideAmounts, { precise: false }) },
      ),
    [transactions, categories, labels, budgets, recurring, accounts, monthStartDay, hideAmounts],
  );

  const visible = insights.filter((i) => !dismissed.includes(i.id));

  const handleCreateRule = (insight: Insight, candidate: SubscriptionCandidate) => {
    const ruleId = addRecurring({
      type: 'expense',
      amount: candidate.amount,
      accountId: candidate.accountId,
      categoryId: candidate.categoryId,
      note: candidate.note,
      labels: candidate.labels,
      frequency: candidate.frequency,
      // Starts at the next expected charge, so nothing already in the ledger is regenerated.
      startDate: candidate.nextDate,
    });
    setDismissed((prev) => [...prev, insight.id]);
    toast.success(`Recurring rule created — next on ${formatShortDate(candidate.nextDate)}`, {
      action: {
        label: 'Undo',
        onClick: () => {
          deleteRecurring(ruleId);
          setDismissed((prev) => prev.filter((id) => id !== insight.id));
        },
      },
    });
  };

  if (visible.length === 0) return null;

  return (
    <section className="card-elevated rounded-md p-4">
      <div className="mb-3 flex items-center gap-2">
        <Lightbulb size={15} className="text-muted-foreground shrink-0" aria-hidden />
        <h3 className="text-sm font-semibold">Insights</h3>
        <span className="text-muted-foreground ml-auto text-xs font-medium">
          {periodLabel(periodRange('monthly', new Date(), monthStartDay), monthStartDay)}
        </span>
      </div>

      <ul className="divide-border divide-y">
        {visible.map((insight) => {
          const Icon = KIND_ICON[insight.kind];
          const iconTone = SEVERITY_ICON[insight.severity];
          const action = insight.action;
          return (
            <li key={insight.id} className="flex gap-3 py-3 first:pt-0 last:pb-0">
              <Icon size={16} className={`mt-0.5 shrink-0 ${iconTone}`} aria-hidden />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-medium">{insight.title}</p>
                <p className="text-muted-foreground mt-0.5 text-xs">{insight.detail}</p>

                {action?.type === 'create-recurring' && (
                  <div className="mt-2 flex flex-wrap gap-2">
                    <Button size="sm" onClick={() => handleCreateRule(insight, action.candidate)}>
                      Create recurring rule
                    </Button>
                    <Button
                      size="sm"
                      variant="ghost"
                      onClick={() => setDismissed((prev) => [...prev, insight.id])}
                    >
                      Not a subscription
                    </Button>
                  </div>
                )}

                {action?.type === 'navigate' && (
                  <button
                    onClick={() => navigate(action.to)}
                    className="text-primary mt-1.5 inline-flex items-center gap-0.5 text-xs font-medium hover:underline"
                  >
                    {action.label}
                    <ChevronRight size={12} />
                  </button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
