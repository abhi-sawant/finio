import { COLOR_PALETTE } from '@/data/colorPalette';
import {
  MISC_CATEGORY_ID,
  NEW_DEFAULT_CATEGORY_IDS,
  defaultCategories,
  defaultLabels,
  defaultSettings,
} from '@/data/defaultData';
import { gc, type GoldenCase } from '../golden';

// Seed data is part of the backup contract: a fresh install on either client must produce the
// same ids, names, icons, colours and order.
export default function cases(): GoldenCase[] {
  return [
    gc('MISC_CATEGORY_ID', [], MISC_CATEGORY_ID),
    gc('defaultCategories', [], defaultCategories),
    gc('NEW_DEFAULT_CATEGORY_IDS', [], NEW_DEFAULT_CATEGORY_IDS),
    gc('defaultLabels', [], defaultLabels),
    gc('defaultSettings', [], defaultSettings),
    gc('COLOR_PALETTE', [], COLOR_PALETTE),
  ];
}
