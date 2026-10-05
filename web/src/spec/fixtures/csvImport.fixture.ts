import Papa from 'papaparse';
import {
  DATE_FORMATS,
  buildTransactionsFromCsv,
  detectDateFormat,
  detectDateFormatInfo,
  findDuplicateRows,
  guessColumnMapping,
  parseAmount,
  parseCsvText,
  parseDateWithFormat,
  type CsvImportOptions,
  type DateFormatCode,
  type ParsedCsvTransaction,
} from '@/utils/csvImport';
import type { Category, CategoryRule, Transaction } from '@/types';
import { gc, type GoldenCase } from '../golden';

/** CSV texts for the papaparse port: quoting, newlines, delimiters, BOM, malformed input. */
const CSV_TEXTS = [
  '',
  '   ',
  'Date, Amount ,Note\n2026-01-01,100,Coffee\n2026-01-02,50,Tea',
  'Date,Amount,Note\r\n2026-01-01,100,Coffee\r\n2026-01-02,50,Tea\r\n',
  'Date,Amount,Note\r2026-01-01,100,Coffee\r2026-01-02,50,Tea',
  'Date,Amount,Note\n2026-01-01,100,"Rent, January"',
  'Date,Amount,Note\n2026-01-01,100,"He said ""hi"""\n2026-01-02,5,x',
  'Date,Amount,Note\n2026-01-01,100,"line one\nline two"\n2026-01-02,5,"a\r\nb"',
  'Date,Amount,Note\r\n2026-01-01,100,"multi\r\nline"\r\n2026-01-02,5,x',
  '﻿Date,Amount\n2026-01-01,100',
  '﻿﻿Date,Amount\n2026-01-01,100',
  'Date;Amount;Note\n27.07.2026;-1.234,56;Kaffee\n28.07.2026;2.000;Gehalt',
  'Date\tAmount\tNote\n2026-01-01\t100\tCoffee, black\n2026-01-02\t50\tTea',
  'Date|Amount|Note\n2026-01-01|100|Coffee\n2026-01-02|50|Tea',
  'Date\u001eAmount\n2026-01-01\u001e100',
  'Date\u001fAmount\n2026-01-01\u001f100',
  'Date,Amount;Note\n2026-01-01,100;x\n2026-01-02,50;y',
  'a,b;c;d\n1,2;3;4\n5,6;7;8',
  'only one column\nrow two\nrow three',
  'Date,Amount\n\n2026-01-01,100\n\n\n2026-01-02,50\n',
  'Date,Amount\n,\n2026-01-01,100\n ,\n',
  'Date,Amount\n   \n2026-01-01,100',
  'Statement for account 1234\nGenerated on 2026-07-01\nDate,Amount\n2026-01-01,100',
  'Date,Amount,Note\n2026-01-01,100,"unterminated\n2026-01-02,50,Tea',
  'Date,Amount,Note\n2026-01-01,100,"bad"quote,x\n2026-01-02,50,Tea',
  'Date,Amount,Note\n2026-01-01,100,"spaced"   ,x\n2026-01-02,50,"tail"  \n2026-01-03,1,z',
  'Date,Amount,Note\n2026-01-01,100,mid"quote"here\n2026-01-02,50,Tea',
  '"Date","Amount","Note"\n"2026-01-01","100","Coffee"\n"2026-01-02","50",""',
  '"a",\n"b",',
  '""\n""',
  '"x"',
  '"',
  '","',
  'a,"b\n',
  'a\r\nb\nc\r\nd',
  'a\nb\r\nc\rd',
  '"x\ny"\r\nz\r\nw',
  'Date,Amount\n' +
    Array.from({ length: 15 }, (_, i) => `2026-01-${String(i + 1).padStart(2, '0')},${i}`).join(
      '\n',
    ),
  'h1;h2\n' + Array.from({ length: 12 }, (_, i) => `${i},${i};x`).join('\n'),
  'Txn Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance\n01/07/26,UPI-SWIGGY,0000123,01/07/26,450.00,,10550.00\n02/07/26,SALARY JULY,0000124,02/07/26,,85000.00,95550.00',
  ' Date,Amount \n2026-01-01,100',
  'Date ,  Amount\t\n2026-01-01,100',
  'a,b\n1,2,3,4\n5\n6,7',
];

