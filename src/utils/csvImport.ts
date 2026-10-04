import Papa from 'papaparse';
import { findMatchingRule, mergeLabels } from './autoCategorize';
import type { Category, CategoryRule, Transaction } from '@/types';

/**
 * Bank CSV exports are free-form: unknown headers, unknown date format, and either a single
 * signed amount column or separate debit/credit columns. Everything here is a pure function of
 * (headers, rows, mapping) so the wizard UI can re-run it on every mapping tweak without
 * touching the store, and so it's testable without a browser.
 */

export interface CsvParseResult {
  headers: string[];
  rows: string[][];
}

/** Thin wrapper around Papa.parse — handles quoted fields, embedded commas/newlines, and CRLF. */
export function parseCsvText(text: string, skipRows = 0): CsvParseResult {
  const result = Papa.parse<string[]>(text.trim(), { skipEmptyLines: true });
  const dataLines = result.data.slice(skipRows);
  const [headerRow, ...dataRows] = dataLines;
  return { headers: (headerRow ?? []).map((h) => h.trim()), rows: dataRows };
}

/** Columns the mapping step pre-selects from the header names alone. */
export interface GuessedColumns {
  dateCol?: number;
  amountMode: 'signed' | 'debitCredit';
  amountCol?: number;
  debitCol?: number;
  creditCol?: number;
  noteCol?: number;
  categoryCol?: number;
}

const DEBIT_HEADER = /\bdebit|withdraw|\bdr\b|money out|paid out/i;
const CREDIT_HEADER = /\bcredit|deposit|\bcr\b|money in|paid in/i;
const AMOUNT_HEADER = /amount|\bamt\b/i;
const NOTE_HEADER = /note|desc|narration|particular|memo|details|remark|payee/i;
const CATEGORY_HEADER = /categ/i;
const DATE_HEADER = /date/i;

/**
 * Best-effort auto-mapping from header names — the user can always override it. Separate
 * Debit/Credit (or Withdrawal/Deposit) headers win over a single amount column, because a
 * header like "Withdrawal Amt." is a debit column that also happens to say "amount".
 */
export function guessColumnMapping(headers: string[]): GuessedColumns {
  const find = (re: RegExp, exclude: Array<number | undefined> = []) => {
    const i = headers.findIndex((h, idx) => re.test(h) && !exclude.includes(idx));
    return i >= 0 ? i : undefined;
  };
  const dateCol = find(DATE_HEADER);
  const debitCol = find(DEBIT_HEADER, [dateCol]);
  const creditCol = find(CREDIT_HEADER, [dateCol, debitCol]);
  const taken = [dateCol, debitCol, creditCol];
  const noteCol = find(NOTE_HEADER, taken);
  const categoryCol = find(CATEGORY_HEADER, [...taken, noteCol]);

  if (debitCol !== undefined && creditCol !== undefined) {
    return { dateCol, amountMode: 'debitCredit', debitCol, creditCol, noteCol, categoryCol };
  }
  const amountCol = find(AMOUNT_HEADER, [dateCol, noteCol, categoryCol]);
  return { dateCol, amountMode: 'signed', amountCol, noteCol, categoryCol };
}

export type DateFormatCode =
  | 'YYYY-MM-DD'
  | 'DD/MM/YYYY'
  | 'MM/DD/YYYY'
  | 'DD-MM-YYYY'
  | 'MM-DD-YYYY'
  | 'DD.MM.YYYY';

