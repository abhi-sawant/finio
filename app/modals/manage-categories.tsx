import React from 'react'
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native'
import { useRouter } from 'expo-router'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import { X } from 'lucide-react-native'
import { Colors } from '@/constants/Colors'
import { CategoryManager } from '@/components/categories/CategoryManager'

export default function ManageCategoriesModal() {
  const router = useRouter()
  const insets = useSafeAreaInsets()

  return (
    <View style={[styles.container, { paddingTop: insets.top }]}>
      <View style={styles.header}>
        <Text style={styles.title}>Categories</Text>
        <TouchableOpacity onPress={() => router.back()} hitSlop={8}>
          <X size={22} color={Colors.textMuted} />
        </TouchableOpacity>
      </View>
      <CategoryManager />
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
})
