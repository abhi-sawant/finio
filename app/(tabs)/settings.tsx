import React, { useState } from 'react'
import {
  View,
  Text,
  ScrollView,
  StyleSheet,
  TouchableOpacity,
  Switch,
  Alert,
  TextInput,
  ActivityIndicator,
} from 'react-native'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { useRouter } from 'expo-router'
import {
  User,
  Bell,
  Vibrate,
  Tag,
  List,
  Download,
  Upload,
  Trash2,
  Info,
  ChevronRight,
  Moon,
  DollarSign,
  Shield,
  Check,
  Cloud,
  CloudOff,
  CloudUpload,
  CloudDownload,
  LogOut,
} from 'lucide-react-native'
import * as FileSystem from 'expo-file-system/legacy'
import * as Sharing from 'expo-sharing'
import * as DocumentPicker from 'expo-document-picker'
import { useColors } from '@/hooks/useColors'
import type { ColorPalette } from '@/constants/Colors'
import { BottomSheet } from '@/components/common/BottomSheet'
import { useFinanceStore } from '@/store/useFinanceStore'
import { useAuthStore } from '@/store/useAuthStore'
import { uploadBackup, restoreLatestBackup } from '@/services/backup'
import { showToast } from '@/components/common/Toast'
import { warningHaptic, lightHaptic } from '@/utils/haptics'
import type { Currency, Theme } from '@/types'

const CURRENCIES: { code: Currency; symbol: string; name: string }[] = [
  { code: 'INR', symbol: '₹', name: 'Indian Rupee' },
  { code: 'USD', symbol: '$', name: 'US Dollar' },
  { code: 'EUR', symbol: '€', name: 'Euro' },
  { code: 'GBP', symbol: '£', name: 'British Pound' },
  { code: 'JPY', symbol: '¥', name: 'Japanese Yen' },
  { code: 'CAD', symbol: 'CA$', name: 'Canadian Dollar' },
  { code: 'AUD', symbol: 'A$', name: 'Australian Dollar' },
]


type SectionItem = {
  icon: React.ReactNode
  label: string
  onPress?: () => void
  right?: React.ReactNode
  danger?: boolean
}

function SettingsRow({ icon, label, onPress, right, danger }: SectionItem) {
  const colors = useColors()
  const styles = makeStyles(colors)
  return (
    <TouchableOpacity
      style={styles.row}
      onPress={onPress}
      disabled={!onPress && !right}
      activeOpacity={0.7}
    >
      <View style={styles.rowLeft}>
        <View style={[styles.iconBox, danger && styles.iconBoxDanger]}>{icon}</View>
        <Text style={[styles.rowLabel, danger && styles.rowLabelDanger]}>{label}</Text>
      </View>
      {right ?? (onPress ? <ChevronRight size={16} color={colors.textMuted} /> : null)}
    </TouchableOpacity>
  )
}

function SectionHeader({ title }: { title: string }) {
  const colors = useColors()
  const styles = makeStyles(colors)
  return <Text style={styles.sectionHeader}>{title}</Text>
}

function formatLastBackup(iso: string | null): string {
  if (!iso) return 'Never'
  const d = new Date(iso)
  const now = new Date()
  const isToday =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear()
  const time = d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  return isToday ? `Today at ${time}` : d.toLocaleDateString()
}

const THEME_OPTIONS: Array<{ value: Theme; label: string; icon: string }> = [
  { value: 'light', label: 'Light', icon: '☀️' },
  { value: 'dark', label: 'Dark', icon: '🌙' },
  { value: 'system', label: 'System', icon: '⚙️' },
]