export const DATE_FORMATS: { value: DateFormatCode; label: string }[] = [
  { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (2026-07-27)' },
  { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (27/07/2026)' },
  { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (07/27/2026)' },
  { value: 'DD-MM-YYYY', label: 'DD-MM-YYYY (27-07-2026)' },
  { value: 'MM-DD-YYYY', label: 'MM-DD-YYYY (07-27-2026)' },
  { value: 'DD.MM.YYYY', label: 'DD.MM.YYYY (27.07.2026)' },
];

function isoFromParts(year: number, month: number, day: number): string | null {
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  const d = new Date(Date.UTC(year, month - 1, day));
  // Date normalizes out-of-range days (e.g. 31 Feb) instead of failing — catch that here.
  if (d.getUTCFullYear() !== year || d.getUTCMonth() !== month - 1 || d.getUTCDate() !== day) {
    return null;
  }
  return d.toISOString();
}

export function parseDateWithFormat(raw: string, format: DateFormatCode): string | null {
  const trimmed = raw.trim();
  if (!trimmed) return null;

  let m: RegExpMatchArray | null;
  switch (format) {
    case 'YYYY-MM-DD':
      m = trimmed.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
      return m ? isoFromParts(+m[1], +m[2], +m[3]) : null;
    case 'DD/MM/YYYY':
      m = trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
      return m ? isoFromParts(+m[3], +m[2], +m[1]) : null;
    case 'MM/DD/YYYY':
      m = trimmed.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
      return m ? isoFromParts(+m[3], +m[1], +m[2]) : null;
    case 'DD-MM-YYYY':
      m = trimmed.match(/^(\d{1,2})-(\d{1,2})-(\d{4})/);
      return m ? isoFromParts(+m[3], +m[2], +m[1]) : null;
    case 'MM-DD-YYYY':
      m = trimmed.match(/^(\d{1,2})-(\d{1,2})-(\d{4})/);
      return m ? isoFromParts(+m[3], +m[1], +m[2]) : null;
    case 'DD.MM.YYYY':
      m = trimmed.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})/);
      return m ? isoFromParts(+m[3], +m[2], +m[1]) : null;
    default:
      return null;
  }
}

export interface DateFormatDetection {
  format: DateFormatCode | undefined;
  /** True when every sample has day and month both <= 12, so DD/MM vs MM/DD was a guess. */
  ambiguous: boolean;
}

/** Share of non-empty samples a format must parse to be accepted (tolerates a few junk rows). */
const DATE_MATCH_THRESHOLD = 0.9;

const SWAPPED_TWIN: Partial<Record<DateFormatCode, DateFormatCode>> = {
  'DD/MM/YYYY': 'MM/DD/YYYY',
  'MM/DD/YYYY': 'DD/MM/YYYY',
  'DD-MM-YYYY': 'MM-DD-YYYY',
  'MM-DD-YYYY': 'DD-MM-YYYY',
};

function matchRatio(samples: string[], format: DateFormatCode): number {
  const hits = samples.filter((s) => parseDateWithFormat(s, format) !== null).length;
  return hits / samples.length;
}

/**
 * Tries formats most-unambiguous-first and picks the first that parses at least 90% of the
 * samples. Pass every row, not just the first few — one row with day > 12 is what settles
 * DD/MM vs MM/DD. `ambiguous` is set when the swapped twin fits equally well (every row has
 * both parts <= 12), in which case the day-first default was a guess.
 */
export function detectDateFormatInfo(samples: string[]): DateFormatDetection {
  const nonEmpty = samples.map((s) => s.trim()).filter(Boolean);
  if (nonEmpty.length === 0) return { format: undefined, ambiguous: false };
  const found = DATE_FORMATS.find(
    ({ value }) => matchRatio(nonEmpty, value) >= DATE_MATCH_THRESHOLD,
  );
  if (!found) return { format: undefined, ambiguous: false };
  const twin = SWAPPED_TWIN[found.value];
  const ambiguous =
    !!twin &&
    nonEmpty.every((s) => {
      const m = s.match(/^(\d{1,2})[/-](\d{1,2})/);
      return !!m && +m[1] <= 12 && +m[2] <= 12;
    });
  return { format: found.value, ambiguous };
}

export function detectDateFormat(samples: string[]): DateFormatCode | undefined {
  return detectDateFormatInfo(samples).format;
}

const CURRENCY_TOKEN = '(?:[A-Za-z]+\\.?|[$\u20B9\u20AC\u00A3\u00A5]|\\s)+';
const LEADING_CURRENCY = new RegExp(`^${CURRENCY_TOKEN}`);
const TRAILING_CURRENCY = new RegExp(`${CURRENCY_TOKEN}$`);
const PLAIN_NUMBER = /^(?:\d+\.?\d*|\.\d+)(?:e[-+]?\d+)?$/i;

