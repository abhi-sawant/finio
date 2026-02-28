import React, { useCallback, useState } from 'react'
import {
  View,
  Text,
  ScrollView,
  RefreshControl,
  StyleSheet,
  TouchableOpacity,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useRouter } from 'expo-router'
import { getHours } from 'date-fns'
import { BellRing, Settings2 } from 'lucide-react-native'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { SummaryCards } from '@/components/dashboard/SummaryCards'
import { RecentTransactions } from '@/components/dashboard/RecentTransactions'
import { AccountCard } from '@/components/accounts/AccountCard'
import { SpendingDonut } from '@/components/charts/SpendingDonut'
import { Toast } from '@/components/common/Toast'
import { useFinanceStore } from '@/store/useFinanceStore'
import { lightHaptic } from '@/utils/haptics'
import type { Account } from '@/types'

function getGreeting(): string {
  const hour = getHours(new Date())
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function DashboardScreen() {
  const insets = useSafeAreaInsets()
  const router = useRouter()
  const colors = useColors()
  const styles = makeStyles(colors)
  const { accounts, settings } = useFinanceStore()
  const [refreshing, setRefreshing] = useState(false)

  const onRefresh = useCallback(async () => {
    setRefreshing(true)
    await lightHaptic()
    // Slightly delay to show refresh indicator
    setTimeout(() => setRefreshing(false), 500)
  }, [])

  const handleAccountPress = (account: Account) => {
    router.push({ pathname: '/modals/add-account', params: { id: account.id } })
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <View>
          <Text style={styles.greeting}>{getGreeting()},</Text>
          <Text style={styles.username}>{settings.userName} 👋</Text>
        </View>
        <TouchableOpacity
          onPress={() => router.push('/(tabs)/settings')}
          hitSlop={8}
          style={styles.settingsBtn}
        >
          <Settings2 size={22} color={colors.textMuted} />
        </TouchableOpacity>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
          />
        }
        contentContainerStyle={styles.scrollContent}
      >
        {/* Balance summary cards */}
        <SummaryCards />

        {/* Accounts horizontal scroll */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>My Accounts</Text>
            <TouchableOpacity onPress={() => router.push('/(tabs)/accounts')}>
              <Text style={styles.seeAll}>See all</Text>
            </TouchableOpacity>
          </View>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.accountsRow}
          >
            {accounts.map((account) => (
              <View key={account.id} style={styles.accountCardWrapper}>
                <AccountCard
                  account={account}
                  onPress={handleAccountPress}
                  variant="grid"
                />
              </View>
            ))}
          </ScrollView>
        </View>

        {/* Recent Transactions */}
        <RecentTransactions />

        {/* Spending Donut */}
        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { paddingHorizontal: 16, marginBottom: 8 }]}>
            This Month's Spending
          </Text>
          <SpendingDonut compact />
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>

      <Toast />
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
  greeting: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: colors.textMuted,
  },
  username: {
    fontFamily: 'Sora_700Bold',
    fontSize: 22,
    color: colors.textPrimary,
  },
  settingsBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: colors.border,
  },
  scrollContent: {
    gap: 16,
  },
  section: {
    gap: 8,
  },
  sectionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
  },
  sectionTitle: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: colors.textPrimary,
  },
  seeAll: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: colors.primary,
  },
  accountsRow: {
    paddingHorizontal: 16,
    gap: 12,
    paddingBottom: 4,
  },
  accountCardWrapper: {
    width: 160,
  },
})
}
