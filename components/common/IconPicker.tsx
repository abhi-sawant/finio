import React from 'react';
import { TouchableOpacity, ScrollView, StyleSheet } from 'react-native';
import {
  Landmark,
  PiggyBank,
  Wallet,
  CreditCard,
  Banknote,
  Coins,
  Briefcase,
  Laptop,
  TrendingUp,
  Building2,
  Home,
  Car,
  Utensils,
  ShoppingCart,
  Film,
  Music,
  HeartPulse,
  BookOpen,
  Zap,
  Droplets,
  Wifi,
  Phone,
  Gift,
  Star,
  Coffee,
  Pizza,
  Shirt,
  Plane,
  Train,
  Bus,
  Dumbbell,
  Gamepad2,
  Scissors,
  Wrench,
  Package,
  Truck,
  Repeat,
  CircleEllipsis,
  Tag,
  Folder,
  Bookmark,
  Bell,
} from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import { BottomSheet } from './BottomSheet';
import { lightHaptic } from '@/utils/haptics';

export const AVAILABLE_ICONS = [
  'landmark',
  'piggy-bank',
  'wallet',
  'credit-card',
  'banknote',
  'coins',
  'briefcase',
  'laptop',
  'trending-up',
  'building-2',
  'home',
  'car',
  'utensils',
  'shopping-bag',
  'film',
  'music',
  'heart-pulse',
  'book-open',
  'zap',
  'droplets',
  'wifi',
  'phone',
  'gift',
  'star',
  'coffee',
  'pizza',
  'shirt',
  'plane',
  'train',
  'bus',
  'dumbbell',
  'gamepad-2',
  'scissors',
  'wrench',
  'package',
  'truck',
  'repeat',
  'circle-ellipsis',
  'tag',
  'folder',
  'bookmark',
  'bell',
] as const;

export type IconName = (typeof AVAILABLE_ICONS)[number];

type IconComponentType = React.ComponentType<{
  size: number;
  color: string;
  strokeWidth: number;
}>;

const ICON_COMPONENTS: Record<IconName, IconComponentType> = {
  landmark: Landmark,
  'piggy-bank': PiggyBank,
  wallet: Wallet,
  'credit-card': CreditCard,
  banknote: Banknote,
  coins: Coins,
  briefcase: Briefcase,
  laptop: Laptop,
  'trending-up': TrendingUp,
  'building-2': Building2,
  home: Home,
  car: Car,
  utensils: Utensils,
  // Keep the existing icon name for compatibility with stored values.
  'shopping-bag': ShoppingCart,
  film: Film,
  music: Music,
  'heart-pulse': HeartPulse,
  'book-open': BookOpen,
  zap: Zap,
  droplets: Droplets,
  wifi: Wifi,
  phone: Phone,
  gift: Gift,
  star: Star,
  coffee: Coffee,
  pizza: Pizza,
  shirt: Shirt,
  plane: Plane,
  train: Train,
  bus: Bus,
  dumbbell: Dumbbell,
  'gamepad-2': Gamepad2,
  scissors: Scissors,
  wrench: Wrench,
  package: Package,
  truck: Truck,
  repeat: Repeat,
  'circle-ellipsis': CircleEllipsis,
  tag: Tag,
  folder: Folder,
  bookmark: Bookmark,
  bell: Bell,
};

export function LucideIcon({
  name,
  size = 20,
  color,
  strokeWidth = 2,
}: {
  name: string;
  size?: number;
  color?: string;
  strokeWidth?: number;
}) {
  const fallbackColors = useColors();
  const resolvedColor = color ?? fallbackColors.textPrimary;
  const IconComponent = ICON_COMPONENTS[name as IconName] ?? CircleEllipsis;

  return <IconComponent size={size} color={resolvedColor} strokeWidth={strokeWidth} />;
}

interface IconPickerProps {
  selectedIcon: string;
  onChange: (icon: string) => void;
  accentColor?: string;
  visible: boolean;
  onClose: () => void;
}

export function IconPicker({
  selectedIcon,
  onChange,
  accentColor,
  visible,
  onClose,
}: IconPickerProps) {
  const colors = useColors();
  const resolvedAccent = accentColor ?? colors.primary;
  const styles = makeStyles(colors);
  const handleSelect = async (icon: string) => {
    await lightHaptic();
    onChange(icon);
    onClose();
  };

  return (
    <BottomSheet visible={visible} onClose={onClose} title="Select Icon" snapPoint={0.75}>
      <ScrollView contentContainerStyle={styles.grid}>
        {AVAILABLE_ICONS.map((icon) => {
          const isSelected = icon === selectedIcon;
          return (
            <TouchableOpacity
              key={icon}
              onPress={() => handleSelect(icon)}
              style={[
                styles.iconBtn,
                isSelected && {
                  backgroundColor: resolvedAccent + '33',
                  borderColor: resolvedAccent,
                },
              ]}
              activeOpacity={0.7}
            >
              <LucideIcon
                name={icon}
                size={22}
                color={isSelected ? resolvedAccent : colors.textPrimary}
              />
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </BottomSheet>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    grid: {
      flexDirection: 'row',
      flexWrap: 'wrap',
      padding: 16,
      gap: 10,
      justifyContent: 'center',
    },
    iconBtn: {
      width: 52,
      height: 52,
      borderRadius: 14,
      backgroundColor: colors.surfaceElevated,
      borderWidth: 1,
      borderColor: colors.border,
      alignItems: 'center',
      justifyContent: 'center',
    },
  });
}
