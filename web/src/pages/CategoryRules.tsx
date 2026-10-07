import { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { ArrowLeft, ChevronDown, ChevronUp, Pencil, Plus, Trash2, Wand2 } from 'lucide-react';
import { toast } from 'sonner';
import { CategoryIcon } from '@/components/categories/CategoryIcon';
import { CategoryGrid } from '@/components/categories/CategoryGrid';
import { isCategoryValidForType, miscLast } from '@/utils/calculations';
import { MAX_PATTERN_LENGTH } from '@/utils/validation';
import { useFinanceStore } from '@/store/useFinanceStore';
import { MISC_CATEGORY_ID } from '@/data/defaultData';
import {
  MATCH_TYPES,
  MATCH_TYPE_LABELS,
  isValidPattern,
  planRuleApplication,
} from '@/utils/autoCategorize';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { useConfirm } from '@/components/ui/use-confirm';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import type { CategoryRule, RuleMatchType, RuleScope } from '@/types';

const SCOPES: { value: RuleScope; label: string }[] = [
  { value: 'any', label: 'Expenses & income' },
  { value: 'expense', label: 'Expenses only' },
  { value: 'income', label: 'Income only' },
];

export default function CategoryRules() {
  const navigate = useNavigate();
  const location = useLocation();
  const confirm = useConfirm();
  const rules = useFinanceStore((s) => s.rules);
  const categories = useFinanceStore((s) => s.categories);
  const labels = useFinanceStore((s) => s.labels);
  const transactions = useFinanceStore((s) => s.transactions);
  const addRule = useFinanceStore((s) => s.addRule);
  const updateRule = useFinanceStore((s) => s.updateRule);
  const deleteRule = useFinanceStore((s) => s.deleteRule);
  const moveRule = useFinanceStore((s) => s.moveRule);
  const applyRulesToExisting = useFinanceStore((s) => s.applyRulesToExisting);
  const restoreCategorization = useFinanceStore((s) => s.restoreCategorization);

  // Arriving from a merchant's "Create a rule" button — prefill the pattern instead of making
  // the user retype what they just saw. Read once as each field's initial value (like the OTP
  // pages read a passed-in email) rather than via an effect, since this only matters on the
  // render right after that navigation.
  const rulePrefill = location.state as { pattern?: string; scope?: RuleScope } | null;

  const [open, setOpen] = useState(!!rulePrefill?.pattern);
  const [editId, setEditId] = useState<string | null>(null);
  const [pattern, setPattern] = useState(rulePrefill?.pattern ?? '');
  const [matchType, setMatchType] = useState<RuleMatchType>('contains');
  const [scope, setScope] = useState<RuleScope>(rulePrefill?.scope ?? 'any');
  const [categoryId, setCategoryId] = useState('');
  const [labelIds, setLabelIds] = useState<string[]>([]);
  const [replayOpen, setReplayOpen] = useState(false);
  const [onlyUncategorized, setOnlyUncategorized] = useState(true);

  // A rule can file into an expense or income category, so the picker offers both.
  const selectableCategories = useMemo(
    () =>
      miscLast(
        categories.filter((c) =>
          scope === 'any'
            ? isCategoryValidForType(c, 'expense') || isCategoryValidForType(c, 'income')
            : isCategoryValidForType(c, scope),
        ),
      ),
    [categories, scope],
  );

  const categoryOf = (id: string) => categories.find((c) => c.id === id);

  /**
   * How many transactions the current rule set would move if replayed — recomputed live so the
   * replay dialog never promises a number it won't deliver.
   */
  const replayPlan = useMemo(
    () =>
      planRuleApplication(transactions, rules, {
        restrictToCategoryId: onlyUncategorized ? MISC_CATEGORY_ID : undefined,
      }),
    [transactions, rules, onlyUncategorized],
  );

  const patternValid = isValidPattern(pattern, matchType);

  const resetForm = () => {
    setOpen(false);
    setEditId(null);
    setPattern('');
    setMatchType('contains');
    setScope('any');
    setCategoryId('');
    setLabelIds([]);
  };

  const handleEdit = (rule: CategoryRule) => {
    setEditId(rule.id);
    setPattern(rule.pattern);
    setMatchType(rule.matchType);
    setScope(rule.scope);
    setCategoryId(rule.categoryId);
    setLabelIds(rule.labelIds);
    setOpen(true);
  };

  const handleSubmit = () => {
    if (!patternValid) {
      toast.error(matchType === 'regex' ? 'That regex is not valid' : 'Enter something to match');
      return;
    }
    if (!categoryId) {
      toast.error('Pick a category to file matches into');
      return;
    }

    if (editId) {
      updateRule(editId, { pattern: pattern.trim(), matchType, scope, categoryId, labelIds });
    } else {
      addRule({ pattern: pattern.trim(), matchType, scope, categoryId, labelIds, enabled: true });
    }
    resetForm();
  };

  const handleDelete = async (rule: CategoryRule) => {
    const confirmed = await confirm({
      title: `Delete this rule?`,
      description: `New transactions whose note ${MATCH_TYPE_LABELS[rule.matchType]} "${rule.pattern}" will no longer be filed automatically. Transactions it has already categorized keep their category.`,
      confirmLabel: 'Delete rule',
    });
    if (confirmed) deleteRule(rule.id);
  };

  const handleReplay = () => {
    const { changed, previous } = applyRulesToExisting({
      restrictToCategoryId: onlyUncategorized ? MISC_CATEGORY_ID : undefined,
    });
    setReplayOpen(false);
    if (changed === 0) {
      toast('Nothing to recategorize');
      return;
    }
    toast.success(`Recategorized ${changed} transaction${changed === 1 ? '' : 's'}`, {
      action: { label: 'Undo', onClick: () => restoreCategorization(previous) },
    });
  };

  const toggleLabel = (id: string) => {
    setLabelIds((prev) => (prev.includes(id) ? prev.filter((l) => l !== id) : [...prev, id]));
  };

  return (
    <>
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Categorization rules</h1>
        <HeaderIconButton
          onClick={() => {
            resetForm();
            setOpen(true);
          }}
          aria-label="Add rule"
          tone="primary"
        >
          <Plus />
        </HeaderIconButton>
      </Header>

      <Main className="lg:max-w-xl">
        {rules.length === 0 ? (
          <div className="card-elevated space-y-2 rounded-md p-6 text-center">
            <Wand2 size={28} className="text-muted-foreground mx-auto" />
            <p className="text-sm font-medium">No rules yet</p>
            <p className="text-muted-foreground text-xs">
              A rule files a transaction automatically from its note — "contains Uber" → Transport.
              Rules run when you add a transaction and when you import a bank CSV.
            </p>
          </div>
        ) : (
          <>
            <p className="text-muted-foreground px-1 text-xs">
              Checked top to bottom — the first matching rule wins. Rules never touch transfers or
              split transactions.
            </p>

            <div className="card-elevated divide-border divide-y overflow-hidden rounded-md">
              {rules.map((rule, index) => {
                const category = categoryOf(rule.categoryId);
                const ruleLabels = rule.labelIds
                  .map((id) => labels.find((l) => l.id === id))
                  .filter((l) => l !== undefined);

                return (
                  <div key={rule.id} className="p-3">
                    <div className="flex items-start gap-3">
                      <div className="flex flex-col">
                        <button
                          onClick={() => moveRule(rule.id, 'up')}
                          disabled={index === 0}
                          aria-label={`Move rule "${rule.pattern}" up`}
                          className="text-muted-foreground hover:text-foreground rounded-full p-0.5 disabled:opacity-25"
                        >
                          <ChevronUp size={16} />
                        </button>
                        <button
                          onClick={() => moveRule(rule.id, 'down')}
                          disabled={index === rules.length - 1}
                          aria-label={`Move rule "${rule.pattern}" down`}
                          className="text-muted-foreground hover:text-foreground rounded-full p-0.5 disabled:opacity-25"
                        >
                          <ChevronDown size={16} />
                        </button>
                      </div>

                      <div className={`min-w-0 flex-1 ${rule.enabled ? '' : 'opacity-60'}`}>
                        <p className="truncate text-sm">
                          <span className="text-muted-foreground">Note </span>
                          {MATCH_TYPE_LABELS[rule.matchType]}{' '}
                          <span className="font-semibold">"{rule.pattern}"</span>
                        </p>
                        <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
                          {category && (
                            <span
                              className="text-foreground flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
                              style={{
                                backgroundColor: `color-mix(in srgb, ${category.color} 16%, transparent)`,
                              }}
                            >
                              <CategoryIcon icon={category.icon} size={12} color={category.color} />
                              {category.name}
                            </span>
                          )}
                          {ruleLabels.map((label) => (
                            <span
                              key={label.id}
                              className="bg-muted text-foreground flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium"
                            >
                              <span
                                className="size-1.5 shrink-0 rounded-full"
                                style={{ backgroundColor: label.color }}
                                aria-hidden="true"
                              />
                              {label.name}
                            </span>
                          ))}
                          {rule.scope !== 'any' && (
                            <span className="bg-muted text-muted-foreground rounded-full px-2 py-0.5 text-xs">
                              {rule.scope === 'expense' ? 'Expenses' : 'Income'}
                            </span>
                          )}
                        </div>
                      </div>

                      <div className="flex shrink-0 items-center gap-1">
                        <Switch
                          size="sm"
                          checked={rule.enabled}
                          onCheckedChange={(enabled) => updateRule(rule.id, { enabled })}
                          aria-label={`Rule "${rule.pattern}" enabled`}
                        />
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleEdit(rule)}
                          className="h-8 w-8"
                          aria-label={`Edit rule "${rule.pattern}"`}
                        >
                          <Pencil size={14} className="text-muted-foreground" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleDelete(rule)}
                          className="h-8 w-8"
                          aria-label={`Delete rule "${rule.pattern}"`}
                        >
                          <Trash2 size={14} className="text-destructive" />
                        </Button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <Button onClick={() => setReplayOpen(true)} variant="secondary" className="w-full">
              <Wand2 size={15} /> Apply to existing transactions
            </Button>
          </>
        )}

        {/* Add / edit rule */}
        <Dialog
          open={open}
          onOpenChange={(v) => {
            if (!v) resetForm();
          }}
        >
          {/* Header and footer stay put; only the form body scrolls on a short screen. */}
          <DialogContent className="flex max-h-[calc(100dvh-2rem)] flex-col overflow-hidden sm:max-w-md">
            <DialogHeader className="shrink-0 pr-8">
              <DialogTitle>{editId ? 'Edit rule' : 'New rule'}</DialogTitle>
              <DialogDescription>
                When a transaction's note matches, file it into a category and tag it.
              </DialogDescription>
            </DialogHeader>

            <div className="-mx-4 min-h-0 flex-1 space-y-3 overflow-y-auto px-4 pb-1">
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-2">
                <div>
                  <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                    Note
                  </Label>
                  <Select
                    value={matchType}
                    onValueChange={(v) => setMatchType((v as RuleMatchType) ?? matchType)}
                  >
                    <SelectTrigger className="w-full">
                      <SelectValue>{MATCH_TYPE_LABELS[matchType]}</SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {MATCH_TYPES.map((m) => (
                        <SelectItem key={m.value} value={m.value}>
                          {m.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                    Applies to
                  </Label>
                  <Select value={scope} onValueChange={(v) => setScope((v as RuleScope) ?? scope)}>
                    <SelectTrigger className="w-full">
                      <SelectValue>{SCOPES.find((s) => s.value === scope)?.label}</SelectValue>
                    </SelectTrigger>
                    <SelectContent>
                      {SCOPES.map((s) => (
                        <SelectItem key={s.value} value={s.value}>
                          {s.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div>
                <Label
                  htmlFor="rule-pattern"
                  className="text-muted-foreground mb-1.5 block text-xs font-medium"
                >
                  Pattern
                </Label>
                <Input
                  id="rule-pattern"
                  type="text"
                  placeholder={matchType === 'regex' ? 'e\\.g\\. uber|ola' : 'e.g. Uber'}
                  value={pattern}
                  maxLength={MAX_PATTERN_LENGTH}
                  onChange={(e) => setPattern(e.target.value)}
                />
                {matchType === 'regex' && pattern.trim() !== '' && !patternValid && (
                  <p className="text-destructive mt-1 text-xs">Not a valid regular expression</p>
                )}
                <p className="text-muted-foreground mt-1 text-xs">
                  Matching ignores case.
                  {matchType === 'regex' && ' Regex runs against the whole note.'}
                </p>
              </div>

              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  File into
                </Label>
                <CategoryGrid className="max-h-40">
                  {selectableCategories.map((cat) => {
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
                        {scope === 'any' && cat.type !== 'both' && (
                          <span className="text-muted-foreground text-[10px] leading-none capitalize">
                            {cat.type}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </CategoryGrid>
              </div>

              {labels.length > 0 && (
                <div>
                  <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                    Also tag with (optional)
                  </Label>
                  <div className="flex flex-wrap gap-2">
                    {labels.map((label) => {
                      const active = labelIds.includes(label.id);
                      return (
                        <button
                          key={label.id}
                          type="button"
                          onClick={() => toggleLabel(label.id)}
                          aria-pressed={active}
                          className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium transition-all ${
                            active
                              ? 'text-foreground'
                              : 'bg-muted text-muted-foreground border-transparent'
                          }`}
                          style={
                            active
                              ? {
                                  backgroundColor: `color-mix(in srgb, ${label.color} 18%, transparent)`,
                                  borderColor: label.color,
                                }
                              : undefined
                          }
                        >
                          <span
                            className="size-2 shrink-0 rounded-full"
                            style={{ backgroundColor: label.color }}
                            aria-hidden="true"
                          />
                          {label.name}
                        </button>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            <DialogFooter className="shrink-0">
              <Button variant="outline" onClick={resetForm}>
                Cancel
              </Button>
              <Button onClick={handleSubmit}>{editId ? 'Update rule' : 'Add rule'}</Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Replay over existing history */}
        <Dialog open={replayOpen} onOpenChange={setReplayOpen}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader className="pr-8">
              <DialogTitle>Apply rules to existing transactions</DialogTitle>
              <DialogDescription>
                Runs every enabled rule over transactions already in your ledger. Transfers and
                split transactions are left alone, and you can undo the whole pass.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-3">
              <div
                className="bg-muted grid grid-cols-2 gap-1 rounded-full p-1"
                role="group"
                aria-label="Which transactions"
              >
                {[
                  { value: true, label: 'Uncategorized only' },
                  { value: false, label: 'All transactions' },
                ].map((option) => (
                  <button
                    key={String(option.value)}
                    type="button"
                    onClick={() => setOnlyUncategorized(option.value)}
                    aria-pressed={onlyUncategorized === option.value}
                    className={`rounded-full py-1.5 text-xs font-medium transition-all ${
                      onlyUncategorized === option.value
                        ? 'bg-grad-primary shadow-glow-primary text-white'
                        : 'text-muted-foreground'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>
              <p className="text-muted-foreground text-xs">
                {onlyUncategorized
                  ? 'Only touches transactions currently filed under Miscellaneous — the usual state after a bank import.'
                  : 'Re-files every matching transaction, including ones you categorized by hand.'}
              </p>

              <p className="text-sm">
                <span className="text-lg font-bold">{replayPlan.length}</span> transaction
                {replayPlan.length === 1 ? '' : 's'} would change.
              </p>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setReplayOpen(false)}>
                Cancel
              </Button>
              <Button onClick={handleReplay} disabled={replayPlan.length === 0}>
                Apply
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </Main>
    </>
  );
}