const categories: Category[] = [
  { id: 'cat-food', name: 'Food', icon: 'utensils', color: '#ef4444', type: 'expense' },
  { id: 'cat-salary', name: 'Salary', icon: 'briefcase', color: '#22c55e', type: 'income' },
  {
    id: 'cat-misc',
    name: 'Miscellaneous',
    icon: 'circle-ellipsis',
    color: '#94a3b8',
    type: 'both',
  },
  { id: 'cat-cab', name: 'Cab', icon: 'car', color: '#000', type: 'expense' },
  { id: 'cat-cafe', name: 'Café', icon: 'coffee', color: '#000', type: 'expense' },
];

const uberRule: CategoryRule = {
  id: 'rule-uber',
  pattern: 'uber',
  matchType: 'contains',
  scope: 'any',
  categoryId: 'cat-food',
  labelIds: ['lbl-essential'],
  enabled: true,
  createdAt: '2026-01-01T00:00:00.000Z',
};
const salaryRule: CategoryRule = {
  ...uberRule,
  id: 'rule-salary',
  pattern: '^salary',
  matchType: 'regex',
  scope: 'income',
  categoryId: 'cat-salary',
  labelIds: ['lbl-income', 'lbl-essential'],
};
const blankIdRule: CategoryRule = { ...uberRule, id: '', pattern: 'tea' };

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('constants', [], { DATE_FORMATS }));

  for (const text of CSV_TEXTS) {
    for (const skipEmptyLines of [true, false]) {
      const r = Papa.parse<string[]>(text, { skipEmptyLines });
      out.push(
        gc('Papa.parse', [text, skipEmptyLines], {
          data: r.data,
          delimiter: r.meta.delimiter,
          linebreak: r.meta.linebreak,
        }),
      );
    }
    for (const skipRows of [0, 1, 2, 5, -1]) {
      out.push(gc('parseCsvText', [text, skipRows], parseCsvText(text, skipRows)));
    }
  }

  const headerSets = [
    ['Date', 'Description', 'Category', 'Amount'],
    ['Txn Date', 'Narration', 'Debit', 'Credit'],
    ['Date', 'Narration', 'Chq./Ref.No.', 'Withdrawal Amt.', 'Deposit Amt.', 'Closing Balance'],
    ['When', 'Debit', 'Something'],
    ['Date', 'Dr', 'Cr', 'Particulars'],
    ['Posting Date', 'Value Date', 'Money Out', 'Money In', 'Memo'],
    ['Transaction Date', 'Paid out', 'Paid in', 'Payee', 'Categories'],
    ['date', 'amt', 'notes', 'category'],
    ['DATE', 'AMOUNT', 'DESCRIPTION'],
    ['Credit Card', 'Debited', 'Amount'],
    ['Dr.', 'Cr.', 'Date'],
    ['Address', 'Credit', 'Debit'],
    ['Categ', 'Note', 'Amount', 'Date'],
    ['Remarks', 'Debit Amount', 'Credit Amount', 'Date'],
    ['Updated', 'Amount (INR)', 'Details'],
    [],
    ['', '', ''],
    ['Débit', 'Crédit', 'Datum'],
    ['Amount', 'Amount', 'Date'],
    ['Deposit', 'Withdrawal', 'Description', 'Category Name'],
  ];
  for (const h of headerSets) out.push(gc('guessColumnMapping', [h], guessColumnMapping(h)));

  const dateSamples = [
    '2026-07-27',
    '2026/7/5',
    '2026-7-27',
    '2026-07-27T10:00:00',
    '2026-13-01',
    '2026-02-29',
    '2028-02-29',
    '2026-02-30',
    '0099-01-01',
    '0100-01-01',
    '0000-01-01',
    '1900-02-29',
    '2000-02-29',
    '9999-12-31',
    '27/07/2026',
    '07/27/2026',
    '5/1/2026',
    '31/02/2026',
    '31/12/2026',
    '12/31/2026',
    '27-07-2026',
    '07-27-2026',
    '27.07.2026',
    '1.1.2026',
    '27/07/26',
    '27-07/2026',
    '  27/07/2026  ',
    '﻿27/07/2026',
    ' 2026-07-27',
    '',
    '   ',
    'not a date',
    '27/07/2026 extra',
    '2026-07-27 extra',
    '١٢/٠١/٢٠٢٦',
    '00/01/2026',
    '01/00/2026',
    '32/01/2026',
    '2026-00-10',
    '2026-1-0',
    '123/01/2026',
  ];
  const formats = DATE_FORMATS.map((f) => f.value);
  for (const s of dateSamples) {
    for (const f of formats) out.push(gc('parseDateWithFormat', [s, f], parseDateWithFormat(s, f)));
  }

  const detectSets = [
    ['2026-07-27', '2026-01-05'],
    ['27/07/2026', '05/01/2026'],
    ['not a date', 'also not'],
    ['05/01/2026', '27/01/2026'],
    ['05/01/2026', '03/02/2026'],
    ['01/27/2026', '02/03/2026'],
    [...Array.from({ length: 19 }, () => '2026-07-27'), 'Total'],
    [...Array.from({ length: 9 }, () => '2026-07-27'), 'Total'],
    [...Array.from({ length: 8 }, () => '2026-07-27'), 'Total', 'Junk'],
    ['05-01-2026', '03-02-2026'],
    ['05-01-2026', '13-02-2026'],
    ['01-13-2026', '02-03-2026'],
    ['05.01.2026', '03.02.2026'],
    ['', '  ', ''],
    [],
    ['5/1/2026', '1/5/2026'],
    ['05/01/2026', '', '03/02/2026'],
    ['2026/01/05', '2026/02/03'],
    ['05/01/2026', '03-02-2026'],
    ['12/12/2026', '01/01/2026', '13/12/2026'],
  ];
  for (const s of detectSets) {
    out.push(gc('detectDateFormatInfo', [s], detectDateFormatInfo(s)));
    out.push(gc('detectDateFormat', [s], detectDateFormat(s) ?? null));
  }

  const amounts = [
    '100',
    '-50.5',
    '₹1,234.56',
    '₹1,23,456.78',
    'Rs. 2,000',
    'Rs.2,000',
    'rs 2000',
    '$1,000.00',
    '(500.00)',
    '( 500.00 )',
    '(-500)',
    '-(500)',
    '',
    '   ',
    'abc',
    '-',
    '+',
    '+5',
    '- 5',
    '-$5.50',
    '$-5',
    '$ - 5',
    '-$-5',
    '1e3',
    '1.5E2',
    '1e-3',
    '1e+3',
    '1e',
    'e3',
    '1e3.5',
    '1e400',
    '-1e400',
    '1e-400',
    '0',
    '-0',
    '0.00',
    '+0',
    '450 USD',
    '450USD',
    'USD 450',
    'INR450.00',
    '450 Cr',
    '450 Dr',
    '450 Dr.',
    '450.00 CR',
    '.5',
    '5.',
    '5..',
    '1.2.3',
    '12abc34',
    '1,2,3',
    ',',
    ',5',
    '5,',
    '1 000',
    '1 000',
    '1 000',
    '₹ 1 23 456',
    '€5',
    '£5',
    '¥5',
    '₹₹5',
    '5₹',
    '5 ₹ ',
    'Rs',
    'Rs.',
    '..5',
    '0x10',
    'Infinity',
    'NaN',
    '١٢٣',
    '１２３',
    '﻿100',
    '(5)(6)',
    '(',
    ')',
    '()',
    '(\n5\n)',
    '5\n',
    'Rs\n5',
    '12,34,567.891',
    '00012',
    '9007199254740993',
    '123456789012345678901234567890',
    '0.1',
    '0.30000000000000004',
    '1.005',
    '-2.675',
  ];
  for (const a of amounts) out.push(gc('parseAmount', [a], parseAmount(a)));

  const base = (p: Partial<CsvImportOptions>): CsvImportOptions => ({
    mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1, noteCol: 2 },
    dateFormat: 'YYYY-MM-DD',
    accountId: 'acc-1',
    categories,
    fallbackCategoryId: 'cat-misc',
    ...p,
  });
  const builds: [string[][], CsvImportOptions][] = [
    [
      [
        ['2026-01-01', '-100', 'Coffee'],
        ['2026-01-02', '5000', 'Paycheck'],
      ],
      base({}),
    ],
    [
      [['2026-01-01', '-100', '']],
      base({
        mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1, negativeIsExpense: false },
      }),
    ],
    [
      [
        ['2026-01-01', '100', ''],
        ['2026-01-01', '-100', ''],
      ],
      base({
        mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1, negativeIsExpense: true },
      }),
    ],
    [
      [
        ['2026-01-01', '250', '', 'Groceries'],
        ['2026-01-02', '', '3000', 'Refund'],
        ['2026-01-03', '100', '100', 'x'],
        ['2026-01-04', '', '', 'y'],
        ['2026-01-05', '-250', '', 'neg debit'],
        ['2026-01-06', '0', '(40)', 'paren credit'],
        ['2026-01-07', 'abc', '', 'junk debit'],
      ],
      base({
        mapping: { dateCol: 0, amountMode: 'debitCredit', debitCol: 1, creditCol: 2, noteCol: 3 },
      }),
    ],
    [
      [['2026-01-01', '', '5']],
      base({ mapping: { dateCol: 0, amountMode: 'debitCredit', creditCol: 2 } }),
    ],
    [[['2026-01-01', '5', '']], base({ mapping: { dateCol: 0, amountMode: 'debitCredit' } })],
    [
      [
        ['2026-01-01', '-100', 'food'],
        ['2026-01-02', '-50', 'Unknown Category'],
        ['2026-01-03', '50', 'FOOD'],
        ['2026-01-04', '50', 'salary'],
        ['2026-01-05', '-50', 'miscellaneous'],
        ['2026-01-06', '-5', '  Cab  '],
        ['2026-01-07', '-5', 'CAFÉ'],
        ['2026-01-08', '-5', ''],
        ['2026-01-09', '-5'],
      ],
      base({ mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1, categoryCol: 2 } }),
    ],
    [
      Array.from({ length: 12 }, (_, i) => ['not-a-date', String(i)]),
      base({ mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1 } }),
    ],
    [
      Array.from({ length: 9 }, (_, i) => ['2026-01-01', i % 2 ? 'x' : '0']),
      base({ mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1 } }),
    ],
    [Array.from({ length: 8 }, () => ['bad', '1']), base({})],
    [
      [
        ['2026-01-01', '-250', 'UBER *TRIP 1234'],
        ['2026-01-02', '-80', 'Something else'],
        ['2026-01-03', '5000', 'SALARY JULY'],
        ['2026-01-04', '-5000', 'salary refund'],
        ['2026-01-05', '-20', '  '],
        ['2026-01-06', '-20', 'tea'],
        ['2026-01-07', '20', '  uber  '],
      ],
      base({ rules: [uberRule, salaryRule, blankIdRule] }),
    ],
    [
      [
        ['2026-01-01', '-250', 'UBER *TRIP', 'Salary'],
        ['2026-01-01', '-250', 'UBER *TRIP', 'Cab'],
        ['2026-01-01', '-250', 'UBER *TRIP', ''],
      ],
      base({
        mapping: { dateCol: 0, amountMode: 'signed', amountCol: 1, noteCol: 2, categoryCol: 3 },
        rules: [uberRule],
      }),
    ],
    [[['2026-01-01', '-250', 'uber']], base({ rules: [] })],
    [
      [
        ['27/07/2026', 'Rs. 1,234.50', ' Coffee '],
        ['31/02/2026', '5', 'bad'],
        ['5/1/2026', '(12)', 'paren'],
      ],
      base({ dateFormat: 'DD/MM/YYYY' }),
    ],
    [[['07-27-2026', '1e3', 'sci']], base({ dateFormat: 'MM-DD-YYYY' })],
    [
      [
        ['27.07.2026', '-0', 'neg zero'],
        ['27.07.2026', '0.001', 'tiny'],
      ],
      base({ dateFormat: 'DD.MM.YYYY' }),
    ],
    [[['2026-01-01']], base({})],
    [[[]], base({})],
    [[], base({})],
    [
      [['x', '2026-01-01', '5']],
      base({ mapping: { dateCol: 1, amountMode: 'signed', amountCol: 2, noteCol: 9 } }),
    ],
    [[['2026-01-01', '5']], base({ mapping: { dateCol: -1, amountMode: 'signed', amountCol: 1 } })],
  ];
  const builtResults: ReturnType<typeof buildTransactionsFromCsv>[] = [];
  for (const [rows, opts] of builds) {
    const r = buildTransactionsFromCsv(rows, opts);
    builtResults.push(r);
    out.push(gc('buildTransactionsFromCsv', [rows, opts], r));
  }

  const candidate = (
    rowIndex: number,
    o: Partial<ParsedCsvTransaction['transaction']> = {},
  ): ParsedCsvTransaction => ({
    rowIndex,
    categoryMatched: true,
    transaction: {
      type: 'expense',
      amount: 100,
      accountId: 'acc-1',
      categoryId: 'cat-food',
      date: '2026-01-01T00:00:00.000Z',
      note: 'Coffee',
      labels: [],
      ...o,
    },
  });
  const existing = (o: Partial<Transaction> = {}): Transaction => ({
    id: 'tx-1',
    type: 'expense',
    amount: 100,
    accountId: 'acc-1',
    categoryId: 'cat-food',
    date: '2026-01-01T00:00:00.000Z',
    note: 'Coffee',
    labels: [],
    createdAt: '2026-01-01T00:00:00.000Z',
    ...o,
  });
  const dupCases: [ParsedCsvTransaction[], Transaction[]][] = [
    [[candidate(0)], [existing()]],
    [[candidate(0, { note: '  COFFEE  ' })], [existing({ note: 'coffee' })]],
    [
      [
        candidate(0, { amount: 200 }),
        candidate(1, { type: 'income' }),
        candidate(2, { date: '2026-01-02T00:00:00.000Z' }),
      ],
      [existing()],
    ],
    [[candidate(0), candidate(1)], []],
    [[candidate(5), candidate(3), candidate(5), candidate(3)], []],
    [
      [
        candidate(0, { amount: 100.004 }),
        candidate(1, { amount: 100.005 }),
        candidate(2, { amount: 1.005 }),
      ],
      [existing({ amount: 100 }), existing({ amount: 1.01 })],
    ],
    [[candidate(0, { date: '2026-01-01T23:59:59.000Z' })], [existing({ date: '2026-01-01' })]],
    [[candidate(0, { note: '﻿coffee ' })], [existing()]],
    [[candidate(0, { note: 'STRASSE' })], [existing({ note: 'straße' })]],
    [[], [existing()]],
    [builtResults[0].accepted, []],
    [
      builtResults[10].accepted,
      [existing({ date: '2026-01-02T00:00:00.000Z', amount: 80, note: 'something else' })],
    ],
  ];
  for (const [cands, ex] of dupCases) {
    out.push(gc('findDuplicateRows', [cands, ex], [...findDuplicateRows(cands, ex)]));
  }

  // The real pipeline end to end: text → parse → guess → detect → build.
  for (const text of CSV_TEXTS) {
    const { headers, rows } = parseCsvText(text);
    const g = guessColumnMapping(headers);
    if (g.dateCol === undefined) continue;
    const fmt: DateFormatCode =
      detectDateFormat(rows.map((r) => r[g.dateCol!] ?? '')) ?? 'YYYY-MM-DD';
    const opts = base({
      mapping: { ...g, dateCol: g.dateCol },
      dateFormat: fmt,
      rules: [uberRule],
    });
    out.push(
      gc('pipeline', [text, categories, [uberRule]], {
        headers,
        guess: g,
        format: fmt,
        result: buildTransactionsFromCsv(rows, opts),
      }),
    );
  }
  return out;
}
