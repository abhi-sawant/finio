import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import AsyncStorage from '@react-native-async-storage/async-storage'
import {
  defaultAccounts,
  defaultCategories,
  defaultLabels,
  defaultTransactions,
  defaultSettings,
} from '@/data/defaultData'
import type { Account, Category, FinanceStore, Label, Transaction } from '@/types'

function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

const defaultState = {
  accounts: defaultAccounts,
  transactions: defaultTransactions,
  categories: defaultCategories,
  labels: defaultLabels,
  settings: defaultSettings,
  isHydrated: false,
}

export const useFinanceStore = create<FinanceStore>()(
  persist(
    (set, get) => ({
      ...defaultState,

      setHydrated: (hydrated) => set({ isHydrated: hydrated }),

      // --------------- Account Actions ---------------
      addAccount: (accountData) => {
        const account: Account = {
          ...accountData,
          id: generateUUID(),
          createdAt: new Date().toISOString(),
        }
        set((state) => ({ accounts: [...state.accounts, account] }))
      },

      updateAccount: (id, updates) => {
        set((state) => ({
          accounts: state.accounts.map((a) => (a.id === id ? { ...a, ...updates } : a)),
        }))
      },

      deleteAccount: (id) => {
        set((state) => ({
          accounts: state.accounts.filter((a) => a.id !== id),
          // Cascade delete transactions associated with this account
          transactions: state.transactions.filter(
            (t) => t.accountId !== id && t.toAccountId !== id
          ),
        }))
      },

      // --------------- Transaction Actions ---------------
      addTransaction: (txData) => {
        const transaction: Transaction = {
          ...txData,
          id: generateUUID(),
          createdAt: new Date().toISOString(),
        }

        set((state) => {
          const updatedAccounts = state.accounts.map((account) => {
            if (transaction.type === 'transfer') {
              if (account.id === transaction.accountId) {
                return { ...account, balance: account.balance - transaction.amount }
              }
              if (transaction.toAccountId && account.id === transaction.toAccountId) {
                return { ...account, balance: account.balance + transaction.amount }
              }
            } else if (transaction.type === 'expense') {
              if (account.id === transaction.accountId) {
                return { ...account, balance: account.balance - transaction.amount }
              }
            } else if (transaction.type === 'income') {
              if (account.id === transaction.accountId) {
                return { ...account, balance: account.balance + transaction.amount }
              }
            }
            return account
          })

          return {
            transactions: [transaction, ...state.transactions],
            accounts: updatedAccounts,
          }
        })
      },

      updateTransaction: (id, updates) => {
        const state = get()
        const originalTx = state.transactions.find((t) => t.id === id)
        if (!originalTx) return

        // Reverse original transaction's effect on balances
        const reversedAccounts = state.accounts.map((account) => {
          if (originalTx.type === 'transfer') {
            if (account.id === originalTx.accountId) {
              return { ...account, balance: account.balance + originalTx.amount }
            }
            if (originalTx.toAccountId && account.id === originalTx.toAccountId) {
              return { ...account, balance: account.balance - originalTx.amount }
            }
          } else if (originalTx.type === 'expense') {
            if (account.id === originalTx.accountId) {
              return { ...account, balance: account.balance + originalTx.amount }
            }
          } else if (originalTx.type === 'income') {
            if (account.id === originalTx.accountId) {
              return { ...account, balance: account.balance - originalTx.amount }
            }
          }
          return account
        })

        const updatedTx = { ...originalTx, ...updates }

        // Apply new transaction's effect
        const finalAccounts = reversedAccounts.map((account) => {
          if (updatedTx.type === 'transfer') {
            if (account.id === updatedTx.accountId) {
              return { ...account, balance: account.balance - updatedTx.amount }
            }
            if (updatedTx.toAccountId && account.id === updatedTx.toAccountId) {
              return { ...account, balance: account.balance + updatedTx.amount }
            }
          } else if (updatedTx.type === 'expense') {
            if (account.id === updatedTx.accountId) {
              return { ...account, balance: account.balance - updatedTx.amount }
            }
          } else if (updatedTx.type === 'income') {
            if (account.id === updatedTx.accountId) {
              return { ...account, balance: account.balance + updatedTx.amount }
            }
          }
          return account
        })

        set({
          transactions: state.transactions.map((t) => (t.id === id ? updatedTx : t)),
          accounts: finalAccounts,
        })
      },

      deleteTransaction: (id) => {
        const state = get()
        const tx = state.transactions.find((t) => t.id === id)
        if (!tx) return

        // Reverse this transaction's effect on balances
        const updatedAccounts = state.accounts.map((account) => {
          if (tx.type === 'transfer') {
            if (account.id === tx.accountId) {
              return { ...account, balance: account.balance + tx.amount }
            }
            if (tx.toAccountId && account.id === tx.toAccountId) {
              return { ...account, balance: account.balance - tx.amount }
            }
          } else if (tx.type === 'expense') {
            if (account.id === tx.accountId) {
              return { ...account, balance: account.balance + tx.amount }
            }
          } else if (tx.type === 'income') {
            if (account.id === tx.accountId) {
              return { ...account, balance: account.balance - tx.amount }
            }
          }
          return account
        })

        set({
          transactions: state.transactions.filter((t) => t.id !== id),
          accounts: updatedAccounts,
        })
      },

      // --------------- Category Actions ---------------
      addCategory: (categoryData) => {
        const category: Category = {
          ...categoryData,
          id: generateUUID(),
        }
        set((state) => ({ categories: [...state.categories, category] }))
      },

      updateCategory: (id, updates) => {
        set((state) => ({
          categories: state.categories.map((c) => (c.id === id ? { ...c, ...updates } : c)),
        }))
      },

      deleteCategory: (id) => {
        set((state) => ({
          categories: state.categories.filter((c) => c.id !== id),
        }))
      },

      // --------------- Label Actions ---------------
      addLabel: (labelData) => {
        const label: Label = {
          ...labelData,
          id: generateUUID(),
        }
        set((state) => ({ labels: [...state.labels, label] }))
      },

      updateLabel: (id, updates) => {
        set((state) => ({
          labels: state.labels.map((l) => (l.id === id ? { ...l, ...updates } : l)),
        }))
      },

      deleteLabel: (id) => {
        set((state) => ({
          labels: state.labels.filter((l) => l.id !== id),
          // Remove this label from all transactions
          transactions: state.transactions.map((t) => ({
            ...t,
            labels: t.labels.filter((lId) => lId !== id),
          })),
        }))
      },

      // --------------- Settings Actions ---------------
      updateSettings: (updates) => {
        set((state) => ({
          settings: { ...state.settings, ...updates },
        }))
      },

      // --------------- Data Actions ---------------
      resetToDefaults: () => {
        set({
          accounts: [],
          transactions: [],
          categories: defaultCategories,
          labels: defaultLabels,
          settings: defaultSettings,
        })
      },

      importData: (data) => {
        set((state) => ({
          accounts: data.accounts ?? state.accounts,
          transactions: data.transactions ?? state.transactions,
          categories: data.categories ?? state.categories,
          labels: data.labels ?? state.labels,
          settings: data.settings ?? state.settings,
        }))
      },
    }),
    {
      name: 'finio-storage',
      storage: createJSONStorage(() => AsyncStorage),
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.setHydrated(true)
        }
      },
    }
  )
)