/**
 * Parses a bank-export amount. Only currency symbols/words at the very start or end are
 * dropped ("Rs. 2,000", "450 USD", "$1,000.00"), along with thousand separators. Anything else
 * left over ("12abc34") is junk and returns null rather than being silently mangled. Scientific
 * notation ("1e3") is accepted, since spreadsheet exports produce it. Accounting-style
 * parentheses ("(500.00)") are negative.
 */
export function parseAmount(raw: string): number | null {
  if (raw == null) return null;
  let s = raw.trim();
  if (!s) return null;

  let negative = false;
  if (/^\(.*\)$/.test(s)) {
    negative = true;
    s = s.slice(1, -1).trim();
  }

  let sign = 1;
  const signMatch = s.match(/^([-+])\s*/);
  if (signMatch) {
    if (signMatch[1] === '-') sign = -1;
    s = s.slice(signMatch[0].length);
  }

  s = s.replace(LEADING_CURRENCY, '');
  // A sign may also follow the currency symbol ("$-5").
  const innerSign = s.match(/^([-+])\s*/);
  if (innerSign) {
    if (innerSign[1] === '-') sign = -sign;
    s = s.slice(innerSign[0].length);
  }
  s = s.replace(TRAILING_CURRENCY, '').replace(/,/g, '').replace(/\s+/g, '');
  if (!PLAIN_NUMBER.test(s)) return null;

  const n = Number(s) * sign;
  if (!Number.isFinite(n)) return null;
  return negative ? -Math.abs(n) : n;
}

export type AmountMode = 'signed' | 'debitCredit';

export interface ColumnMapping {
  dateCol: number;
  noteCol?: number;
  categoryCol?: number;
  amountMode: AmountMode;
  /** Used when `amountMode === 'signed'`. */
  amountCol?: number;
  /** Whether a negative signed amount is an expense (true, the common convention) or income. */
  negativeIsExpense?: boolean;
  /** Used when `amountMode === 'debitCredit'`. */
  debitCol?: number;
  creditCol?: number;
}

export interface CsvImportOptions {
  mapping: ColumnMapping;
  dateFormat: DateFormatCode;
  accountId: string;
  categories: Category[];
  /** Category to fall back to when no category column is mapped, or its value matches nothing. */
  fallbackCategoryId: string;
  /**
   * Auto-categorization rules, in priority order. They fill the gap the statement leaves: a
   * rule only fires when the file's own category column didn't already say where the row goes,
   * so explicit data from the bank always outranks a guess from a note pattern.
   */
  rules?: CategoryRule[];
}

export interface ParsedCsvTransaction {
  /** Index into the original data rows (0-based) — lets the UI point back at the source row. */
  rowIndex: number;
  transaction: Omit<Transaction, 'id' | 'createdAt'>;
  /** False when a category column was mapped but its value didn't match any existing category. */
  categoryMatched: boolean;
  /** Set when an auto-categorization rule picked this row's category. */
  matchedRuleId?: string;
}

export interface CsvImportResult {
  accepted: ParsedCsvTransaction[];
  totalRows: number;
  /** Per-row rejection reasons, capped for display. */
  issues: string[];
}

const MAX_ISSUES = 8;

