import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { defaultCategories, defaultLabels, defaultSettings } from '@/data/defaultData';
import type { Account, Category, FinanceStore, Label, Transaction } from '@/types';

function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

/**
 * Apply or reverse a transaction's effect on account balances.
 * direction = 1  → apply   (addTransaction)
 * direction = -1 → reverse (deleteTransaction / undo for updateTransaction)
 */
function applyBalanceDelta(
  accounts: Account[],
  tx: Pick<Transaction, 'type' | 'accountId' | 'toAccountId' | 'amount'>,
  direction: 1 | -1,
): Account[] {
  return accounts.map((account) => {
    if (tx.type === 'expense' && account.id === tx.accountId) {
      return { ...account, balance: account.balance - direction * tx.amount };
    }
    if (tx.type === 'income' && account.id === tx.accountId) {
      return { ...account, balance: account.balance + direction * tx.amount };
    }
    if (tx.type === 'transfer') {
      if (account.id === tx.accountId) {
        return { ...account, balance: account.balance - direction * tx.amount };
      }
      if (tx.toAccountId && account.id === tx.toAccountId) {
        return { ...account, balance: account.balance + direction * tx.amount };
      }
    }
    return account;
  });
}

const defaultState = {
  accounts: [] as Account[],
  transactions: [] as Transaction[],
  categories: defaultCategories,
  labels: defaultLabels,
  settings: defaultSettings,
  isHydrated: false,
};

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
        };
        set((state) => ({ accounts: [...state.accounts, account] }));
      },

      updateAccount: (id, updates) => {
        set((state) => ({
          accounts: state.accounts.map((a) => (a.id === id ? { ...a, ...updates } : a)),
        }));
      },

      deleteAccount: (id) => {
        set((state) => ({
          accounts: state.accounts.filter((a) => a.id !== id),
          // Cascade delete transactions associated with this account
          transactions: state.transactions.filter(
            (t) => t.accountId !== id && t.toAccountId !== id,
          ),
        }));
      },

      // --------------- Transaction Actions ---------------
      addTransaction: (txData) => {
        const transaction: Transaction = {
          ...txData,
          id: generateUUID(),
          createdAt: new Date().toISOString(),
        };

        set((state) => ({
          transactions: [transaction, ...state.transactions],
          accounts: applyBalanceDelta(state.accounts, transaction, 1),
        }));
      },

      updateTransaction: (id, updates) => {
        const state = get();
        const originalTx = state.transactions.find((t) => t.id === id);
        if (!originalTx) return;

        const updatedTx = { ...originalTx, ...updates };
        const afterReverse = applyBalanceDelta(state.accounts, originalTx, -1);
        const finalAccounts = applyBalanceDelta(afterReverse, updatedTx, 1);

        set({
          transactions: state.transactions.map((t) => (t.id === id ? updatedTx : t)),
          accounts: finalAccounts,
        });
      },

      deleteTransaction: (id) => {
        const state = get();
        const tx = state.transactions.find((t) => t.id === id);
        if (!tx) return;

        set({
          transactions: state.transactions.filter((t) => t.id !== id),
          accounts: applyBalanceDelta(state.accounts, tx, -1),
        });
      },

      // --------------- Category Actions ---------------
      addCategory: (categoryData) => {
        const category: Category = {
          ...categoryData,
          id: generateUUID(),
        };
        set((state) => ({ categories: [...state.categories, category] }));
      },

      updateCategory: (id, updates) => {
        set((state) => ({
          categories: state.categories.map((c) => (c.id === id ? { ...c, ...updates } : c)),
        }));
      },

      deleteCategory: (id) => {
        set((state) => ({
          categories: state.categories.filter((c) => c.id !== id),
        }));
      },

      // --------------- Label Actions ---------------
      addLabel: (labelData) => {
        const label: Label = {
          ...labelData,
          id: generateUUID(),
        };
        set((state) => ({ labels: [...state.labels, label] }));
      },

      updateLabel: (id, updates) => {
        set((state) => ({
          labels: state.labels.map((l) => (l.id === id ? { ...l, ...updates } : l)),
        }));
      },

      deleteLabel: (id) => {
        set((state) => ({
          labels: state.labels.filter((l) => l.id !== id),
          // Remove this label from all transactions
          transactions: state.transactions.map((t) => ({
            ...t,
            labels: t.labels.filter((lId) => lId !== id),
          })),
        }));
      },

      // --------------- Settings Actions ---------------
      updateSettings: (updates) => {
        set((state) => ({
          settings: { ...state.settings, ...updates },
        }));
      },

      // --------------- Data Actions ---------------
      resetToDefaults: () => {
        set({
          accounts: [],
          transactions: [],
          categories: defaultCategories,
          labels: defaultLabels,
          settings: defaultSettings,
        });
      },

      importData: (data) => {
        set((state) => ({
          accounts: data.accounts ?? state.accounts,
          transactions: data.transactions ?? state.transactions,
          categories: data.categories ?? state.categories,
          labels: data.labels ?? state.labels,
          settings: data.settings ?? state.settings,
        }));
      },
    }),
    {
      name: 'finio-storage',
      storage: createJSONStorage(() => AsyncStorage),
      onRehydrateStorage: () => (state) => {
        if (state) {
          state.setHydrated(true);
        }
      },
    },
  ),
);
