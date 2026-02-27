import React, { useState } from 'react'
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ScrollView,
  Alert,
} from 'react-native'
import { useRouter } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X, Plus, Pencil, Trash2, Check } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { BottomSheet } from '@/components/common/BottomSheet'
import { ColorPicker } from '@/components/common/ColorPicker'
import { useFinanceStore } from '@/store/useFinanceStore'
import { showToast } from '@/components/common/Toast'
import { warningHaptic, lightHaptic, successHaptic } from '@/utils/haptics'
import type { Label } from '@/types'

export default function ManageLabelsModal() {
  const router = useRouter()
  const insets = useSafeAreaInsets()
  const { labels, addLabel, updateLabel, deleteLabel } = useFinanceStore()

  const [sheetVisible, setSheetVisible] = useState(false)
  const [editingLabel, setEditingLabel] = useState<Label | null>(null)
  const [name, setName] = useState('')
  const [color, setColor] = useState('#6C63FF')

  const openAdd = () => {
    setEditingLabel(null)
    setName('')
    setColor('#6C63FF')
    setSheetVisible(true)
  }

  const openEdit = (label: Label) => {
    setEditingLabel(label)
    setName(label.name)
    setColor(label.color)
    setSheetVisible(true)
  }

  const handleSave = async () => {
    if (!name.trim()) {
      showToast({ message: 'Enter a label name', type: 'error' })
      return
    }
    await lightHaptic()
    if (editingLabel) {
      updateLabel(editingLabel.id, { name: name.trim(), color })
      showToast({ message: 'Label updated', type: 'success' })
    } else {
      addLabel({ name: name.trim(), color })
      showToast({ message: 'Label added', type: 'success' })
    }
    setSheetVisible(false)
  }

  const handleDelete = (label: Label) => {
    Alert.alert('Delete Label', `Delete "${label.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await warningHaptic()
          deleteLabel(label.id)
          showToast({ message: `"${label.name}" deleted`, type: 'error' })
        },
      },
    ])
  }

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Text style={styles.title}>Labels</Text>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={Colors.textMuted} />
        </TouchableOpacity>
      </View>

      <TouchableOpacity style={styles.addBtn} onPress={openAdd} activeOpacity={0.8}>
        <Plus size={18} color={Colors.primary} />
        <Text style={styles.addBtnText}>Add Label</Text>
      </TouchableOpacity>

      <ScrollView contentContainerStyle={styles.list}>
        {labels.map((label) => (
          <View key={label.id} style={styles.item}>
            <View style={[styles.dot, { backgroundColor: label.color }]} />
            <Text style={styles.name}>{label.name}</Text>
            <View style={styles.actions}>
              <TouchableOpacity onPress={() => openEdit(label)} hitSlop={8} style={styles.actionBtn}>
                <Pencil size={16} color={Colors.textMuted} />
              </TouchableOpacity>
              <TouchableOpacity onPress={() => handleDelete(label)} hitSlop={8} style={styles.actionBtn}>
                <Trash2 size={16} color={Colors.expense} />
              </TouchableOpacity>
            </View>
          </View>
        ))}
        {labels.length === 0 && (
          <Text style={styles.empty}>No labels yet. Add one above.</Text>
        )}
      </ScrollView>

      {/* Add/Edit sheet */}
      <BottomSheet
        visible={sheetVisible}
        onClose={() => setSheetVisible(false)}
        title={editingLabel ? 'Edit Label' : 'New Label'}
        snapPoint={0.55}
      >
        <ScrollView contentContainerStyle={styles.form}>
          <Text style={styles.fieldLabel}>Name</Text>
          <TextInput
            style={styles.input}
            value={name}
            onChangeText={setName}
            placeholder="e.g. Recurring, Work..."
            placeholderTextColor={Colors.textMuted}
            autoFocus
          />
          <ColorPicker selectedColor={color} onChange={setColor} label="Color" />
          <TouchableOpacity style={styles.saveBtn} onPress={handleSave} activeOpacity={0.8}>
            <Check size={18} color="#fff" />
            <Text style={styles.saveBtnLabel}>{editingLabel ? 'Update' : 'Add Label'}</Text>
          </TouchableOpacity>
        </ScrollView>
      </BottomSheet>
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 20,
    color: Colors.textPrimary,
  },
  addBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    margin: 16,
    padding: 14,
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: Colors.primary,
    justifyContent: 'center',
  },
  addBtnText: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 15,
    color: Colors.primary,
  },
  list: {
    paddingHorizontal: 16,
    gap: 8,
    paddingBottom: 24,
  },
  item: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  dot: {
    width: 14,
    height: 14,
    borderRadius: 7,
  },
  name: {
    flex: 1,
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
  },
  actionBtn: {
    padding: 6,
  },
  empty: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 14,
    color: Colors.textMuted,
    textAlign: 'center',
    marginTop: 32,
  },
  form: {
    padding: 20,
    gap: 16,
  },
  fieldLabel: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 13,
    color: Colors.textMuted,
    marginBottom: 4,
  },
  input: {
    backgroundColor: Colors.surfaceElevated ?? Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: Colors.textPrimary,
  },
  saveBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.primary,
    borderRadius: 14,
    paddingVertical: 14,
    marginTop: 8,
  },
  saveBtnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: '#fff',
  },
})
