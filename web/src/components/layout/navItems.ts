import {
  LayoutDashboard,
  Wallet,
  ArrowLeftRight,
  BarChart3,
  Settings,
  Target,
  Repeat,
  PiggyBank,
  HandCoins,
  Landmark,
  Store,
  PartyPopper,
  type LucideIcon,
} from 'lucide-react';

export const navTabs: { path: string; icon: LucideIcon; label: string; desktopOnly?: boolean }[] = [
  { path: '/', icon: LayoutDashboard, label: 'Home' },
  { path: '/accounts', icon: Wallet, label: 'Accounts' },
  { path: '/transactions', icon: ArrowLeftRight, label: 'Txns' },
  { path: '/analytics', icon: BarChart3, label: 'Analytics' },
  // Mobile reaches Settings from the Dashboard header button instead of a tab.
  { path: '/settings', icon: Settings, label: 'Settings', desktopOnly: true },
];

/** Secondary destinations — the Sidebar's "Tools" group and the mobile `/tools` page. */
export const moreNavItems = [
  {
    path: '/budgets',
    icon: Target,
    label: 'Budgets',
    description: 'Spending limits by category or label',
  },
  {
    path: '/recurring',
    icon: Repeat,
    label: 'Recurring',
    description: 'Bills, subscriptions and income that repeat',
  },
  {
    path: '/goals',
    icon: PiggyBank,
    label: 'Goals',
    description: 'Savings targets and contributions',
  },
  { path: '/debts', icon: HandCoins, label: 'Debts', description: "Money you've lent or owe" },
  { path: '/loans', icon: Landmark, label: 'Loans', description: 'EMIs, schedule and prepayments' },
  {
    path: '/merchants',
    icon: Store,
    label: 'Merchants',
    description: 'Where your money goes, grouped by name',
  },
  {
    path: '/year-in-review',
    icon: PartyPopper,
    label: 'Year in review',
    description: 'Your financial year, looked back on',
  },
];

/** A tab owns its sub-routes (e.g. `/settings/backup`), except Home, which owns only `/`. */
export const isTabActive = (pathname: string, tabPath: string) =>
  tabPath === '/' ? pathname === '/' : pathname === tabPath || pathname.startsWith(`${tabPath}/`);
