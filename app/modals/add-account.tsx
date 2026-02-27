import React from 'react'
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
} from 'react-native'
import { useRouter, useLocalSearchParams } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { AccountForm } from '@/components/accounts/AccountForm'
import { useFinanceStore } from '@/store/useFinanceStore'
import { showToast } from '@/components/common/Toast'
import { successHaptic } from '@/utils/haptics'
import type { Account } from '@/types'

export default function AddAccountModal() {
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { id } = useLocalSearchParams<{ id?: string }>()
  const { accounts, addAccount, updateAccount } = useFinanceStore()

  const existing = id ? accounts.find((a) => a.id === id) : undefined
  const isEdit = !!existing

  const handleSubmit = (data: Omit<Account, 'id' | 'createdAt'>) => {
    if (isEdit && existing) {
      updateAccount(existing.id, data)
      showToast({ message: 'Account updated', type: 'success' })
    } else {
      addAccount(data)
      showToast({ message: 'Account added', type: 'success' })
    }
    successHaptic()
    router.back()
  }

  return (
    <View style={[styles.container, { paddingBottom: insets.bottom }]}>
      {/* Header */}
      <View style={[styles.header, { paddingTop: insets.top + 16 }]}>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={Colors.textMuted} />
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{isEdit ? 'Edit Account' : 'New Account'}</Text>
        <View style={{ width: 22 }} />
      </View>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.flex}
      >
        <ScrollView
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          contentContainerStyle={styles.scroll}
        >
          <AccountForm {...(existing && { initialData: existing })} onSubmit={handleSubmit} />
          <View style={{ height: 40 }} />
        </ScrollView>
      </KeyboardAvoidingView>
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
  scroll: {
    padding: 16,
  },
})
