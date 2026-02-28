import React, { useState } from 'react'
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Alert,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useRouter } from 'expo-router'
import { Plus } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { AccountCard } from '@/components/accounts/AccountCard'
import { useFinanceStore } from '@/store/useFinanceStore'
import { getTotalBalance } from '@/store/selectors'
import { formatCurrency } from '@/utils/formatters'
import { warningHaptic, lightHaptic } from '@/utils/haptics'
import { showToast } from '@/components/common/Toast'
import type { Account } from '@/types'

export default function AccountsScreen() {
  const colors = useColors()
  const styles = makeStyles(colors)
  const insets = useSafeAreaInsets()
  const router = useRouter()
  const { accounts, settings, deleteAccount } = useFinanceStore()
  const total = getTotalBalance(accounts)

  const handleAccountPress = (account: Account) => {
    lightHaptic()
    router.push({ pathname: '/modals/add-account', params: { id: account.id } })
  }

  const handleAccountLongPress = (account: Account) => {
    warningHaptic()
    Alert.alert(
      account.name,
      'What would you like to do with this account?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Edit',
          onPress: () =>
            router.push({ pathname: '/modals/add-account', params: { id: account.id } }),
        },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            Alert.alert(
              'Delete Account',
              `Delete "${account.name}"? All associated transactions will also be deleted. This cannot be undone.`,
              [
                { text: 'Cancel', style: 'cancel' },
                {
                  text: 'Delete',
                  style: 'destructive',
                  onPress: () => {
                    deleteAccount(account.id)
                    showToast({ message: 'Account deleted', type: 'success' })
                  },
                },
              ]
            )
          },
        },
      ]
    )
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 16 }]}>
        <View>
          <Text style={styles.title}>Accounts</Text>
          <Text style={styles.totalBalance}>
            {formatCurrency(total, settings.currency)} total
          </Text>
        </View>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => router.push('/modals/add-account')}
          hitSlop={8}
        >
          <Plus size={20} color={colors.background} />
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        {/* 2-column grid */}
        <View style={styles.grid}>
          {accounts.map((account) => (
            <View key={account.id} style={styles.gridItem}>
              <AccountCard
                account={account}
                variant="grid"
                onPress={handleAccountPress}
                onLongPress={handleAccountLongPress}
              />
            </View>
          ))}
        </View>

        {accounts.length === 0 && (
          <View style={styles.empty}>
            <Text style={styles.emptyTitle}>No accounts yet</Text>
            <Text style={styles.emptyDesc}>
              Tap the + button to add your first account
            </Text>
          </View>
        )}

        <View style={{ height: 100 }} />
      </ScrollView>
    </View>
  )
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 24,
    color: colors.textPrimary,
  },
  totalBalance: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 13,
    color: colors.textMuted,
    marginTop: 2,
  },
  addBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  scrollContent: {
    padding: 16,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 12,
  },
  gridItem: {
    width: '47.5%',
  },
  empty: {
    alignItems: 'center',
    paddingTop: 80,
    gap: 8,
  },
  emptyTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 18,
    color: colors.textPrimary,
  },
  emptyDesc: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textMuted,
    textAlign: 'center',
  },
})
}
