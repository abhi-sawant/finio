// Shared sample data — mirrors web/src/data/sampleData.ts values shown on the landing captures,
// so every theme prototype shows the exact same product state.
export const D = {
  user: 'Asha',
  safe: '1,562',
  left: '25,000',
  days: 16,
  spent: '15,000',
  budget: '40,000',
  pct: 38,
  dayPct: 48, // day 15 of 31
  total: '1,50,075',
  accounts: [
    { name: 'HDFC Checking', type: 'Bank account', bal: '1,11,925', short: '1.1L', kind: 'bank' },
    { name: 'Savings', type: 'Savings', bal: '40,075', short: '40K', kind: 'savings' },
    { name: 'Credit Card', type: 'Credit card', bal: '1,925', short: '1.9K', kind: 'card', owed: true },
  ],
  due: { name: 'Credit Card', days: 3, amt: '1,925' },
  groups: [
    {
      date: 'Fri, 2 Oct',
      rows: [
        { name: 'Salary', cat: 'Income', acct: 'HDFC Checking', amt: '85,000', inc: true, icon: 'banknote', tag: null },
        { name: 'Monthly rent', cat: 'Housing', acct: 'HDFC Checking', amt: '15,000', icon: 'house', tag: 'Essential', rec: true },
      ],
    },
    {
      date: 'Sun, 27 Sep',
      rows: [{ name: 'Lunch with friends', cat: 'Food', acct: 'HDFC Checking', amt: '275', icon: 'utensils', tag: 'Discretionary' }],
    },
    {
      date: 'Thu, 24 Sep',
      rows: [{ name: 'Groceries', cat: 'Food', acct: 'HDFC Checking', amt: '2,000', icon: 'utensils', tag: 'Essential' }],
    },
    {
      date: 'Tue, 22 Sep',
      rows: [{ name: 'Cab rides', cat: 'Transport', acct: 'HDFC Checking', amt: '450', icon: 'car', tag: null }],
    },
  ],
  cats: [
    ['Food', 'utensils'], ['Transport', 'car'], ['Shopping', 'bag'], ['Entertainment', 'film'],
    ['Utilities', 'zap'], ['Healthcare', 'heart'], ['Education', 'book'], ['Housing', 'house'],
  ],
  budgets: [
    { name: 'Overall expenses', spent: '15,000', limit: '40,000', left: '25,000', pct: 38, state: 'On track' },
    { name: 'Housing', spent: '15,000', limit: '16,000', left: '1,000', pct: 94, state: 'Near limit' },
    { name: 'Food', spent: '0', limit: '8,000', left: '8,000', pct: 0, state: 'On track' },
    { name: 'Shopping', spent: '0', limit: '3,000', left: '3,000', pct: 0, state: 'On track' },
  ],
  goals: [
    { name: 'Emergency Fund', have: '20,000', target: '1,00,000', pct: 20 },
    { name: 'Vacation Fund', have: '0', target: '60,000', pct: 0 },
  ],
  debt: { who: 'Rahul', amt: '1,500' },
  nav: [
    ['Home', 'home'], ['Accounts', 'wallet'], ['Transactions', 'arrows'], ['Analytics', 'chart'], ['Settings', 'sliders'],
  ],
  tools: ['Budgets', 'Recurring', 'Goals', 'Debts', 'Loans', 'Merchants', 'Year in review'],
};