export default function SettingsScreen() {
  const insets = useSafeAreaInsets()
  const router = useRouter()
  const colors = useColors()
  const styles = makeStyles(colors)
  const { settings, updateSettings, transactions, accounts, categories, labels, resetToDefaults, importData } =
    useFinanceStore()
  const { user, token, lastBackupAt, clearAuth } = useAuthStore()

  const [nameSheetVisible, setNameSheetVisible] = useState(false)
  const [currencySheetVisible, setCurrencySheetVisible] = useState(false)
  const [nameInput, setNameInput] = useState(settings.userName)
  const [backupLoading, setBackupLoading] = useState(false)
  const [restoreLoading, setRestoreLoading] = useState(false)

  // ─── Name ───────────────────────────────────────────────
  const handleSaveName = () => {
    if (nameInput.trim()) {
      updateSettings({ userName: nameInput.trim() })
      showToast({ message: 'Name updated', type: 'success' })
    }
    setNameSheetVisible(false)
  }

  // ─── Currency ────────────────────────────────────────────
  const handleSelectCurrency = (code: Currency) => {
    updateSettings({ currency: code })
    showToast({ message: `Currency set to ${code}`, type: 'success' })
    setCurrencySheetVisible(false)
  }

  // ─── Notifications ───────────────────────────────────────
  const handleNotificationsToggle = async (enabled: boolean) => {
    try {
      const Notifications = await import('expo-notifications')
      Notifications.setNotificationHandler({
        handleNotification: async () => ({
          shouldShowBanner: true,
          shouldShowList: true,
          shouldPlaySound: false,
          shouldSetBadge: false,
        }),
      })
      if (enabled) {
        const { status } = await Notifications.requestPermissionsAsync()
        if (status !== 'granted') {
          showToast({ message: 'Notification permission denied', type: 'error' })
          return
        }
        await Notifications.cancelAllScheduledNotificationsAsync()
        await Notifications.scheduleNotificationAsync({
          content: {
            title: 'Fina Reminder',
            body: "Don't forget to log today's expenses!",
          },
          trigger: {
            type: Notifications.SchedulableTriggerInputTypes.DAILY,
            hour: 9,
            minute: 0,
          },
        })
        updateSettings({ notifications: true })
        showToast({ message: 'Daily reminders enabled', type: 'success' })
      } else {
        await Notifications.cancelAllScheduledNotificationsAsync()
        updateSettings({ notifications: false })
        showToast({ message: 'Notifications disabled', type: 'info' })
      }
    } catch {
      showToast({ message: 'Notifications not supported in this environment', type: 'error' })
    }
  }

  // ─── Export ──────────────────────────────────────────────
  const handleExport = async () => {
    lightHaptic()
    try {
      const data = {
        accounts,
        transactions,
        categories,
        labels,
        exportedAt: new Date().toISOString(),
      }
      const json = JSON.stringify(data, null, 2)
      const fileName = `fina-export-${new Date().toISOString().slice(0, 10)}.json`
      const fileUri = `${FileSystem.cacheDirectory}${fileName}`
      await FileSystem.writeAsStringAsync(fileUri, json, { encoding: 'utf8' })
      const canShare = await Sharing.isAvailableAsync()
      if (!canShare) {
        showToast({ message: 'Sharing not available on this device', type: 'error' })
        return
      }
      await Sharing.shareAsync(fileUri, {
        mimeType: 'application/json',
        dialogTitle: 'Save Fina Export',
        UTI: 'public.json',
      })
    } catch (err) {
      console.error('Export error:', err)
      showToast({ message: 'Export failed', type: 'error' })
    }
  }

  // ─── Import ──────────────────────────────────────────────
  const handleImport = async () => {
    lightHaptic()
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/json',
        copyToCacheDirectory: true,
      })
      if (result.canceled || !result.assets?.[0]) return
      const fileUri = result.assets[0].uri
      const raw = await FileSystem.readAsStringAsync(fileUri, { encoding: 'utf8' })
      const parsed = JSON.parse(raw)
      if (!parsed.accounts && !parsed.transactions && !parsed.categories) {
        showToast({ message: 'Invalid Fina export file', type: 'error' })
        return
      }
      Alert.alert(
        'Import Data',
        'This will replace your current data with the imported data. Continue?',
        [
          { text: 'Cancel', style: 'cancel' },
          {
            text: 'Import',
            onPress: () => {
              importData(parsed)
              showToast({ message: 'Data imported successfully', type: 'success' })
            },
          },
        ]
      )
    } catch (err) {
      console.error('Import error:', err)
      showToast({ message: 'Import failed — check if the file is valid', type: 'error' })
    }
  }

  // ─── Cloud Backup ─────────────────────────────────────────
  const handleBackupNow = async () => {
    if (!token || backupLoading) return
    setBackupLoading(true)
    try {
      await uploadBackup()
      showToast({ message: 'Backup uploaded successfully', type: 'success' })
    } catch (err: unknown) {
      showToast({
        message: err instanceof Error ? err.message : 'Backup failed',
        type: 'error',
      })
    } finally {
      setBackupLoading(false)
    }
  }

  const handleRestore = () => {
    if (restoreLoading) return
    Alert.alert(
      'Restore from Backup',
      'This will replace your current data with your latest cloud backup. This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Restore',
          style: 'destructive',
          onPress: async () => {
            setRestoreLoading(true)
            try {
              await restoreLatestBackup()
              showToast({ message: 'Data restored from backup', type: 'success' })
            } catch (err: unknown) {
              showToast({
                message: err instanceof Error ? err.message : 'Restore failed',
                type: 'error',
              })
            } finally {
              setRestoreLoading(false)
            }
          },
        },
      ]
    )
  }

  const handleSignOut = () => {
    Alert.alert('Sign Out', 'You will no longer receive automatic backups.', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Sign Out',
        style: 'destructive',
        onPress: async () => {
          await clearAuth()
          showToast({ message: 'Signed out', type: 'info' })
        },
      },
    ])
  }

  // ─── Clear ───────────────────────────────────────────────
  const handleClearData = () => {
    warningHaptic()
    Alert.alert(
      'Clear All Data',
      'This will permanently delete all your accounts, transactions, and categories. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear Everything',
          style: 'destructive',
          onPress: () => {
            resetToDefaults()
            showToast({ message: 'All data cleared', type: 'info' })
          },
        },
      ]
    )
  }

  const selectedCurrency = CURRENCIES.find((c) => c.code === settings.currency)

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Text style={styles.title}>Settings</Text>
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scroll}>

        {/* Profile */}
        <SectionHeader title="Profile" />
        <View style={styles.section}>
          <SettingsRow
            icon={<User size={16} color={colors.primary} />}
            label={settings.userName}
            onPress={() => {
              setNameInput(settings.userName)
              setNameSheetVisible(true)
            }}
          />
        </View>

        {/* Preferences */}
        <SectionHeader title="Preferences" />
        <View style={styles.section}>
          <SettingsRow
            icon={<DollarSign size={16} color={colors.primary} />}
            label={`${selectedCurrency?.symbol ?? ''} ${settings.currency}`}
            onPress={() => setCurrencySheetVisible(true)}
          />
          {/* Theme selection: Light / Dark / System */}
          <View style={styles.row}>
            <View style={styles.rowLeft}>
              <View style={styles.iconBox}>
                <Moon size={16} color={colors.primary} />
              </View>
              <Text style={styles.rowLabel}>Theme</Text>
            </View>
            <View style={styles.themeToggleGroup}>
              {THEME_OPTIONS.map((opt) => {
                const isActive = settings.theme === opt.value
                return (
                  <TouchableOpacity
                    key={opt.value}
                    onPress={() => {
                      lightHaptic()
                      updateSettings({ theme: opt.value })
                    }}
                    style={[
                      styles.themeBtn,
                      isActive && { backgroundColor: colors.primary, borderColor: colors.primary },
                    ]}
                    activeOpacity={0.75}
                  >
                    <Text style={[styles.themeBtnText, isActive && styles.themeBtnTextActive]}>
                      {opt.label}
                    </Text>
                  </TouchableOpacity>
                )
              })}
            </View>
          </View>
          <SettingsRow
            icon={<Vibrate size={16} color={colors.primary} />}
            label="Haptic Feedback"
            right={
              <Switch
                value={settings.hapticFeedback}
                onValueChange={(v) => updateSettings({ hapticFeedback: v })}
                trackColor={{ false: colors.border, true: colors.primary }}
                thumbColor="#fff"
              />
            }
          />
          <SettingsRow
            icon={<Bell size={16} color={colors.primary} />}
            label="Daily Reminders"
            right={
              <Switch
                value={settings.notifications ?? false}
                onValueChange={handleNotificationsToggle}
                trackColor={{ false: colors.border, true: colors.primary }}
                thumbColor="#fff"
              />
            }
          />
        </View>

        {/* Categories & Labels */}
        <SectionHeader title="Categories & Labels" />
        <View style={styles.section}>
          <SettingsRow
            icon={<List size={16} color={colors.primary} />}
            label="Manage Categories"
            onPress={() => { lightHaptic(); router.push('/modals/manage-categories') }}
          />
          <SettingsRow
            icon={<Tag size={16} color={colors.primary} />}
            label="Manage Labels"
            onPress={() => { lightHaptic(); router.push('/modals/manage-labels') }}
          />
        </View>

        {/* Cloud Backup */}
        <SectionHeader title="Cloud Backup" />
        {user ? (
          <View style={styles.section}>
            {/* Signed-in user info */}
            <View style={[styles.row, { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: colors.border }]}>
              <View style={styles.rowLeft}>
                <View style={styles.iconBox}>
                  <Cloud size={16} color={colors.primary} />
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={styles.rowLabel}>{user.name}</Text>
                  <Text style={[styles.rowLabel, { fontSize: 12, color: colors.textMuted, fontFamily: 'DMSans_400Regular' }]}>
                    {user.email}
                  </Text>
                </View>
              </View>
              <Text style={[styles.rowLabel, { fontSize: 12, color: colors.textMuted, fontFamily: 'DMSans_400Regular' }]}>
                {formatLastBackup(lastBackupAt)}
              </Text>
            </View>
            <SettingsRow
              icon={<CloudUpload size={16} color={colors.primary} />}
              label="Back Up Now"
              onPress={handleBackupNow}
              right={backupLoading ? <ActivityIndicator size="small" color={colors.primary} /> : undefined}
            />
            <SettingsRow
              icon={<CloudDownload size={16} color={colors.primary} />}
              label="Restore from Backup"
              onPress={handleRestore}
              right={restoreLoading ? <ActivityIndicator size="small" color={colors.primary} /> : undefined}
            />
            <SettingsRow
              icon={<LogOut size={16} color={colors.expense} />}
              label="Sign Out"
              onPress={handleSignOut}
              danger
            />
          </View>
        ) : (
          <View style={styles.section}>
            <View style={styles.backupBanner}>
              <CloudOff size={18} color={colors.textMuted} />
              <Text style={styles.backupBannerText}>
                Sign in to automatically back up your data once a day
              </Text>
            </View>
            <SettingsRow
              icon={<User size={16} color={colors.primary} />}
              label="Sign In"
              onPress={() => { lightHaptic(); router.push('/auth/login' as never) }}
            />
            <SettingsRow
              icon={<Cloud size={16} color={colors.primary} />}
              label="Create Account"
              onPress={() => { lightHaptic(); router.push('/auth/register' as never) }}
            />
          </View>
        )}

        {/* Data */}
        <SectionHeader title="Data" />
        <View style={styles.section}>
          <SettingsRow
            icon={<Download size={16} color={colors.primary} />}
            label="Export Data (JSON)"
            onPress={handleExport}
          />
          <SettingsRow
            icon={<Upload size={16} color={colors.primary} />}
            label="Import Data (JSON)"
            onPress={handleImport}
          />
          <SettingsRow
            icon={<Trash2 size={16} color={colors.expense} />}
            label="Clear All Data"
            onPress={handleClearData}
            danger
          />
        </View>

        {/* About */}
        <SectionHeader title="About" />
        <View style={styles.section}>
          <SettingsRow
            icon={<Info size={16} color={colors.primary} />}
            label="Version 1.0.0"
          />
          <SettingsRow
            icon={<Shield size={16} color={colors.primary} />}
            label="Privacy Policy"
            onPress={() => showToast({ message: 'Privacy policy coming soon', type: 'info' })}
          />
        </View>

        {/* App stats */}
        <View style={styles.statsRow}>
          <View style={styles.stat}>
            <Text style={styles.statValue}>{accounts.length}</Text>
            <Text style={styles.statLabel}>Accounts</Text>
          </View>
          <View style={styles.stat}>
            <Text style={styles.statValue}>{transactions.length}</Text>
            <Text style={styles.statLabel}>Transactions</Text>
          </View>
          <View style={styles.stat}>
            <Text style={styles.statValue}>{categories.length}</Text>
            <Text style={styles.statLabel}>Categories</Text>
          </View>
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>

      {/* Name Edit Sheet */}
      <BottomSheet
        visible={nameSheetVisible}
        onClose={() => setNameSheetVisible(false)}
        title="Your Name"
        snapPoint={0.35}
      >
        <View style={styles.nameForm}>
          <TextInput
            style={styles.nameInput}
            value={nameInput}
            onChangeText={setNameInput}
            placeholder="Enter your name"
            placeholderTextColor={colors.textMuted}
            autoFocus
            returnKeyType="done"
            onSubmitEditing={handleSaveName}
          />
          <TouchableOpacity style={styles.saveBtn} onPress={handleSaveName} activeOpacity={0.8}>
            <Check size={18} color="#fff" />
            <Text style={styles.saveBtnLabel}>Save</Text>
          </TouchableOpacity>
        </View>
      </BottomSheet>

      {/* Currency Picker Sheet */}
      <BottomSheet
        visible={currencySheetVisible}
        onClose={() => setCurrencySheetVisible(false)}
        title="Select Currency"
        snapPoint={0.6}
      >
        <ScrollView contentContainerStyle={styles.currencyList}>
          {CURRENCIES.map((c) => {
            const isSelected = c.code === settings.currency
            return (
              <TouchableOpacity
                key={c.code}
                style={[styles.currencyRow, isSelected && styles.currencyRowSelected]}
                onPress={() => handleSelectCurrency(c.code)}
                activeOpacity={0.7}
              >
                <View style={styles.currencySymbolBox}>
                  <Text style={styles.currencySymbol}>{c.symbol}</Text>
                </View>
                <View style={styles.currencyInfo}>
                  <Text style={[styles.currencyCode, isSelected && { color: colors.primary }]}>{c.code}</Text>
                  <Text style={styles.currencyName}>{c.name}</Text>
                </View>
                {isSelected && <Check size={18} color={colors.primary} strokeWidth={2.5} />}
              </TouchableOpacity>
            )
          })}
        </ScrollView>
      </BottomSheet>
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
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 24,
    color: colors.textPrimary,
  },
  scroll: {
    paddingHorizontal: 16,
    gap: 4,
  },
  sectionHeader: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 12,
    color: colors.textMuted,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
    paddingHorizontal: 4,
    marginTop: 16,
    marginBottom: 4,
  },
  section: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  rowLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    flex: 1,
  },
  iconBox: {
    width: 30,
    height: 30,
    borderRadius: 8,
    backgroundColor: colors.surfaceElevated ?? colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  iconBoxDanger: {
    backgroundColor: `${colors.expense}22`,
  },
  rowLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: colors.textPrimary,
  },
  rowLabelDanger: {
    color: colors.expense,
  },
  backupBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  backupBannerText: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 13,
    color: colors.textMuted,
    flex: 1,
    lineHeight: 18,
  },
  statsRow: {
    flexDirection: 'row',
    marginTop: 24,
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.border,
    overflow: 'hidden',
  },
  stat: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 16,
    borderRightWidth: StyleSheet.hairlineWidth,
    borderRightColor: colors.border,
  },
  statValue: {
    fontFamily: 'Sora_700Bold',
    fontSize: 22,
    color: colors.primary,
  },
  statLabel: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 2,
  },
  // Name form
  nameForm: {
    padding: 20,
    gap: 16,
  },
  nameInput: {
    backgroundColor: colors.surfaceElevated ?? colors.surface,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 13,
    fontFamily: 'DMSans_400Regular',
    fontSize: 16,
    color: colors.textPrimary,
  },
  saveBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: colors.primary,
    borderRadius: 14,
    paddingVertical: 14,
  },
  saveBtnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: '#fff',
  },
  // Currency picker
  currencyList: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 6,
  },
  currencyRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: colors.border,
    backgroundColor: colors.surface,
  },
  currencyRowSelected: {
    borderColor: colors.primary,
    backgroundColor: `${colors.primary}14`,
  },
  currencySymbolBox: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: colors.surfaceElevated ?? colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  currencySymbol: {
    fontFamily: 'Sora_700Bold',
    fontSize: 16,
    color: colors.textPrimary,
  },
  currencyInfo: {
    flex: 1,
  },
  currencyCode: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 15,
    color: colors.textPrimary,
  },
  currencyName: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: colors.textMuted,
    marginTop: 1,
  },
  // Theme picker
  themeToggleGroup: {
    flexDirection: 'row',
    backgroundColor: colors.surfaceElevated ?? colors.surface,
    borderRadius: 10,
    padding: 3,
    gap: 2,
  },
  themeBtn: {
    paddingHorizontal: 11,
    paddingVertical: 6,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  themeBtnText: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 12,
    color: colors.textMuted,
  },
  themeBtnTextActive: {
    color: '#fff',
    fontFamily: 'DMSans_700Bold',
  },
  })
}
