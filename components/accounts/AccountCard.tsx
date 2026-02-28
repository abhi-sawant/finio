import React from 'react'
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { LucideIcon } from '@/components/common/IconPicker'
import { formatCurrency, hexToRgba } from '@/utils/formatters'
import type { Account } from '@/types'

const ACCOUNT_TYPE_LABELS: Record<Account['type'], string> = {
  checking: 'Checking',
  savings: 'Savings',
  cash: 'Cash',
  credit: 'Credit',
  investment: 'Investment',
  wallet: 'Wallet',
}

interface AccountCardProps {
  account: Account
  onPress?: (account: Account) => void
  onLongPress?: (account: Account) => void
  variant?: 'grid' | 'horizontal'
}

export function AccountCard({ account, onPress, onLongPress, variant = 'grid' }: AccountCardProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  if (variant === 'horizontal') {
    return (
      <TouchableOpacity
        onPress={() => onPress?.(account)}
        onLongPress={() => onLongPress?.(account)}
        activeOpacity={0.8}
        style={[
          styles.horizontal,
          { borderLeftColor: account.color, borderLeftWidth: 4 },
        ]}
      >
        <View
          style={[
            styles.iconCircle,
            { backgroundColor: hexToRgba(account.color, 0.2) },
          ]}
        >
          <LucideIcon name={account.icon} size={18} color={account.color} />
        </View>
        <View style={styles.horizontalInfo}>
          <Text style={styles.accountName} numberOfLines={1}>{account.name}</Text>
          <Text style={styles.accountType}>{ACCOUNT_TYPE_LABELS[account.type]}</Text>
        </View>
        <Text style={styles.horizontalBalance}>
          {formatCurrency(account.balance, account.currency, true)}
        </Text>
      </TouchableOpacity>
    )
  }

  return (
    <TouchableOpacity
      onPress={() => onPress?.(account)}
      onLongPress={() => onLongPress?.(account)}
      activeOpacity={0.8}
      style={[styles.card, { borderColor: hexToRgba(account.color, 0.3) }]}
    >
      <View style={[styles.cardTop]}>
        <View
          style={[
            styles.iconCircle,
            { backgroundColor: hexToRgba(account.color, 0.2) },
          ]}
        >
          <LucideIcon name={account.icon} size={22} color={account.color} />
        </View>
        <View
          style={[styles.typeBadge, { backgroundColor: hexToRgba(account.color, 0.15) }]}
        >
          <Text style={[styles.typeBadgeText, { color: account.color }]}>
            {ACCOUNT_TYPE_LABELS[account.type]}
          </Text>
        </View>
      </View>

      <Text style={styles.cardName} numberOfLines={2}>{account.name}</Text>
      <Text style={[styles.cardBalance, { color: account.color }]}>
        {formatCurrency(account.balance, account.currency, true)}
      </Text>
    </TouchableOpacity>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  // Grid variant
  card: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    padding: 16,
    borderWidth: 1,
    gap: 8,
    flex: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  cardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 4,
  },
  cardName: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: colors.textPrimary,
  },
  cardBalance: {
    fontFamily: 'Sora_700Bold',
    fontSize: 18,
  },
  typeBadge: {
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  typeBadgeText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 10,
  },

  // Horizontal variant
  horizontal: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: colors.surface,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.border,
  },
  horizontalInfo: {
    flex: 1,
  },
  accountName: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: colors.textPrimary,
  },
  accountType: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  horizontalBalance: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: colors.textPrimary,
  },

  // Shared
  iconCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
})
}
