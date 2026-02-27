import React, { useState } from 'react'
import { View, Text, TouchableOpacity, Platform, StyleSheet } from 'react-native'
import DateTimePicker, { type DateTimePickerEvent } from '@react-native-community/datetimepicker'
import { format } from 'date-fns'
import { CalendarDays } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { BottomSheet } from './BottomSheet'
import { lightHaptic } from '@/utils/haptics'

interface DatePickerProps {
  value: Date
  onChange: (date: Date) => void
  label?: string
}

export function DatePicker({ value, onChange, label = 'Date' }: DatePickerProps) {
  const [showSheet, setShowSheet] = useState(false)

  const handleChange = (_: DateTimePickerEvent, selectedDate?: Date) => {
    if (selectedDate) {
      onChange(selectedDate)
      if (Platform.OS === 'android') setShowSheet(false)
    }
  }

  const handlePress = async () => {
    await lightHaptic()
    setShowSheet(true)
  }

  return (
    <>
      <TouchableOpacity style={styles.trigger} onPress={handlePress} activeOpacity={0.7}>
        <CalendarDays size={18} color={Colors.textMuted} />
        <View style={styles.textContainer}>
          <Text style={styles.label}>{label}</Text>
          <Text style={styles.value}>{format(value, 'dd MMM yyyy')}</Text>
        </View>
      </TouchableOpacity>

      {Platform.OS === 'ios' ? (
        <BottomSheet visible={showSheet} onClose={() => setShowSheet(false)} title="Select Date">
          <View style={styles.pickerWrapper}>
            <DateTimePicker
              value={value}
              mode="date"
              display="spinner"
              onChange={handleChange}
              textColor={Colors.textPrimary}
              style={{ flex: 1 }}
            />
            <TouchableOpacity
              style={styles.doneBtn}
              onPress={() => setShowSheet(false)}
            >
              <Text style={styles.doneBtnText}>Done</Text>
            </TouchableOpacity>
          </View>
        </BottomSheet>
      ) : (
        showSheet && (
          <DateTimePicker
            value={value}
            mode="date"
            display="default"
            onChange={handleChange}
          />
        )
      )}
    </>
  )
}

const styles = StyleSheet.create({
  trigger: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    backgroundColor: Colors.surfaceElevated,
    borderRadius: 14,
    padding: 14,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  textContainer: {
    flex: 1,
  },
  label: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 12,
    color: Colors.textMuted,
  },
  value: {
    fontFamily: 'DMSans_500Medium',
    fontSize: 15,
    color: Colors.textPrimary,
    marginTop: 2,
  },
  pickerWrapper: {
    paddingHorizontal: 16,
    paddingBottom: 16,
  },
  doneBtn: {
    backgroundColor: Colors.primary,
    borderRadius: 14,
    padding: 14,
    alignItems: 'center',
    marginTop: 8,
  },
  doneBtnText: {
    fontFamily: 'DMSans_700Bold',
    fontSize: 16,
    color: '#fff',
  },
})
