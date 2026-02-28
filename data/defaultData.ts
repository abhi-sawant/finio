import { subDays, formatISO } from 'date-fns'
import type { Account, Category, Label, Settings, Transaction } from '@/types'

const now = new Date()
const d = (daysAgo: number): string => formatISO(subDays(now, daysAgo))

export const defaultAccounts: Account[] = [
  {
    id: 'acc-1',
    name: 'HDFC Checking',
    type: 'checking',
    currency: 'INR',
    color: '#6C63FF',
    icon: 'landmark',
    balance: 85000,
    createdAt: d(120),
  },
  {
    id: 'acc-2',
    name: 'SBI Savings',
    type: 'savings',
    currency: 'INR',
    color: '#22c55e',
    icon: 'piggy-bank',
    balance: 240000,
    createdAt: d(120),
  },
  {
    id: 'acc-3',
    name: 'Cash Wallet',
    type: 'cash',
    currency: 'INR',
    color: '#f59e0b',
    icon: 'wallet',
    balance: 3500,
    createdAt: d(120),
  },
]

export const defaultCategories: Category[] = [
  // Expense categories
  { id: 'cat-1', name: 'Food & Dining', icon: 'utensils', color: '#ef4444', type: 'expense' },
  { id: 'cat-2', name: 'Transport', icon: 'car', color: '#f97316', type: 'expense' },
  { id: 'cat-3', name: 'Shopping', icon: 'shopping-bag', color: '#8b5cf6', type: 'expense' },
  { id: 'cat-4', name: 'Entertainment', icon: 'film', color: '#ec4899', type: 'expense' },
  { id: 'cat-5', name: 'Utilities', icon: 'zap', color: '#06b6d4', type: 'expense' },
  { id: 'cat-6', name: 'Healthcare', icon: 'heart-pulse', color: '#10b981', type: 'expense' },
  { id: 'cat-7', name: 'Education', icon: 'book-open', color: '#3b82f6', type: 'expense' },
  { id: 'cat-8', name: 'Rent & Housing', icon: 'home', color: '#64748b', type: 'expense' },
  { id: 'cat-15', name: 'Travel', icon: 'plane', color: '#ef4444', type: 'expense' },
  { id: 'cat-16', name: 'Gifts & Donations', icon: 'gift', color: '#f97316', type: 'expense' },
  { id: 'cat-17', name: 'Personal Care', icon: 'scissors', color: '#8b5cf6', type: 'expense' },
  { id: 'cat-18', name: 'Subscriptions', icon: 'repeat', color: '#ec4899', type: 'expense' },
  { id: 'cat-19', name: 'Vehicles', icon: 'truck', color: '#06b6d4', type: 'expense' },
  { id: 'cat-20', name: 'Financial Expenses', icon: 'dollar-sign', color: '#10b981', type: 'expense' },
  { id: 'cat-11', name: 'Investments', icon: 'trending-up', color: '#f59e0b', type: 'expense' },
  // Income categories
  { id: 'cat-9', name: 'Salary', icon: 'briefcase', color: '#22c55e', type: 'income' },
  { id: 'cat-10', name: 'Freelance', icon: 'laptop', color: '#6C63FF', type: 'income' },
  { id: 'cat-12', name: 'Business', icon: 'building-2', color: '#a855f7', type: 'income' },
  { id: 'cat-21', name: 'Gifts (Income)', icon: 'gift', color: '#f97316', type: 'income' },
  { id: 'cat-22', name: 'Rental Income', icon: 'home', color: '#3b82f6', type: 'income' },
  { id: 'cat-23', name: 'Interest Income', icon: 'dollar-sign', color: '#10b981', type: 'income' },
  // Both
  { id: 'cat-13', name: 'Transfer', icon: 'repeat', color: '#3b82f6', type: 'both' },
  { id: 'cat-14', name: 'Other', icon: 'circle-ellipsis', color: '#94a3b8', type: 'both' },
  { id: 'cat-24', name: 'Miscellaneous', icon: 'circle-ellipsis', color: '#94a3b8', type: 'both' },
]

export const defaultLabels: Label[] = [
  { id: 'lbl-1', name: 'Essential', color: '#22c55e' },
  { id: 'lbl-2', name: 'Discretionary', color: '#f59e0b' },
  { id: 'lbl-3', name: 'Recurring', color: '#3b82f6' },
  { id: 'lbl-4', name: 'Tax', color: '#ef4444' },
  { id: 'lbl-5', name: 'Financial Obligation', color: '#10b981' },
  { id: 'lbl-6', name: 'Investment', color: '#8b5cf6' },
  { id: 'lbl-7', name: 'Lending', color: '#ec4899' },
  { id: 'lbl-8', name: 'My Expense', color: '#64748b' },
  { id: 'lbl-9', name: 'Other\'s Expense', color: '#06b6d4' },
]

