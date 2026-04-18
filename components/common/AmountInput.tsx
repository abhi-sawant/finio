import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Delete } from 'lucide-react-native';
import { useColors } from '@/hooks/useColors';
import type { ColorPalette } from '@/constants/Colors';
import type { Currency } from '@/types';

const CURRENCY_SYMBOLS: Record<Currency, string> = {
  INR: '₹',
  USD: '$',
  EUR: '€',
  GBP: '£',
  JPY: '¥',
  CAD: 'C$',
  AUD: 'A$',
};

const KEYS = [
  ['7', '8', '9'],
  ['4', '5', '6'],
  ['1', '2', '3'],
  ['.', '0', '⌫'],
];

interface AmountInputProps {
  value: number;
  onChange: (value: number) => void;
  currency?: Currency;
  placeholder?: string;
  autoFocus?: boolean;
}

export function AmountInput({ value, onChange, currency = 'INR' }: AmountInputProps) {
  const colors = useColors();
  const styles = makeStyles(colors);
  const [displayText, setDisplayText] = useState(value > 0 ? value.toString() : '');

  useEffect(() => {
    setDisplayText(value > 0 ? value.toString() : '');
  }, [value]);

  const symbol = CURRENCY_SYMBOLS[currency];

  const handleKey = (key: string) => {
    if (key === '⌫') {
      setDisplayText((prev) => {
        const next = prev.slice(0, -1);
        const parsed = parseFloat(next);
        onChange(isNaN(parsed) ? 0 : parsed);
        return next;
      });
      return;
    }
    setDisplayText((prev) => {
      if (key === '.' && prev.includes('.')) return prev;
      if (key === '.' && prev === '') return prev;
      if (prev.includes('.')) {
        const decimals = prev.split('.')[1] ?? '';
        if (decimals.length >= 2) return prev;
      }
      if (!prev.includes('.') && prev.replace(/^0/, '').length >= 9) return prev;

      const next = prev === '' && key === '0' ? '0' : prev + key;
      const parsed = parseFloat(next);
      onChange(isNaN(parsed) ? 0 : parsed);
      return next;
    });
  };

  const formattedDisplay = (): string => {
    if (!displayText) return '0';
    const parts = displayText.split('.');
    const intPart = parseInt(parts[0] ?? '0', 10);
    const formatted = new Intl.NumberFormat('en-IN').format(intPart);
    return parts.length > 1 ? formatted + '.' + (parts[1] ?? '') : formatted;
  };

  return (
    <View style={styles.wrapper}>
      {/* Display */}
      <View style={styles.display}>
        <Text style={styles.symbol}>{symbol}</Text>
        <Text style={[styles.amount, !displayText && styles.placeholder]}>
          {formattedDisplay()}
        </Text>
      </View>

      {/* Numpad */}
      <View style={styles.numpad}>
        {KEYS.map((row, ri) => (
          <View key={ri} style={styles.row}>
            {row.map((key) => (
              <TouchableOpacity
                key={key}
                style={styles.key}
                onPress={() => handleKey(key)}
                activeOpacity={0.6}
              >
                {key === '⌫' ? (
                  <Delete size={22} color={colors.textPrimary} />
                ) : (
                  <Text style={styles.keyLabel}>{key}</Text>
                )}
              </TouchableOpacity>
            ))}
          </View>
        ))}
      </View>
    </View>
  );
}

function makeStyles(colors: ColorPalette) {
  return StyleSheet.create({
    wrapper: {
      gap: 8,
    },
    display: {
      flexDirection: 'row',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 4,
      paddingVertical: 8,
    },
    symbol: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 32,
      color: colors.textPrimary,
    },
    amount: {
      fontFamily: 'DMSans_700Bold',
      fontSize: 48,
      color: colors.textPrimary,
    },
    placeholder: {
      color: colors.textMuted,
    },
    numpad: {
      gap: 4,
    },
    row: {
      flexDirection: 'row',
      gap: 4,
    },
    key: {
      flex: 1,
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: colors.surface,
      borderRadius: 12,
      paddingVertical: 16,
      borderWidth: StyleSheet.hairlineWidth,
      borderColor: colors.border,
    },
    keyLabel: {
      fontFamily: 'DMSans_500Medium',
      fontSize: 22,
      color: colors.textPrimary,
    },
  });
}
