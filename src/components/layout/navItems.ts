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
} from 'lucide-react';

export const navTabs = [
  { path: '/', icon: LayoutDashboard, label: 'Home' },
  { path: '/accounts', icon: Wallet, label: 'Accounts' },
  { path: '/transactions', icon: ArrowLeftRight, label: 'Txns' },
  { path: '/analytics', icon: BarChart3, label: 'Analytics' },
  { path: '/settings', icon: Settings, label: 'Settings' },
];

/** Secondary destinations — the Sidebar's "Manage" group and the mobile "More" popover. */
export const moreNavItems = [
  { path: '/budgets', icon: Target, label: 'Budgets' },
  { path: '/recurring', icon: Repeat, label: 'Recurring' },
  { path: '/goals', icon: PiggyBank, label: 'Goals' },
  { path: '/debts', icon: HandCoins, label: 'Debts' },
  { path: '/loans', icon: Landmark, label: 'Loans' },
  { path: '/merchants', icon: Store, label: 'Merchants' },
  { path: '/year-in-review', icon: PartyPopper, label: 'Year in Review' },
];