export const defaultTransactions: Transaction[] = [
  // Income
  {
    id: 'txn-1', type: 'income', amount: 120000, accountId: 'acc-1',
    categoryId: 'cat-9', date: d(60), note: 'Monthly Salary - January',
    labels: ['lbl-3'], createdAt: d(60),
  },
  {
    id: 'txn-2', type: 'income', amount: 120000, accountId: 'acc-1',
    categoryId: 'cat-9', date: d(30), note: 'Monthly Salary - February',
    labels: ['lbl-3'], createdAt: d(30),
  },
  {
    id: 'txn-3', type: 'income', amount: 35000, accountId: 'acc-1',
    categoryId: 'cat-10', date: d(45), note: 'Freelance - UI Design Project',
    labels: ['lbl-4'], createdAt: d(45),
  },
  {
    id: 'txn-4', type: 'income', amount: 12500, accountId: 'acc-2',
    categoryId: 'cat-11', date: d(15), note: 'Mutual Fund Returns',
    labels: ['lbl-4'], createdAt: d(15),
  },
  {
    id: 'txn-5', type: 'income', amount: 8000, accountId: 'acc-1',
    categoryId: 'cat-10', date: d(20), note: 'Freelance - Logo Design',
    labels: [], createdAt: d(20),
  },

  // Expenses
  {
    id: 'txn-6', type: 'expense', amount: 18000, accountId: 'acc-1',
    categoryId: 'cat-8', date: d(58), note: 'Rent - January',
    labels: ['lbl-1', 'lbl-3'], createdAt: d(58),
  },
  {
    id: 'txn-7', type: 'expense', amount: 18000, accountId: 'acc-1',
    categoryId: 'cat-8', date: d(28), note: 'Rent - February',
    labels: ['lbl-1', 'lbl-3'], createdAt: d(28),
  },
  {
    id: 'txn-8', type: 'expense', amount: 4500, accountId: 'acc-1',
    categoryId: 'cat-1', date: d(5), note: 'Zomato & Swiggy orders',
    labels: ['lbl-2'], createdAt: d(5),
  },
  {
    id: 'txn-9', type: 'expense', amount: 1200, accountId: 'acc-3',
    categoryId: 'cat-2', date: d(3), note: 'Uber rides this week',
    labels: [], createdAt: d(3),
  },
  {
    id: 'txn-10', type: 'expense', amount: 6800, accountId: 'acc-1',
    categoryId: 'cat-3', date: d(10), note: 'Amazon shopping',
    labels: ['lbl-2'], createdAt: d(10),
  },
  {
    id: 'txn-11', type: 'expense', amount: 2400, accountId: 'acc-1',
    categoryId: 'cat-5', date: d(25), note: 'Electricity & internet bill',
    labels: ['lbl-1', 'lbl-3'], createdAt: d(25),
  },
  {
    id: 'txn-12', type: 'expense', amount: 3200, accountId: 'acc-1',
    categoryId: 'cat-1', date: d(35), note: 'Grocery shopping - D-Mart',
    labels: ['lbl-1'], createdAt: d(35),
  },
  {
    id: 'txn-13', type: 'expense', amount: 1500, accountId: 'acc-3',
    categoryId: 'cat-4', date: d(18), note: 'Movie tickets - PVR',
    labels: ['lbl-2'], createdAt: d(18),
  },
  {
    id: 'txn-14', type: 'expense', amount: 850, accountId: 'acc-3',
    categoryId: 'cat-2', date: d(7), note: 'Auto & metro',
    labels: [], createdAt: d(7),
  },
  {
    id: 'txn-15', type: 'expense', amount: 4200, accountId: 'acc-1',
    categoryId: 'cat-6', date: d(50), note: 'Doctor visit + medicines',
    labels: ['lbl-1'], createdAt: d(50),
  },
  {
    id: 'txn-16', type: 'expense', amount: 12000, accountId: 'acc-1',
    categoryId: 'cat-7', date: d(55), note: 'Online course subscription',
    labels: ['lbl-4'], createdAt: d(55),
  },
  {
    id: 'txn-17', type: 'expense', amount: 920, accountId: 'acc-1',
    categoryId: 'cat-5', date: d(55), note: 'Netflix + Spotify',
    labels: ['lbl-3'], createdAt: d(55),
  },
  {
    id: 'txn-18', type: 'expense', amount: 5500, accountId: 'acc-1',
    categoryId: 'cat-3', date: d(40), note: 'H&M clothing',
    labels: ['lbl-2'], createdAt: d(40),
  },
  {
    id: 'txn-19', type: 'expense', amount: 2800, accountId: 'acc-1',
    categoryId: 'cat-1', date: d(12), note: 'Restaurant dinner with friends',
    labels: ['lbl-2'], createdAt: d(12),
  },
  {
    id: 'txn-20', type: 'expense', amount: 600, accountId: 'acc-3',
    categoryId: 'cat-4', date: d(2), note: 'Board game café',
    labels: ['lbl-2'], createdAt: d(2),
  },

  // Transfers
  {
    id: 'txn-21', type: 'transfer', amount: 30000, accountId: 'acc-1',
    toAccountId: 'acc-2', categoryId: 'cat-13', date: d(62),
    note: 'Transfer to savings', labels: [], createdAt: d(62),
  },
  {
    id: 'txn-22', type: 'transfer', amount: 5000, accountId: 'acc-1',
    toAccountId: 'acc-3', categoryId: 'cat-13', date: d(14),
    note: 'Cash withdrawal', labels: [], createdAt: d(14),
  },
  {
    id: 'txn-23', type: 'transfer', amount: 20000, accountId: 'acc-1',
    toAccountId: 'acc-2', categoryId: 'cat-13', date: d(32),
    note: 'Monthly savings', labels: [], createdAt: d(32),
  },

  // Additional expenses
  {
    id: 'txn-24', type: 'expense', amount: 1800, accountId: 'acc-1',
    categoryId: 'cat-1', date: d(1), note: 'Weekly groceries',
    labels: ['lbl-1'], createdAt: d(1),
  },
  {
    id: 'txn-25', type: 'expense', amount: 450, accountId: 'acc-3',
    categoryId: 'cat-2', date: d(1), note: 'Auto to office',
    labels: [], createdAt: d(1),
  },
]

export const defaultSettings: Settings = {
  currency: 'INR',
  theme: 'system',
  userName: 'Alex',
  useBiometrics: false,
  hapticFeedback: true,
  notifications: false,
}
