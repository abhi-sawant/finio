import React, { useCallback, useMemo } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Gesture, GestureDetector } from 'react-native-gesture-handler';
import Animated, { useSharedValue, useAnimatedStyle, withSpring } from 'react-native-reanimated';
import { Pencil, Trash2 } from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { LucideIcon } from '@/components/common/IconPicker';
import { formatCurrency, formatTime, hexToRgba } from '@/utils/formatters';
import { getCategoryById } from '@/store/selectors';
import type { Transaction, Category, Label } from '@/types';

interface TransactionItemProps {
  transaction: Transaction;
  onPress: (transaction: Transaction) => void;
  onEdit: (transaction: Transaction) => void;
  onDelete: (transaction: Transaction) => void;
  /** Passed from parent — avoids per-item store subscriptions */
  categories: Category[];
  labels: Label[];
  currency: string;
  /** Pre-computed closing balance passed from TransactionList (avoids O(n) work per item) */
  closingBalance?: number;
  /** Resolved account name to display alongside the closing balance */
  accountName?: string;
}

const SWIPE_THRESHOLD = 60;
const ACTION_WIDTH = 80;

export const TransactionItem = React.memo(function TransactionItem({
  transaction,
  onPress,
  onEdit,
  onDelete,
  categories,
  labels,
  currency,
  closingBalance,
  accountName,
}: TransactionItemProps) {
  const colors = useColors();
  const styles = useMemo(() => makeStyles(colors), [colors]);

  const category = useMemo(
    () => getCategoryById(categories, transaction.categoryId),
    [categories, transaction.categoryId],
  );

  const txLabels = useMemo(
    () => labels.filter((l) => transaction.labels.includes(l.id)),
    [labels, transaction.labels],
  );

  const translateX = useSharedValue(0);
  const startX = useSharedValue(0);

  // Memoize the gesture object so GestureDetector doesn't reinstall it every render
  const pan = useMemo(
    () =>
      Gesture.Pan()
        .activeOffsetX([-10, 10])
        .onStart(() => {
          startX.value = translateX.value;
        })
        .onUpdate((e) => {
          const newX = startX.value + e.translationX;
          translateX.value = Math.max(-ACTION_WIDTH, Math.min(ACTION_WIDTH, newX));
        })
        .onEnd((e) => {
          if (e.translationX > SWIPE_THRESHOLD) {
            translateX.value = withSpring(ACTION_WIDTH, { damping: 20, stiffness: 200 });
          } else if (e.translationX < -SWIPE_THRESHOLD) {
            translateX.value = withSpring(-ACTION_WIDTH, { damping: 20, stiffness: 200 });
          } else {
            translateX.value = withSpring(0, { damping: 20, stiffness: 200 });
          }
        }),
    // shared values are stable references — empty deps is intentional
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  const rowStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: translateX.value }],
  }));

  const amountColor =
    transaction.type === 'income'
      ? colors.income
      : transaction.type === 'expense'
        ? colors.expense
        : colors.transfer;

  const amountPrefix =
    transaction.type === 'income' ? '+' : transaction.type === 'expense' ? '-' : '↔';

  const handleEdit = useCallback(() => {
    translateX.value = withSpring(0);
    onEdit(transaction);
  }, [transaction, onEdit]);

  const handleDelete = useCallback(() => {
    translateX.value = withSpring(0);
    onDelete(transaction);
  }, [transaction, onDelete]);

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
                style={[styles.iconCircle, { backgroundColor: hexToRgba(category.color, 0.2) }]}
              >
                <LucideIcon name={category.icon} size={18} color={category.color} />
              </View>
            ) : (
              <View style={[styles.iconCircle, { backgroundColor: colors.surfaceElevated }]}>
                <LucideIcon name="circle-ellipsis" size={18} color={colors.textMuted} />
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
            <View style={styles.amountContainer}>
              <Text style={[styles.amount, { color: amountColor }]}>
                {amountPrefix}
                {formatCurrency(transaction.amount, currency)}
              </Text>
              {accountName !== undefined && closingBalance !== undefined && (
                <Text style={styles.closingBalance}>
                  {accountName}: {formatCurrency(closingBalance, currency)}
                </Text>
              )}
            </View>
          </TouchableOpacity>
        </Animated.View>
      </GestureDetector>
    </View>
  );
});

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
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
      backgroundColor: colors.transfer,
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
      backgroundColor: colors.expense,
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
      backgroundColor: colors.background,
    },
    rowInner: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 12,
      paddingVertical: 12,
      paddingHorizontal: 16,
      backgroundColor: colors.background,
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
      color: colors.textPrimary,
    },
    metaRow: {
      flexDirection: 'row',
      alignItems: 'center',
      gap: 8,
    },
    time: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 12,
      color: colors.textMuted,
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
    amountContainer: {
      alignItems: 'flex-end',
      gap: 2,
    },
    amount: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 14,
    },
    closingBalance: {
      fontFamily: 'DMSans_400Regular',
      fontSize: 11,
      color: colors.textMuted,
    },
  });
}
