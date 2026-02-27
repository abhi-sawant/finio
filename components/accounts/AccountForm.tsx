import React, { useState } from 'react'
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
} from 'react-native'
import { useForm, Controller } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Colors } from '@/constants/Colors'
import { ColorPicker } from '@/components/common/ColorPicker'
import { IconPicker, LucideIcon } from '@/components/common/IconPicker'
import { AmountInput } from '@/components/common/AmountInput'
import { showToast } from '@/components/common/Toast'
import { successHaptic } from '@/utils/haptics'
import { useFinanceStore } from '@/store/useFinanceStore'
import type { Account, AccountType } from '@/types'

const ACCOUNT_TYPES: Array<{ value: AccountType; label: string }> = [
  { value: 'checking', label: 'Checking' },
  { value: 'savings', label: 'Savings' },
  { value: 'cash', label: 'Cash' },
  { value: 'credit', label: 'Credit' },
  { value: 'investment', label: 'Investment' },
  { value: 'wallet', label: 'Wallet' },
]

const schema = z.object({
  name: z.string().min(1, 'Name is required').max(30),
  type: z.enum(['checking', 'savings', 'cash', 'credit', 'investment', 'wallet']),
  color: z.string().min(1),
  icon: z.string().min(1),
  balance: z.number().min(0),
})

type FormData = z.infer<typeof schema>

interface AccountFormProps {
  initialData?: Partial<Account>
  onSubmit: (data: Omit<Account, 'id' | 'createdAt'>) => void
}

export function AccountForm({ initialData, onSubmit }: AccountFormProps) {
  const [showIconPicker, setShowIconPicker] = useState(false)
  const currency = useFinanceStore((s) => s.settings.currency)

  const { control, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: initialData?.name ?? '',
      type: initialData?.type ?? 'checking',
      color: initialData?.color ?? '#6C63FF',
      icon: initialData?.icon ?? 'landmark',
      balance: initialData?.balance ?? 0,
    },
  })

  const selectedColor = watch('color')
  const selectedIcon = watch('icon')

  const handleFormSubmit = async (data: FormData) => {
    await successHaptic()
    onSubmit({ ...data, currency })
    showToast({ message: 'Account saved!', type: 'success' })
  }

  return (
    <View style={styles.container}>
        {/* Icon + Color row */}
        <View style={styles.iconColorRow}>
          <TouchableOpacity
            onPress={() => setShowIconPicker(true)}
            style={[styles.iconPreview, { backgroundColor: selectedColor + '33', borderColor: selectedColor }]}
          >
            <LucideIcon name={selectedIcon} size={28} color={selectedColor} />
          </TouchableOpacity>
          <Controller
            control={control}
            name="color"
            render={({ field }) => (
              <ColorPicker selectedColor={field.value} onChange={field.onChange} />
            )}
          />
        </View>

        {/* Name */}
        <View style={styles.field}>
          <Text style={styles.label}>Account Name</Text>
          <Controller
            control={control}
            name="name"
            render={({ field }) => (
              <TextInput
                value={field.value}
                onChangeText={field.onChange}
                style={[styles.input, errors.name && styles.inputError]}
                placeholder="e.g. HDFC Savings"
                placeholderTextColor={Colors.textMuted}
                selectionColor={Colors.primary}
              />
            )}
          />
          {errors.name && <Text style={styles.errorText}>{errors.name.message}</Text>}
        </View>

        {/* Account Type */}
        <View style={styles.field}>
          <Text style={styles.label}>Account Type</Text>
          <Controller
            control={control}
            name="type"
            render={({ field }) => (
              <View style={styles.chipRow}>
                {ACCOUNT_TYPES.map((t) => (
                  <TouchableOpacity
                    key={t.value}
                    onPress={() => field.onChange(t.value)}
                    style={[
                      styles.chip,
                      field.value === t.value && { backgroundColor: selectedColor + '33', borderColor: selectedColor },
                    ]}
                  >
                    <Text
                      style={[
                        styles.chipText,
                        field.value === t.value && { color: selectedColor },
                      ]}
                    >
                      {t.label}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            )}
          />
        </View>

        {/* Opening Balance */}
        <View style={styles.field}>
          <Text style={styles.label}>Opening Balance</Text>
          <Controller
            control={control}
            name="balance"
            render={({ field }) => (
              <View style={styles.amountWrapper}>
                <AmountInput
                  value={field.value}
                  onChange={field.onChange}
                  currency={currency}
                />
              </View>
            )}
          />
        </View>

        {/* Submit */}
        <TouchableOpacity
          style={[styles.submitBtn, { backgroundColor: selectedColor }]}
          onPress={handleSubmit(handleFormSubmit)}
          activeOpacity={0.85}
        >
          <Text style={styles.submitText}>
            {initialData?.id ? 'Update Account' : 'Create Account'}
          </Text>
        </TouchableOpacity>

      <Controller
        control={control}
        name="icon"
        render={({ field }) => (
          <IconPicker
            visible={showIconPicker}
            onClose={() => setShowIconPicker(false)}
            selectedIcon={field.value}
            onChange={field.onChange}
            accentColor={selectedColor}
          />
        )}
      />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    gap: 20,
  },
  iconColorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
  iconPreview: {
    width: 64,
    height: 64,
    borderRadius: 20,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  field: {
    gap: 8,
  },
  label: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  input: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 14,
    padding: 14,
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.textPrimary,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  inputError: {
    borderColor: Colors.expense,
  },
  errorText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.expense,
  },
  chipRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.surfaceElevated,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  chipText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
  },
  amountWrapper: {
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.border,
    overflow: 'hidden',
  },
  submitBtn: {
    borderRadius: 16,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  submitText: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 16,
    color: '#fff',
  },
})
