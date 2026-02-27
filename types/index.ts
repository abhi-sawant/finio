export type AccountType = 'checking' | 'savings' | 'cash' | 'credit' | 'investment' | 'wallet'
export type TransactionType = 'expense' | 'income' | 'transfer'
export type CategoryType = 'expense' | 'income' | 'both'
export type Currency = 'USD' | 'EUR' | 'GBP' | 'INR' | 'JPY' | 'CAD' | 'AUD'
export type Theme = 'dark' | 'light'

export interface Account {
  id: string
  name: string
  type: AccountType
  currency: Currency
  color: string
  icon: string
  balance: number
  createdAt: string
}

export interface Transaction {
  id: string
  type: TransactionType
  amount: number
  accountId: string
  toAccountId?: string
  categoryId: string
  date: string
  note: string
  labels: string[]
  createdAt: string
}

export interface Category {
  id: string
  name: string
  icon: string
  color: string
  type: CategoryType
}

export interface Label {
  id: string
  name: string
  color: string
}

export interface Settings {
  currency: Currency
  theme: Theme
  userName: string
  useBiometrics: boolean
  hapticFeedback: boolean
  notifications: boolean
}

export interface MonthlySummary {
  month: number
  year: number
  income: number
  expenses: number
  net: number
}

export interface FinanceStore {
  accounts: Account[]
  transactions: Transaction[]
  categories: Category[]
  labels: Label[]
  settings: Settings
  isHydrated: boolean

  // Account actions
  addAccount: (account: Omit<Account, 'id' | 'createdAt'>) => void
  updateAccount: (id: string, updates: Partial<Omit<Account, 'id'>>) => void
  deleteAccount: (id: string) => void

  // Transaction actions
  addTransaction: (transaction: Omit<Transaction, 'id' | 'createdAt'>) => void
  updateTransaction: (id: string, updates: Partial<Omit<Transaction, 'id'>>) => void
  deleteTransaction: (id: string) => void

  // Category actions
  addCategory: (category: Omit<Category, 'id'>) => void
  updateCategory: (id: string, updates: Partial<Omit<Category, 'id'>>) => void
  deleteCategory: (id: string) => void

  // Label actions
  addLabel: (label: Omit<Label, 'id'>) => void
  updateLabel: (id: string, updates: Partial<Omit<Label, 'id'>>) => void
  deleteLabel: (id: string) => void

  // Settings actions
  updateSettings: (updates: Partial<Settings>) => void

  // Data actions
  resetToDefaults: () => void
  importData: (data: Partial<Pick<FinanceStore, 'accounts' | 'transactions' | 'categories' | 'labels' | 'settings'>>) => void

  // Internal
  setHydrated: (hydrated: boolean) => void
}
