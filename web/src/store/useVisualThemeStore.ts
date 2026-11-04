import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export type VisualTheme = 'mudra' | 'khata' | 'gulal' | 'shunya' | 'bagh';

export const DEFAULT_VISUAL_THEME: VisualTheme = 'mudra';

export const VISUAL_THEMES: readonly {
  value: VisualTheme;
  label: string;
  tagline: string;
  /** Paper, ink, accent, signal — a preview strip for the picker, light then dark. */
  swatch: readonly [string, string, string, string];
  swatchDark: readonly [string, string, string, string];
}[] = [
  {
    value: 'mudra',
    label: 'Mudra',
    tagline: 'Banknote printing. Frosted glass, guilloche engraving, one lavender accent.',
    swatch: ['#f1eefb', '#1d1747', '#4b36c7', '#0b7a55'],
    swatchDark: ['#161236', '#eeeaff', '#b9adff', '#5fe0ae'],
  },
  {
    value: 'khata',
    label: 'Khata',
    tagline:
      'The accountant’s ledger. Ruled paper, ink, a red margin, rupees and paise in columns.',
    swatch: ['#e3ecd9', '#14234a', '#b8322a', '#1b6a42'],
    swatchDark: ['#0f1b17', '#e4eede', '#ff7b6c', '#6fd79d'],
  },
  {
    value: 'gulal',
    label: 'Gulal',
    tagline:
      'Holi-powder pop. Sticker-bright, chunky outlines, hard shadows, a colour per category.',
    swatch: ['#ffe14d', '#1b1230', '#ff5fa8', '#4457ff'],
    swatchDark: ['#1c1233', '#fff3d6', '#ff5fa8', '#ffe14d'],
  },
  {
    value: 'shunya',
    label: 'Shunya',
    tagline:
      'Zero. No cards, no fills — light numerals, hairlines and a month line that shows pace.',
    swatch: ['#fcfcfa', '#0f0f10', '#2740ff', '#12805a'],
    swatchDark: ['#0a0a0b', '#f3f3ef', '#8f9dff', '#5fd3a0'],
  },
  {
    value: 'bagh',
    label: 'Bagh',
    tagline: 'A kitchen garden. Leaf corners, soft and tactile; goals grow as plants.',
    swatch: ['#e6efdc', '#16301f', '#3a7a36', '#f4b21b'],
    swatchDark: ['#0e1a12', '#e9f3e0', '#7fcf7a', '#ffc83d'],
  },
];

const VALID = new Set<string>(VISUAL_THEMES.map((t) => t.value));

export function isVisualTheme(value: unknown): value is VisualTheme {
  return typeof value === 'string' && VALID.has(value);
}

/** The page chrome colour (PWA status bar) each theme paints behind the app, per scheme. */
export const THEME_COLORS: Record<VisualTheme, { light: string; dark: string }> = {
  mudra: { light: '#f1edff', dark: '#1b1546' },
  khata: { light: '#e3ecd9', dark: '#0f1b17' },
  gulal: { light: '#ffe14d', dark: '#1c1233' },
  shunya: { light: '#fcfcfa', dark: '#0a0a0b' },
  bagh: { light: '#e6efdc', dark: '#0e1a12' },
};

interface VisualThemeStore {
  visualTheme: VisualTheme;
  setVisualTheme: (theme: VisualTheme) => void;
}

/**
 * Which of the app's visual worlds is active, in its own store under `finio-visual-theme`.
 *
 * Deliberately **not** in `Settings`: settings are exported into every backup and uploaded to the
 * cloud, so adding a field there changes the backup contract (spec, web validator, Android
 * validator). A look is a device preference — a phone and a laptop restoring the same backup
 * should be free to wear different themes. `index.html` reads this key too, to avoid a flash of
 * the wrong theme before first paint, so keep the key and the `visualTheme` field name stable.
 */
export const useVisualThemeStore = create<VisualThemeStore>()(
  persist(
    (set) => ({
      visualTheme: DEFAULT_VISUAL_THEME,
      setVisualTheme: (visualTheme) => set({ visualTheme }),
    }),
    {
      name: 'finio-visual-theme',
      storage: createJSONStorage(() => localStorage),
      version: 1,
      // Anything unrecognised (a downgrade, a hand-edited value) falls back to the default.
      merge: (persisted, current) => {
        const stored = (persisted as Partial<VisualThemeStore> | undefined)?.visualTheme;
        return { ...current, visualTheme: isVisualTheme(stored) ? stored : DEFAULT_VISUAL_THEME };
      },
    },
  ),
);
