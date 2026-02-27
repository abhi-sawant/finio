import React, { useCallback } from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { Gesture, GestureDetector } from 'react-native-gesture-handler'
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withTiming,
  runOnJS,
} from 'react-native-reanimated'
import { Pencil, Trash2 } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { LucideIcon } from '@/components/common/IconPicker'
import { formatCurrency, formatTime, hexToRgba } from '@/utils/formatters'
import { getCategoryById } from '@/store/selectors'
import { useFinanceStore } from '@/store/useFinanceStore'
import type { Transaction } from '@/types'

interface TransactionItemProps {
  transaction: Transaction
  onPress: (transaction: Transaction) => void
  onEdit: (transaction: Transaction) => void
  onDelete: (transaction: Transaction) => void
  currency?: string
}

const SWIPE_THRESHOLD = 60
const ACTION_WIDTH = 80

export function TransactionItem({
  transaction,
  onPress,
  onEdit,
  onDelete,
  currency = 'INR',
}: TransactionItemProps) {
  const { categories, labels: allLabels, settings } = useFinanceStore()
  const category = getCategoryById(categories, transaction.categoryId)
  const txLabels = allLabels.filter((l) => transaction.labels.includes(l.id))

  const translateX = useSharedValue(0)
  const startX = useSharedValue(0)

  const pan = Gesture.Pan()
    .activeOffsetX([-10, 10])
    .onStart(() => {
      startX.value = translateX.value
    })
    .onUpdate((e) => {
      const newX = startX.value + e.translationX
      translateX.value = Math.max(-ACTION_WIDTH, Math.min(ACTION_WIDTH, newX))
    })
    .onEnd((e) => {
      if (e.translationX > SWIPE_THRESHOLD) {
        translateX.value = withSpring(ACTION_WIDTH, { damping: 20, stiffness: 200 })
      } else if (e.translationX < -SWIPE_THRESHOLD) {
        translateX.value = withSpring(-ACTION_WIDTH, { damping: 20, stiffness: 200 })
      } else {
        translateX.value = withSpring(0, { damping: 20, stiffness: 200 })
      }
    })

  const rowStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: translateX.value }],
  }))

  const amountColor =
    transaction.type === 'income'
      ? Colors.income
      : transaction.type === 'expense'
      ? Colors.expense
      : Colors.transfer

  const amountPrefix =
    transaction.type === 'income' ? '+' : transaction.type === 'expense' ? '-' : '↔'

  const handleEdit = useCallback(() => {
    translateX.value = withSpring(0)
    onEdit(transaction)
  }, [transaction])

  const handleDelete = useCallback(() => {
    translateX.value = withSpring(0)
    onDelete(transaction)
  }, [transaction])

  return (
    <View style={styles.container}>
      {/* Edit action (left) */}
      <TouchableOpacity style={styles.editAction} onPress={handleEdit} activeOpacity={0.8}>
        <Pencil size={20} color="#fff" />
        <Text style={styles.actionText}>Edit</Text>
      </TouchableOpacity>

      {/* Delete action (right) */}
      <TouchableOpacity style={styles.deleteAction} onPress={handleDelete} activeOpacity={0.8}>
        <Trash2 size={20} color="#fff" />
        <Text style={styles.actionText}>Delete</Text>
      </TouchableOpacity>

      <GestureDetector gesture={pan}>
        <Animated.View style={[styles.row, rowStyle]}>
          <TouchableOpacity
            onPress={() => onPress(transaction)}
            style={styles.rowInner}
            activeOpacity={0.8}
          >
            {/* Category icon */}
            {category ? (
              <View
                style={[
                  styles.iconCircle,
                  { backgroundColor: hexToRgba(category.color, 0.2) },
                ]}
              >
                <LucideIcon name={category.icon} size={18} color={category.color} />
              </View>
            ) : (
              <View style={[styles.iconCircle, { backgroundColor: Colors.surfaceElevated }]}>
                <LucideIcon name="circle-ellipsis" size={18} color={Colors.textMuted} />
              </View>
            )}

            {/* Info */}
            <View style={styles.info}>
              <Text style={styles.note} numberOfLines={1}>
                {transaction.note || category?.name || 'Transaction'}
              </Text>
              <View style={styles.metaRow}>
                <Text style={styles.time}>{formatTime(transaction.date)}</Text>
                {txLabels.length > 0 && (
                  <View style={styles.labelsRow}>
                    {txLabels.slice(0, 2).map((l) => (
                      <View key={l.id} style={[styles.labelDot, { backgroundColor: l.color }]} />
                    ))}
                  </View>
                )}
              </View>
            </View>

            {/* Amount */}
            <Text style={[styles.amount, { color: amountColor }]}>
              {amountPrefix}
              {formatCurrency(transaction.amount, settings.currency as Parameters<typeof formatCurrency>[1])}
            </Text>
          </TouchableOpacity>
        </Animated.View>
      </GestureDetector>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    position: 'relative',
    marginVertical: 1,
  },
  editAction: {
    position: 'absolute',
    left: 0,
    top: 0,
    bottom: 0,
    width: ACTION_WIDTH,
    backgroundColor: Colors.transfer,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    borderRadius: 0,
  },
  deleteAction: {
    position: 'absolute',
    right: 0,
    top: 0,
    bottom: 0,
    width: ACTION_WIDTH,
    backgroundColor: Colors.expense,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
  },
  actionText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 11,
    color: '#fff',
  },
  row: {
    backgroundColor: Colors.background,
  },
  rowInner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 12,
    paddingHorizontal: 16,
    backgroundColor: Colors.background,
  },
  iconCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  info: {
    flex: 1,
    gap: 4,
  },
  note: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: Colors.textPrimary,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  time: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
  },
  labelsRow: {
    flexDirection: 'row',
    gap: 4,
    alignItems: 'center',
  },
  labelDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  amount: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 14,
  },
})
