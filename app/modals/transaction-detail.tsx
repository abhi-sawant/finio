import React from 'react'
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Alert,
} from 'react-native'
import { useRouter, useLocalSearchParams } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import {
  X,
  Pencil,
  Trash2,
  Calendar,
  CreditCard,
  Tag,
  FileText,
  ArrowDownLeft,
  ArrowUpRight,
  ArrowLeftRight,
} from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { CategoryBadge } from '@/components/categories/CategoryBadge'
import { useFinanceStore } from '@/store/useFinanceStore'
import { formatCurrency, formatFullDate, formatTime } from '@/utils/formatters'
import { showToast } from '@/components/common/Toast'
import { warningHaptic } from '@/utils/haptics'

export default function TransactionDetailModal() {
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { id } = useLocalSearchParams<{ id: string }>()
  const { transactions, accounts, categories, labels, settings, deleteTransaction } =
    useFinanceStore()

  const transaction = transactions.find((t) => t.id === id)

  if (!transaction) {
    return (
      <View style={[styles.container, { paddingTop: insets.top }]}>
        <View style={[styles.header, { paddingTop: insets.top + 16 }]}>
          <TouchableOpacity onPress={() => router.back()}>
            <X size={22} color={Colors.textMuted} />
          </TouchableOpacity>
          <Text style={styles.headerTitle}>Transaction</Text>
          <View style={{ width: 22 }} />
        </View>
        <View style={styles.notFound}>
          <Text style={styles.notFoundText}>Transaction not found</Text>
        </View>
      </View>
    )
  }

  const account = accounts.find((a) => a.id === transaction.accountId)
  const toAccount =
    transaction.toAccountId ? accounts.find((a) => a.id === transaction.toAccountId) : undefined
  const category =
    transaction.categoryId ? categories.find((c) => c.id === transaction.categoryId) : undefined
  const txLabels = labels.filter((l) => transaction.labels?.includes(l.id))

  const typeColor =
    transaction.type === 'income'
      ? Colors.income
      : transaction.type === 'expense'
      ? Colors.expense
      : Colors.transfer

  const TypeIcon =
    transaction.type === 'income'
      ? ArrowUpRight
      : transaction.type === 'expense'
      ? ArrowDownLeft
      : ArrowLeftRight

  const amountPrefix =
    transaction.type === 'income' ? '+' : transaction.type === 'expense' ? '−' : '↔'

  const handleEdit = () => {
    router.replace({ pathname: '/modals/add-transaction', params: { id: transaction.id } })
  }

  const handleDelete = () => {
    warningHaptic()
    Alert.alert(
      'Delete Transaction',
      'Are you sure you want to delete this transaction? This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            deleteTransaction(transaction.id)
            showToast({ message: 'Transaction deleted', type: 'success' })
            router.back()
          },
        },
      ]
    )
  }

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom }]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={Colors.textMuted} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>Transaction Detail</Text>
        <TouchableOpacity onPress={handleEdit} hitSlop={8}>
          <Pencil size={18} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>
        {/* Amount hero */}
        <View style={styles.heroCard}>
          <View style={[styles.typeIcon, { backgroundColor: `${typeColor}22` }]}>
            <TypeIcon size={28} color={typeColor} />
          </View>
          <Text style={styles.txType}>
            {transaction.type.charAt(0).toUpperCase() + transaction.type.slice(1)}
          </Text>
          <Text style={[styles.amount, { color: typeColor }]}>
            {amountPrefix} {formatCurrency(transaction.amount, settings.currency)}
          </Text>
          {transaction.note ? (
            <Text style={styles.note}>{transaction.note}</Text>
          ) : null}
        </View>

        {/* Details list */}
        <View style={styles.detailCard}>
          {/* Date */}
          <View style={styles.detailRow}>
            <View style={styles.detailLeft}>
              <Calendar size={16} color={Colors.textMuted} />
              <Text style={styles.detailLabel}>Date & Time</Text>
            </View>
            <View style={styles.detailRight}>
              <Text style={styles.detailValue}>{formatFullDate(transaction.date)}</Text>
              <Text style={styles.detailSub}>{formatTime(transaction.date)}</Text>
            </View>
          </View>

          <View style={styles.divider} />

          {/* Account */}
          <View style={styles.detailRow}>
            <View style={styles.detailLeft}>
              <CreditCard size={16} color={Colors.textMuted} />
              <Text style={styles.detailLabel}>
                {transaction.type === 'transfer' ? 'From Account' : 'Account'}
              </Text>
            </View>
            <Text style={styles.detailValue}>{account?.name ?? '—'}</Text>
          </View>

          {toAccount && (
            <>
              <View style={styles.divider} />
              <View style={styles.detailRow}>
                <View style={styles.detailLeft}>
                  <CreditCard size={16} color={Colors.textMuted} />
                  <Text style={styles.detailLabel}>To Account</Text>
                </View>
                <Text style={styles.detailValue}>{toAccount.name}</Text>
              </View>
            </>
          )}

          {category && (
            <>
              <View style={styles.divider} />
              <View style={styles.detailRow}>
                <View style={styles.detailLeft}>
                  <Tag size={16} color={Colors.textMuted} />
                  <Text style={styles.detailLabel}>Category</Text>
                </View>
                <CategoryBadge category={category} size="sm" />
              </View>
            </>
          )}

          {txLabels.length > 0 && (
            <>
              <View style={styles.divider} />
              <View style={styles.detailRow}>
                <View style={styles.detailLeft}>
                  <Tag size={16} color={Colors.textMuted} />
                  <Text style={styles.detailLabel}>Labels</Text>
                </View>
                <View style={styles.labelsRow}>
                  {txLabels.map((lbl) => (
                    <View
                      key={lbl.id}
                      style={[styles.labelChip, { backgroundColor: `${lbl.color}22`, borderColor: lbl.color }]}
                    >
                      <Text style={[styles.labelChipText, { color: lbl.color }]}>{lbl.name}</Text>
                    </View>
                  ))}
                </View>
              </View>
            </>
          )}

          {transaction.note ? (
            <>
              <View style={styles.divider} />
              <View style={styles.detailRow}>
                <View style={styles.detailLeft}>
                  <FileText size={16} color={Colors.textMuted} />
                  <Text style={styles.detailLabel}>Note</Text>
                </View>
                <Text style={[styles.detailValue, styles.noteValue]}>{transaction.note}</Text>
              </View>
            </>
          ) : null}
        </View>

        {/* Delete button */}
        <TouchableOpacity style={styles.deleteBtn} onPress={handleDelete}>
          <Trash2 size={16} color={Colors.expense} />
          <Text style={styles.deleteBtnLabel}>Delete Transaction</Text>
        </TouchableOpacity>

        <View style={{ height: 40 }} />
      </ScrollView>
    </View>
  )
}

