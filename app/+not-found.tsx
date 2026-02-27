import { Link, Stack } from 'expo-router'
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native'
import { Colors } from '@/constants/Colors'
import { CircleAlert } from 'lucide-react-native'

export default function NotFoundScreen() {
  return (
    <>
      <Stack.Screen options={{ title: 'Not Found' }} />
      <View style={styles.container}>
        <CircleAlert size={64} color={Colors.textMuted} />
        <Text style={styles.title}>Page not found</Text>
        <Text style={styles.description}>
          The screen you're looking for doesn't exist.
        </Text>
        <Link href="/" asChild>
          <TouchableOpacity style={styles.btn}>
            <Text style={styles.btnLabel}>Go to Dashboard</Text>
          </TouchableOpacity>
        </Link>
      </View>
    </>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 16,
  },
  title: {
    fontFamily: 'Sora_700Bold',
    fontSize: 24,
    color: Colors.textPrimary,
  },
  description: {
    fontFamily: 'DMSans_400Regular',
    fontSize: 15,
    color: Colors.textMuted,
    textAlign: 'center',
  },
  btn: {
    marginTop: 8,
    backgroundColor: Colors.primary,
    paddingHorizontal: 28,
    paddingVertical: 14,
    borderRadius: 14,
  },
  btnLabel: {
    fontFamily: 'Sora_700Bold',
    fontSize: 15,
    color: '#fff',
  },
})
