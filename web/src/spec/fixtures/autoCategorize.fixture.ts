import {
  MATCH_TYPES,
  MATCH_TYPE_LABELS,
  findMatchingRule,
  isValidPattern,
  mergeLabels,
  planRuleApplication,
  ruleMatches,
} from '@/utils/autoCategorize';
import type { CategoryRule, RuleMatchType, RuleScope, Transaction, TransactionType } from '@/types';
import { gc, type GoldenCase } from '../golden';

const rule = (p: Partial<CategoryRule> & Pick<CategoryRule, 'id' | 'pattern'>): CategoryRule => ({
  matchType: 'contains',
  scope: 'any',
  categoryId: 'cat-transport',
  labelIds: [],
  enabled: true,
  createdAt: '2026-01-01T00:00:00.000Z',
  ...p,
});

const tx = (p: Partial<Transaction> & Pick<Transaction, 'id'>): Transaction => ({
  type: 'expense',
  amount: 100,
  accountId: 'acc-1',
  categoryId: 'cat-misc',
  date: '2026-06-01T00:00:00.000Z',
  note: '',
  labels: [],
  createdAt: '2026-06-01T00:00:00.000Z',
  ...p,
});

/** JS-regex syntax edge cases: validity is decided by JS rules (Annex B), not Java's. */
const SYNTAX = [
  'uber|ola',
  '([',
  '(',
  ')',
  '[',
  ']',
  '{',
  '}',
  'a{',
  'a{2}',
  '{2}',
  'a{2,1}',
  'x{,5}',
  'a**',
  'a*?',
  'a*??',
  'a++',
  '*',
  '+a',
  '?',
  '^*',
  '$*',
  '\\b*',
  '(?=a)*',
  '(?!a)+',
  '(?<=a)*',
  '(?<=a+)b',
  '(?i)abc',
  '(?i:abc)',
  '(?-i:abc)',
  '(?ii:a)',
  '(?i-i:a)',
  '(?x:a)',
  '(?-:a)',
  '(?ims:a)',
  '(?i-:a)',
  '(?<n>a)\\k<n>',
  '\\k<n>',
  '\\k',
  '(?<n>a)\\k',
  '(?<n>a)\\k<m>',
  '(?<n>a)|(?<n>b)',
  '(?<n>a)(?<n>b)',
  '((?<n>a)|b)(?<n>c)',
  '(?:(?<n>a)|b)|(?<n>c)',
  '(?<1n>a)',
  '(?<$x>a)',
  '(?<é>a)',
  '(?<>a)',
  '\\',
  'a\\',
  '\\c',
  '\\cA',
  '[\\c1]',
  '[\\c]',
  '\\1',
  '(a)\\1',
  '(a)\\2',
  '\\8',
  '\\08',
  '\\377',
  '\\400',
  '\\x4',
  '\\x41',
  '\\u004',
  '\\u0041',
  '\\u{41}',
  '\\p{L}',
  '\\Q',
  '[z-a]',
  '[a-z]',
  '[\\d-z]',
  '[a-\\d]',
  '[]',
  '[^]',
  '[]a]',
  '[\\b]',
  '[\\B]',
  '[\\k]',
  '(?<n>x)[\\k]',
  '(?<n>x)[\\k<n>]',
  'a|',
  '|',
  '()',
  '(?:)',
  '(?',
  '(?<',
  '(?<=',
  '(?<a',
  '[\\]',
  '[\\]]',
  '\\/',
  '/',
  'a{2147483648}',
  '\\-',
  '[\\-]',
  '(?<=\\1(a))',
  'a{1,2}{3}',
  'a{1}?',
  'a{0}',
  '(?<n>a)\\k<n',
  '\\k<n',
  '[a-]',
  '[-a]',
  '[a-b-c]',
  '[\\w-a]',
  'x{1,}',
  'x{ 1}',
  'a{1,2',
  '\\0',
  '\\00',
  '\\01',
  '\\9',
  '(a)\\10',
  '\\u{1F389}',
  '(?<a>.)(?<b>.)\\k<a>',
  '[\\u]',
  '[\\x]',
  '\\ca',
  '\\c_',
  '[\\c_]',
  '(?m:^a$)',
  '(?s:.)',
  '(?i:a)(?-i:b)',
  '(?=a){2}',
  '(?<!a)?',
  'a{,}',
  '\\B*',
  '(*)',
  '(|)',
  '[[]',
  '[[]]',
  '\\]',
  'a]',
  '(?:a|b)+?',
  '\\d+(?:\\.\\d{2})?',
  '^swiggy|zomato$',
  '[',
  '\\',
  '(?<n>a)(?<n>b)',
];