export function buildTransactionsFromCsv(
  rows: string[][],
  options: CsvImportOptions,
): CsvImportResult {
  const { mapping, dateFormat, accountId, categories, fallbackCategoryId, rules } = options;
  const accepted: ParsedCsvTransaction[] = [];
  const allIssues: string[] = [];

  rows.forEach((row, index) => {
    const rowLabel = `Row ${index + 1}`;
    const rawDate = row[mapping.dateCol] ?? '';
    const date = parseDateWithFormat(rawDate, dateFormat);
    if (!date) {
      allIssues.push(`${rowLabel}: unparseable date "${rawDate}"`);
      return;
    }

    let amount: number;
    let type: 'expense' | 'income';

    if (mapping.amountMode === 'signed') {
      const raw = mapping.amountCol !== undefined ? (row[mapping.amountCol] ?? '') : '';
      const parsed = parseAmount(raw);
      if (parsed === null || parsed === 0) {
        allIssues.push(`${rowLabel}: unparseable amount "${raw}"`);
        return;
      }
      const negativeIsExpense = mapping.negativeIsExpense ?? true;
      const isExpense = negativeIsExpense ? parsed < 0 : parsed > 0;
      type = isExpense ? 'expense' : 'income';
      amount = Math.abs(parsed);
    } else {
      const rawDebit = mapping.debitCol !== undefined ? (row[mapping.debitCol] ?? '') : '';
      const rawCredit = mapping.creditCol !== undefined ? (row[mapping.creditCol] ?? '') : '';
      const debit = Math.abs(parseAmount(rawDebit) ?? 0);
      const credit = Math.abs(parseAmount(rawCredit) ?? 0);
      if (debit > 0 && credit > 0) {
        allIssues.push(`${rowLabel}: both debit and credit are filled`);
        return;
      }
      if (debit <= 0 && credit <= 0) {
        allIssues.push(`${rowLabel}: no debit or credit amount`);
        return;
      }
      type = debit > 0 ? 'expense' : 'income';
      amount = debit > 0 ? debit : credit;
    }

    const note = mapping.noteCol !== undefined ? (row[mapping.noteCol] ?? '').trim() : '';

    let categoryId = fallbackCategoryId;
    let categoryMatched = false;
    if (mapping.categoryCol !== undefined) {
      const rawCategory = (row[mapping.categoryCol] ?? '').trim();
      if (rawCategory) {
        const match = categories.find(
          (c) =>
            (c.type === type || c.type === 'both') &&
            c.name.toLowerCase() === rawCategory.toLowerCase(),
        );
        if (match) {
          categoryId = match.id;
          categoryMatched = true;
        }
      }
    }

    // The statement had nothing to say about this row's category — let the rules try.
    let labels: string[] = [];
    let matchedRuleId: string | undefined;
    if (!categoryMatched && rules?.length) {
      const rule = findMatchingRule(rules, note, type);
      if (rule) {
        categoryId = rule.categoryId;
        labels = mergeLabels(labels, rule.labelIds);
        matchedRuleId = rule.id;
      }
    }

    accepted.push({
      rowIndex: index,
      categoryMatched,
      ...(matchedRuleId ? { matchedRuleId } : {}),
      transaction: {
        type,
        amount,
        accountId,
        categoryId,
        date,
        note,
        labels,
      },
    });
  });

  const issues = allIssues.slice(0, MAX_ISSUES);
  if (allIssues.length > issues.length) {
    issues.push(`…and ${allIssues.length - issues.length} more`);
  }

  return { accepted, totalRows: rows.length, issues };
}

function dedupeKey(date: string, amount: number, note: string, type: string): string {
  return `${date.slice(0, 10)}|${type}|${amount.toFixed(2)}|${note.trim().toLowerCase()}`;
}

/**
 * Flags rows that look like they're already in the ledger — same day, type, amount and note —
 * whether that match is against existing transactions or another row earlier in this same file
 * (re-importing the same statement, or a bank listing a row twice).
 */
export function findDuplicateRows(
  candidates: ParsedCsvTransaction[],
  existing: Transaction[],
): Set<number> {
  const existingKeys = new Set(existing.map((t) => dedupeKey(t.date, t.amount, t.note, t.type)));
  const seenInBatch = new Set<string>();
  const duplicates = new Set<number>();

  for (const { rowIndex, transaction } of candidates) {
    const key = dedupeKey(transaction.date, transaction.amount, transaction.note, transaction.type);
    if (existingKeys.has(key) || seenInBatch.has(key)) {
      duplicates.add(rowIndex);
    }
    seenInBatch.add(key);
  }

  return duplicates;
}
