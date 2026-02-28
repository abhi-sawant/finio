import React, { useState, useRef } from 'react'
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
} from 'react-native'
import { useRouter, useLocalSearchParams } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { ArrowLeft } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { api } from '@/services/api'
import { useAuthStore } from '@/store/useAuthStore'
import { showToast } from '@/components/common/Toast'

export default function VerifyOtpScreen() {
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { email } = useLocalSearchParams<{ email: string }>()
  const { setAuth } = useAuthStore()

  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const inputs = useRef<(TextInput | null)[]>([])

  const otpValue = otp.join('')

  const handleChange = (value: string, index: number) => {
    // Accept only digits
    const digit = value.replace(/\D/g, '').slice(-1)
    const next = [...otp]
    next[index] = digit
    setOtp(next)

    if (digit && index < 5) {
      inputs.current[index + 1]?.focus()
    }
  }

  const handleKeyPress = (key: string, index: number) => {
    if (key === 'Backspace' && !otp[index] && index > 0) {
      inputs.current[index - 1]?.focus()
    }
  }

  const handleVerify = async () => {
    if (otpValue.length < 6) {
      showToast({ message: 'Enter the full 6-digit OTP', type: 'error' })
      return
    }
    setLoading(true)
    try {
      const res = await api.verifyOtp(email, otpValue)
      await setAuth(res.token, res.user)
      showToast({ message: 'Email verified! You\'re signed in.', type: 'success' })
      router.dismissAll()
    } catch (err: unknown) {
      showToast({
        message: err instanceof Error ? err.message : 'Verification failed',
        type: 'error',
      })
    } finally {
      setLoading(false)
    }
  }

  const handleResend = async () => {
    setResending(true)
    try {
      await api.resendOtp(email)
      showToast({ message: 'A new OTP has been sent', type: 'success' })
    } catch (err: unknown) {
      showToast({
        message: err instanceof Error ? err.message : 'Failed to resend',
        type: 'error',
      })
    } finally {
      setResending(false)
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={[styles.container, { paddingTop: insets.top, paddingBottom: insets.bottom + 32 }]}>
        {/* Header */}
        <View style={styles.header}>
          <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
            <ArrowLeft size={22} color={Colors.textMuted} />
          </TouchableOpacity>
        </View>

        <View style={styles.content}>
          {/* Title */}
          <View style={styles.titleBlock}>
            <Text style={styles.title}>Check your email</Text>
            <Text style={styles.subtitle}>
              We sent a 6-digit code to{'\n'}
              <Text style={styles.emailText}>{email}</Text>
            </Text>
          </View>

          {/* OTP Boxes */}
          <View style={styles.otpRow}>
            {otp.map((digit, i) => (
              <TextInput
                key={i}
                ref={(el) => { inputs.current[i] = el }}
                style={[styles.otpBox, digit ? styles.otpBoxFilled : null]}
                value={digit}
                onChangeText={(v) => handleChange(v, i)}
                onKeyPress={({ nativeEvent }) => handleKeyPress(nativeEvent.key, i)}
                keyboardType="number-pad"
                maxLength={1}
                selectTextOnFocus
                caretHidden
              />
            ))}
          </View>

          {/* Verify button */}
          <TouchableOpacity
            style={[styles.btn, (loading || otpValue.length < 6) && styles.btnDisabled]}
            onPress={handleVerify}
            disabled={loading || otpValue.length < 6}
            activeOpacity={0.85}
          >
            {loading ? (
              <ActivityIndicator color="#fff" />
            ) : (
              <Text style={styles.btnLabel}>Verify</Text>
            )}
          </TouchableOpacity>

          {/* Resend */}
          <View style={styles.resendRow}>
            <Text style={styles.resendText}>Didn't receive it? </Text>
            <TouchableOpacity onPress={handleResend} disabled={resending} hitSlop={8}>
              {resending ? (
                <ActivityIndicator size="small" color={Colors.primary} />
              ) : (
                <Text style={styles.link}>Resend OTP</Text>
              )}
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: Colors.background },
  container: { flex: 1, backgroundColor: Colors.background },
  header: {
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  content: {
    flex: 1,
    paddingHorizontal: 24,
    paddingTop: 16,
  },
  titleBlock: {
    marginBottom: 40,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 28,
    color: Colors.textPrimary,
    marginBottom: 10,
  },
  subtitle: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: Colors.textMuted,
    lineHeight: 24,
  },
  emailText: {
    fontFamily: 'DMSans_500Medium',
    color: Colors.textPrimary,
  },
  otpRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 32,
  },
  otpBox: {
    flex: 1,
    aspectRatio: 0.85,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: Colors.border,
    textAlign: 'center',
    fontFamily: 'Sora_700Bold',
    fontSize: 22,
    color: Colors.textPrimary,
  },
  otpBoxFilled: {
    borderColor: Colors.primary,
    backgroundColor: `${Colors.primary}14`,
  },
  btn: {
    backgroundColor: Colors.primary,
    borderRadius: 14,
    paddingVertical: 16,
    alignItems: 'center',
  },
  btnDisabled: {
    opacity: 0.5,
  },
  btnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: '#fff',
  },
  resendRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 28,
  },
  resendText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textMuted,
  },
  link: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 14,
    color: Colors.primary,
  },
})