const styles = StyleSheet.create({
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
  notFound: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  notFoundText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 16,
    color: Colors.textMuted,
  },
  scroll: {
    padding: 16,
    gap: 16,
  },
  heroCard: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    padding: 24,
    alignItems: 'center',
    gap: 8,
  },
  typeIcon: {
    width: 60,
    height: 60,
    borderRadius: 30,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 4,
  },
  txType: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  amount: {
    fontFamily: 'Sora_800ExtraBold',
    fontSize: 36,
  },
  note: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: 'center',
  },
  detailCard: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: 'hidden',
  },
  detailRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 16,
    gap: 12,
  },
  detailLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  detailLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textMuted,
  },
  detailRight: {
    alignItems: 'flex-end',
  },
  detailValue: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: Colors.textPrimary,
    textAlign: 'right',
    flex: 1,
  },
  detailSub: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
  },
  noteValue: {
    fontFamily: 'DMSans_400Regular',
  },
  divider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: Colors.border,
    marginHorizontal: 16,
  },
  labelsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    justifyContent: 'flex-end',
  },
  labelChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 12,
    borderWidth: 1,
  },
  labelChipText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 12,
  },
  deleteBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingVertical: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: `${Colors.expense}44`,
    backgroundColor: `${Colors.expense}11`,
  },
  deleteBtnLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.expense,
  },
})
