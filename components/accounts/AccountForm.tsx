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
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
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
  creditLimit: z.number().min(0).optional(),
})

type FormData = z.infer<typeof schema>

interface AccountFormProps {
  initialData?: Partial<Account>
  onSubmit: (data: Omit<Account, 'id' | 'createdAt'>) => void
}

export function AccountForm({ initialData, onSubmit }: AccountFormProps) {
  const colors = useColors()
  const styles = makeStyles(colors)
  const [showIconPicker, setShowIconPicker] = useState(false)
  const currency = useFinanceStore((s) => s.settings.currency)

  const { control, handleSubmit, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: initialData?.name ?? '',
      type: initialData?.type ?? 'checking',
      color: initialData?.color ?? '#6C63FF',
      icon: initialData?.icon ?? 'landmark',
      // For credit accounts the stored balance is negative (debt); show absolute value in the form
      balance: initialData?.type === 'credit'
        ? Math.abs(initialData?.balance ?? 0)
        : (initialData?.balance ?? 0),
      creditLimit: initialData?.creditLimit,
    },
  })

  const selectedColor = watch('color')
  const selectedIcon = watch('icon')
  const selectedType = watch('type')
  const isCredit = selectedType === 'credit'

  const handleFormSubmit = async (data: FormData) => {
    await successHaptic()
    // Credit balances are stored as negative numbers (debt) internally
    const outputBalance = data.type === 'credit' ? -data.balance : data.balance
    onSubmit({
      name: data.name,
      type: data.type,
      color: data.color,
      icon: data.icon,
      balance: outputBalance,
      currency,
      ...(data.type === 'credit' && data.creditLimit !== undefined
        ? { creditLimit: data.creditLimit }
        : {}),
    })
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
                placeholder="e.g. HDFC Credit Card"
                placeholderTextColor={colors.textMuted}
                selectionColor={colors.primary}
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

        {/* Opening / Outstanding Balance */}
        <View style={styles.field}>
          <Text style={styles.label}>
            {isCredit ? 'Current Outstanding' : 'Opening Balance'}
          </Text>
          {isCredit && (
            <Text style={styles.fieldHint}>
              How much do you currently owe on this card? Enter 0 if fully paid.
            </Text>
          )}
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

        {/* ── Credit-card only fields ── */}
        {isCredit && (
          <>
            {/* Credit Limit */}
            <View style={styles.field}>
              <Text style={styles.label}>Credit Limit</Text>
              <Text style={styles.fieldHint}>
                Your card's total credit limit (optional but recommended for utilization tracking).
              </Text>
              <Controller
                control={control}
                name="creditLimit"
                render={({ field }) => (
                  <View style={styles.amountWrapper}>
                    <AmountInput
                      value={field.value ?? 0}
                      onChange={field.onChange}
                      currency={currency}
                    />
                  </View>
                )}
              />
            </View>


          </>
        )}

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

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
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
    color: colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  fieldHint: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.textMuted,
    lineHeight: 17,
    marginTop: -4,
  },
  input: {
    backgroundColor: colors.surfaceElevated,
    borderRadius: 14,
    padding: 14,
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: colors.textPrimary,
    borderWidth: 1,
    borderColor: colors.border,
  },
  inputError: {
    borderColor: colors.expense,
  },
  errorText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.expense,
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
    backgroundColor: colors.surfaceElevated,
    borderWidth: 1,
    borderColor: colors.border,
  },
  chipText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: colors.textMuted,
  },
  amountWrapper: {
    backgroundColor: colors.surfaceElevated,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
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
}