/** [pattern, notes] pairs exercising JS matching semantics. */
const MATCHING: [string, string[]][] = [
  ['uber', ['UBER trip home', 'Uber', 'ub er', '', 'SUBERB']],
  ['^ub(er|ur)', ['Ubur ride', 'an uber', 'UBER']],
  ['uber$', ['my uber', 'uber\n', 'uber ', 'UBER']],
  ['^uber', ['\nuber', 'uber']],
  ['(?m:^uber$)', ['x\nuber\ny', 'x\r\nUBER', 'uberx']],
  ['a.c', ['abc', 'a\nc', 'a\rc', 'a c', 'a\u0085c', 'aÿc', 'a😀c']],
  ['(?s:a.c)', ['a\nc', 'abc']],
  ['\\bola\\b', ['ola cab', 'olacab', 'éola', 'ola_', '_ola', 'çolaé', 'ola9']],
  ['\\Bola', ['cola', 'ola']],
  ['\\s', ['a b', 'a b', 'a﻿b', 'a b', 'a\u0085b', 'a\u001cb', 'ab', 'a᠎b']],
  ['\\S+', ['   ', 'x']],
  ['\\w+@', ['é@', 'a@', 'ſ@', '_@']],
  ['\\d', ['١', '9', 'x']],
  ['straße', ['STRASSE', 'STRAßE', 'Straße']],
  ['ſ', ['s', 'S', 'ſ']],
  ['s', ['ſ', 'S']],
  ['k', ['K', 'K']],
  ['K', ['k', 'K', 'K']],
  ['i', ['I', 'İ', 'ı']],
  ['ı', ['I', 'i', 'ı']],
  ['é', ['É', 'e']],
  ['[a-z]+', ['ÀB', 'AB', 'ſ', 'K']],
  ['[^a-z]', ['A', 'é', '😀']],
  ['[^]', ['', 'x', '\n']],
  ['[]', ['', 'x']],
  ['σ', ['Σ', 'ς', 'σ']],
  ['ǅ', ['Ǆ', 'ǆ', 'ǅ']],
  ['µ', ['Μ', 'μ', 'µ']],
  ['\\u0041', ['a', 'A']],
  ['\\x41', ['a']],
  ['\\x4', ['x4', 'X4']],
  ['\\u{41}', ['u{41}', 'uuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu', 'A']],
  ['\\p{L}', ['p{L}', 'P{l}', 'é']],
  ['\\cA', ['\u0001', 'cA']],
  ['\\c', ['\\c', 'c']],
  ['[\\c]', ['\\', 'c', 'x']],
  ['[\\c1]', ['\u0011', '1']],
  ['\\1', ['\u0001', '1']],
  ['(a)\\1', ['aa', 'aA', 'a']],
  ['\\8', ['8']],
  ['\\08', ['\u00008', '8']],
  ['\\377', ['ÿ', 'ÿ', 'Ÿ']],
  ['\\400', [' 0', ' 0']],
  ['a{', ['a{', 'A{']],
  ['x{,5}', ['x{,5}', 'xxxxx']],
  ['a{2}', ['aa', 'a']],
  ['a{0}b', ['b']],
  ['a]', ['a]']],
  ['[]a]', ['a]', 'a']],
  ['\\Q', ['Q', 'q']],
  ['(?<n>a)\\k<n>', ['aa', 'aA']],
  ['\\k<n>', ['k<n>', 'K<N>']],
  ['(?<n>a)|(?<n>b)', ['b', 'a', 'c']],
  ['(?=a)*b', ['b']],
  ['(?<=a+)b', ['aab', 'b']],
  ['(?<=uber )eats', ['uber eats', 'eats']],
  ['(?i:a)(?-i:b)', ['Ab', 'AB']],
  ['uber|', ['', 'anything']],
  ['', ['', 'x']],
  ['😀', ['😀', 'x😀']],
  ['😀+', ['😀😀']],
  ['[😀]', ['😀', '\ud83d']],
  ['\\d+(?:\\.\\d{2})?', ['Rs 12.50', 'Rs', '٣']],
  ['^swiggy|zomato$', ['Swiggy order', 'order ZOMATO', 'zomato order']],
  ['ub[\\b]er', ['ub\ber']],
  ['[\\B]', ['B', 'b']],
  ['[\\d-z]', ['-', '5', 'z', 'm']],
  ['[a-]', ['-']],
  ['\\-', ['-']],
  ['[$€₹]\\d', ['₹5', '$5', '€5', '£5']],
  ['\\/', ['/']],
  ['(?:a|b)+?', ['ab']],
  ['([', ['([']],
];

