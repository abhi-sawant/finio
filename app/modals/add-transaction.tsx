import React, { useState } from 'react'
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native'
import { useRouter, useLocalSearchParams } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X, ArrowDownLeft, ArrowUpRight, ArrowLeftRight, Check, Tag } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { AmountInput } from '@/components/common/AmountInput'
import { DatePicker } from '@/components/common/DatePicker'
import { CategoryPicker } from '@/components/categories/CategoryPicker'
import { LabelPicker } from '@/components/common/LabelPicker'
import { useFinanceStore } from '@/store/useFinanceStore'
import { showToast } from '@/components/common/Toast'
import { successHaptic, errorHaptic, lightHaptic } from '@/utils/haptics'
import type { Category, TransactionType } from '@/types'

export default function AddTransactionModal() {
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { id } = useLocalSearchParams<{ id?: string }>()
  const { transactions, accounts, categories, settings, addTransaction, updateTransaction } =
    useFinanceStore()

  const existing = id ? transactions.find((t) => t.id === id) : undefined
  const isEdit = !!existing

  // Form state
  const [type, setType] = useState<TransactionType>(existing?.type ?? 'expense')
  const [amount, setAmount] = useState(existing?.amount ?? 0)
  const [accountId, setAccountId] = useState(
    existing?.accountId ?? (accounts[0]?.id ?? '')
  )
  const [toAccountId, setToAccountId] = useState(existing?.toAccountId ?? '')
  const [categoryId, setCategoryId] = useState(existing?.categoryId ?? '')
  const [labelIds, setLabelIds] = useState<string[]>(existing?.labels ?? [])
  const [note, setNote] = useState(existing?.note ?? '')
  const [date, setDate] = useState(existing ? new Date(existing.date) : new Date())
  const [categoryPickerVisible, setCategoryPickerVisible] = useState(false)
  const [labelPickerVisible, setLabelPickerVisible] = useState(false)

  const selectedCategory = categories.find((c) => c.id === categoryId)

  const handleCategoryChange = (cat: Category) => {
    setCategoryId(cat.id)
    setCategoryPickerVisible(false)
  }

  const handleSave = () => {
    if (amount <= 0) {
      errorHaptic()
      showToast({ message: 'Enter an amount greater than 0', type: 'error' })
      return
    }
    if (!accountId) {
      errorHaptic()
      showToast({ message: 'Select an account', type: 'error' })
      return
    }
    if (type === 'transfer' && (!toAccountId || toAccountId === accountId)) {
      errorHaptic()
      showToast({ message: 'Select a different destination account', type: 'error' })
      return
    }

    if (isEdit && existing) {
      updateTransaction(existing.id, {
        type,
        amount,
        accountId,
        ...(type === 'transfer' ? { toAccountId } : {}),
        categoryId: type !== 'transfer' ? categoryId : existing.categoryId,
        labels: labelIds,
        note: note.trim(),
        date: date.toISOString(),
      })
      showToast({ message: 'Transaction updated', type: 'success' })
    } else {
      addTransaction({
        type,
        amount,
        accountId,
        ...(type === 'transfer' ? { toAccountId } : {}),
        categoryId: type !== 'transfer' ? categoryId : '',
        labels: labelIds,
        note: note.trim(),
        date: date.toISOString(),
      })
      showToast({ message: 'Transaction added', type: 'success' })
    }

    successHaptic()
    router.back()
  }

  const TYPE_TABS: { key: TransactionType; label: string; icon: React.ReactNode; color: string }[] = [
    {
      key: 'expense',
      label: 'Expense',
      icon: <ArrowDownLeft size={16} color={type === 'expense' ? '#fff' : Colors.expense} />,
      color: Colors.expense,
    },
    {
      key: 'income',
      label: 'Income',
      icon: <ArrowUpRight size={16} color={type === 'income' ? '#fff' : Colors.income} />,
      color: Colors.income,
    },
    {
      key: 'transfer',
      label: 'Transfer',
      icon: <ArrowLeftRight size={16} color={type === 'transfer' ? '#fff' : Colors.transfer} />,
      color: Colors.transfer,
    },
  ]

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 16 }]}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={Colors.textMuted} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{isEdit ? 'Edit Transaction' : 'New Transaction'}</Text>
        <TouchableOpacity onPress={handleSave} hitSlop={8}>
          <Check size={22} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      {/* Type tabs */}
      <View style={styles.typeTabs}>
        {TYPE_TABS.map((tab) => (
          <TouchableOpacity
            key={tab.key}
            onPress={() => {
              lightHaptic()
              setType(tab.key)
            }}
            style={[
              styles.typeTab,
              type === tab.key && { backgroundColor: tab.color, borderColor: tab.color },
            ]}
          >
            {tab.icon}
            <Text
              style={[
                styles.typeTabLabel,
                type === tab.key && styles.typeTabLabelActive,
              ]}
            >
              {tab.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          contentContainerStyle={styles.scroll}
          showsVerticalScrollIndicator={false}
        >
          {/* Amount */}
          <View style={styles.amountContainer}>
            <AmountInput value={amount} onChange={setAmount} currency={settings.currency} />
          </View>

          {/* Date */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Date</Text>
            <DatePicker value={date} onChange={setDate} />
          </View>

          {/* Account */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>
              {type === 'transfer' ? 'From Account' : 'Account'}
            </Text>
            <View style={styles.accountRow}>
              {accounts.map((acc) => (
                <TouchableOpacity
                  key={acc.id}
                  onPress={() => {
                    lightHaptic()
                    setAccountId(acc.id)
                  }}
                  style={[
                    styles.accountChip,
                    accountId === acc.id && styles.accountChipActive,
                  ]}
                >
                  <Text
                    style={[
                      styles.accountChipLabel,
                      accountId === acc.id && styles.accountChipLabelActive,
                    ]}
                  >
                    {acc.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>

          {/* To Account (transfer only) */}
          {type === 'transfer' && (
            <View style={styles.field}>
              <Text style={styles.fieldLabel}>To Account</Text>
              <View style={styles.accountRow}>
                {accounts
                  .filter((acc) => acc.id !== accountId)
                  .map((acc) => (
                    <TouchableOpacity
                      key={acc.id}
                      onPress={() => {
                        lightHaptic()
                        setToAccountId(acc.id)
                      }}
                      style={[
                        styles.accountChip,
                        toAccountId === acc.id && styles.accountChipActive,
                      ]}
                    >
                      <Text
                        style={[
                          styles.accountChipLabel,
                          toAccountId === acc.id && styles.accountChipLabelActive,
                        ]}
                      >
                        {acc.name}
                      </Text>
                    </TouchableOpacity>
                  ))}
              </View>
            </View>
          )}

          {/* Category (not transfer) */}
          {type !== 'transfer' && (
            <View style={styles.field}>
              <Text style={styles.fieldLabel}>Category</Text>
              <TouchableOpacity
                style={styles.pickerBtn}
                onPress={() => {
                  lightHaptic()
                  setCategoryPickerVisible(true)
                }}
              >
                <Text style={styles.pickerBtnLabel}>
                  {selectedCategory ? selectedCategory.name : 'Select category'}
                </Text>
              </TouchableOpacity>
              <CategoryPicker
                visible={categoryPickerVisible}
                onClose={() => setCategoryPickerVisible(false)}
                selectedId={categoryId}
                onChange={handleCategoryChange}
                transactionType={type}
              />
            </View>
          )}

          {/* Labels */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Labels</Text>
            <TouchableOpacity
              style={styles.pickerBtn}
              onPress={() => {
                lightHaptic()
                setLabelPickerVisible(true)
              }}
            >
              <Tag size={14} color={Colors.textMuted} />
              <Text style={styles.pickerBtnLabel}>
                {labelIds.length > 0
                  ? `${labelIds.length} label${labelIds.length !== 1 ? 's' : ''} selected`
                  : 'Select labels'}
              </Text>
            </TouchableOpacity>
            <LabelPicker
              selectedIds={labelIds}
              onChange={setLabelIds}
              visible={labelPickerVisible}
              onClose={() => setLabelPickerVisible(false)}
            />
          </View>

          {/* Note */}
          <View style={styles.field}>
            <Text style={styles.fieldLabel}>Note</Text>
            <TextInput
              style={styles.noteInput}
              value={note}
              onChangeText={setNote}
              placeholder="Add a note…"
              placeholderTextColor={Colors.textMuted}
              multiline
              maxLength={200}
            />
          </View>

          <View style={{ height: 40 }} />
        </ScrollView>
      </KeyboardAvoidingView>

      {/* Save button */}
      <TouchableOpacity
        style={[styles.saveBtn, { marginBottom: insets.bottom || 16 }]}
        onPress={handleSave}
      >
        <Text style={styles.saveBtnLabel}>
          {isEdit ? 'Update Transaction' : 'Add Transaction'}
        </Text>
      </TouchableOpacity>
    </View>
  )
}

const styles = StyleSheet.create({
  flex: { flex: 1 },
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingTop: 16,
    paddingBottom: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: Colors.border,
  },
  headerTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 17,
    color: Colors.textPrimary,
  },
  typeTabs: {
    flexDirection: 'row',
    padding: 16,
    gap: 8,
  },
  typeTab: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  typeTabLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  typeTabLabelActive: {
    color: '#fff',
  },
  amountContainer: {
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  scroll: {
    gap: 4,
  },
  field: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 8,
  },
  fieldLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  accountRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  accountChip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  accountChipActive: {
    borderColor: Colors.primary,
    backgroundColor: `${Colors.primary}22`,
  },
  accountChipLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  accountChipLabelActive: {
    color: Colors.primary,
  },
  pickerBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  pickerBtnLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: Colors.textPrimary,
    flex: 1,
  },
  noteInput: {
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: Colors.textPrimary,
    minHeight: 80,
    textAlignVertical: 'top',
  },
  saveBtn: {
    marginHorizontal: 16,
    backgroundColor: Colors.primary,
    borderRadius: 16,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  saveBtnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: '#fff',
  },
})
