/** Shared input limits and small pure validators for forms. */

/** Names of users, accounts, goals, categories, labels, people, templates, loans. */
export const MAX_NAME_LENGTH = 40;
/** Free-text notes on transactions, recurring rules, goal/debt/loan entries. */
export const MAX_NOTE_LENGTH = 200;
/** A categorization rule's match pattern. */
export const MAX_PATTERN_LENGTH = 100;

/** Trim and cap a free-text value. Caps by code point so an emoji is never split. */
export function cleanText(value: string, max: number): string {
  const trimmed = value.trim();
  const chars = Array.from(trimmed);
  return chars.length > max ? chars.slice(0, max).join('') : trimmed;
}

/** For `onChange`: drop leading whitespace while typing (trailing is kept so words can be typed). */
export function stripLeading(value: string): string {
  return value.replace(/^\s+/, '');
}

/** A pragmatic client-side email check — the server remains the authority. */
export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value.trim());
}

/** Whether a `yyyy-MM-dd` day is before `today` (also `yyyy-MM-dd`). Plain string compare is safe. */
export function isPastDay(dayKey: string, today: string): boolean {
  return dayKey < today;
}

/** Whether both ends of a `yyyy-MM-dd` range are set and From is after To. */
export function isRangeInverted(from: string, to: string): boolean {
  return !!from && !!to && from > to;
}

/**
 * The first budget scope value not already taken, in the order given by `candidates`
 * (Overall, then categories, then labels). `null` when every scope already has a budget.
 */
export function firstFreeScope(candidates: string[], used: Set<string>): string | null {
  return candidates.find((c) => !used.has(c)) ?? null;
}