const NOTES = [
  'Uber to office',
  'UBER EATS order',
  '  uber  ',
  'Ola ride',
  '',
  '   ',
  'uber',
  'Taxi: UBER',
  'Über ride',
  'ÜBER',
  'uber ',
  '﻿uber',
];

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('constants', [], { MATCH_TYPES, MATCH_TYPE_LABELS }));

  const types: RuleMatchType[] = ['contains', 'startsWith', 'endsWith', 'equals', 'regex'];
  for (const p of [...SYNTAX, '', ' ', ' ', '﻿', '\u001c', ' uber ']) {
    for (const t of types) out.push(gc('isValidPattern', [p, t], isValidPattern(p, t)));
  }

  for (const [pattern, notes] of MATCHING) {
    const r = rule({ id: 'r', pattern, matchType: 'regex' });
    for (const note of notes)
      out.push(gc('ruleMatches', [r, note, 'expense'], ruleMatches(r, note, 'expense')));
  }
  // Every syntax-grid pattern against a couple of probe notes (an invalid one must match nothing).
  for (const pattern of SYNTAX) {
    const r = rule({ id: 'r', pattern, matchType: 'regex' });
    for (const note of [pattern, 'a', 'uber']) {
      out.push(gc('ruleMatches', [r, note, 'expense'], ruleMatches(r, note, 'expense')));
    }
  }

  // Literal match types × scopes × types × enabled.
  const literal: [string, RuleMatchType][] = [
    ['uber', 'contains'],
    ['UBER', 'startsWith'],
    ['  ride ', 'endsWith'],
    ['uber', 'equals'],
    ['taxi: uber', 'equals'],
    ['über', 'contains'],
    ['   ', 'contains'],
    ['İ', 'contains'],
    ['uber ', 'endsWith'],
  ];
  const scopes: RuleScope[] = ['any', 'expense', 'income'];
  const txTypes: TransactionType[] = ['expense', 'income', 'transfer'];
  for (const [pattern, matchType] of literal) {
    for (const scope of scopes) {
      const r = rule({ id: 'r', pattern, matchType, scope });
      for (const type of txTypes) {
        for (const note of NOTES)
          out.push(gc('ruleMatches', [r, note, type], ruleMatches(r, note, type)));
      }
    }
  }
  const disabled = rule({ id: 'r', pattern: 'uber', enabled: false });
  out.push(
    gc('ruleMatches', [disabled, 'uber', 'expense'], ruleMatches(disabled, 'uber', 'expense')),
  );
  out.push(
    gc(
      'ruleMatches',
      [rule({ id: 'r', pattern: 'i̇', matchType: 'contains' }), 'İSTANBUL', 'expense'],
      ruleMatches(rule({ id: 'r', pattern: 'i̇', matchType: 'contains' }), 'İSTANBUL', 'expense'),
    ),
  );

  const rules = [
    rule({ id: 'r1', pattern: 'uber eats', categoryId: 'cat-food' }),
    rule({ id: 'r2', pattern: 'uber', categoryId: 'cat-transport' }),
    rule({
      id: 'r3',
      pattern: '^ola',
      matchType: 'regex',
      scope: 'expense',
      categoryId: 'cat-cab',
    }),
    rule({
      id: 'r4',
      pattern: 'salary',
      scope: 'income',
      categoryId: 'cat-salary',
      labelIds: ['lbl-a'],
    }),
    rule({ id: 'r5', pattern: '([', matchType: 'regex', categoryId: 'cat-broken' }),
  ];
  const disabledFirst = [{ ...rules[0], enabled: false }, ...rules.slice(1)];
  for (const rs of [rules, disabledFirst, [], [rules[4]]]) {
    for (const note of [
      'Uber Eats order',
      'Uber ride',
      'OLA cab',
      'my ola',
      'June salary',
      '   ',
      '',
      '([',
    ]) {
      for (const type of txTypes) {
        out.push(
          gc('findMatchingRule', [rs, note, type], findMatchingRule(rs, note, type) ?? null),
        );
      }
    }
  }

  for (const [a, b] of [
    [
      ['a', 'b'],
      ['b', 'c'],
    ],
    [['a'], []],
    [[], ['x', 'x']],
    [['a', 'a'], ['a']],
    [[], []],
  ]) {
    out.push(gc('mergeLabels', [a, b], mergeLabels(a, b)));
  }

  const essential = [
    rule({ id: 'r1', pattern: 'uber', categoryId: 'cat-transport', labelIds: ['lbl-essential'] }),
  ];
  const history = [
    tx({ id: 't1', note: 'Uber to office' }),
    tx({ id: 't2', note: 'Uber', categoryId: 'cat-transport', labels: ['lbl-essential'] }),
    tx({ id: 't3', note: 'Uber', categoryId: 'cat-transport' }),
    tx({ id: 't4', note: 'Uber', type: 'transfer', toAccountId: 'acc-2' }),
    tx({
      id: 't5',
      note: 'Uber',
      categoryId: '',
      splits: [
        { categoryId: 'cat-food', amount: 60 },
        { categoryId: 'cat-transport', amount: 40 },
      ],
    }),
    tx({ id: 't6', note: 'Uber', splits: [] }),
    tx({ id: 't7', note: '  ' }),
    tx({ id: 't8', note: 'Uber', categoryId: 'cat-shopping', labels: ['lbl-x'] }),
    tx({ id: 't9', note: 'ola ride', type: 'income' }),
    tx({
      id: 't10',
      note: 'June salary',
      type: 'income',
      labels: ['lbl-a'],
      categoryId: 'cat-salary',
    }),
    tx({ id: 't11', note: 'June salary', type: 'income' }),
  ];
  const allRules = [...essential, ...rules];
  for (const [rs, opts] of [
    [essential, undefined],
    [essential, { restrictToCategoryId: 'cat-misc' }],
    [essential, { restrictToCategoryId: '' }],
    [allRules, undefined],
    [allRules, { restrictToCategoryId: 'cat-shopping' }],
    [
      [
        rule({ id: 'x', pattern: 'uber', enabled: false }),
        rule({ id: 'y', pattern: '([', matchType: 'regex' }),
      ],
      undefined,
    ],
    [[], undefined],
  ] as const) {
    out.push(
      gc(
        'planRuleApplication',
        [history, rs, opts ?? null],
        planRuleApplication(history, [...rs], opts ? { ...opts } : undefined),
      ),
    );
  }
  return out;
}
