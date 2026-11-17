import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';

export type VisualTheme =
  | 'mudra'
  | 'khata'
  | 'gulal'
  | 'shunya'
  | 'bagh'
  | 'parchi'
  | 'mahal'
  | 'ganak'
  | 'akhbaar'
  | 'khel'
  | 'tareekh'
  | 'safar'
  | 'naksha'
  | 'ajrakh'
  | 'kolam'
  | 'maidan'
  | 'sector'
  | 'daak'
  | 'sargam'
  | 'takhti';

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
  {
    value: 'parchi',
    label: 'Parchi',
    tagline:
      'The kirana receipt. Torn paper, mono line items, double-height totals, one receipt red.',
    swatch: ['#f7f4ec', '#23211c', '#c8242b', '#2e6a3d'],
    swatchDark: ['#11131a', '#dfe6f5', '#ff7a6e', '#7fcf9a'],
  },
  {
    value: 'mahal',
    label: 'Mahal',
    tagline: 'Bombay Art Deco. Peacock teal and gold, stepped frames, a sunburst over your figure.',
    swatch: ['#f4ecdc', '#0f3a3f', '#9a7a35', '#1b7550'],
    swatchDark: ['#0c1f22', '#f3ead6', '#d6b46a', '#6fd3a3'],
  },
  {
    value: 'ganak',
    label: 'Ganak',
    tagline: 'The pocket calculator. Putty plastic, rubber keys and an LCD window.',
    swatch: ['#e7e1d3', '#24211b', '#d4541d', '#b9c3a3'],
    swatchDark: ['#16171a', '#ebe7dc', '#ff7a3d', '#5ef2c4'],
  },
  {
    value: 'akhbaar',
    label: 'Akhbaar',
    tagline: 'The morning paper. Newsprint, headline figures, halftone charts, one masthead red.',
    swatch: ['#f2efe6', '#151515', '#c4161c', '#1e6a3b'],
    swatchDark: ['#17181b', '#ece8dc', '#ff6259', '#6fcf8f'],
  },
  {
    value: 'khel',
    label: 'Khel',
    tagline: 'An 8-bit handheld. Pixel type, health-bar budgets and a coin to add.',
    swatch: ['#e0efbe', '#0f380f', '#9bbc0f', '#a01641'],
    swatchDark: ['#0d0b1e', '#f6edd3', '#ffcf3f', '#ff5c72'],
  },
  {
    value: 'tareekh',
    label: 'Tareekh',
    tagline:
      'The household wall calendar. White coated paper, black ink, calendar red and postal blue; the month drawn as a real grid with today ringed and every day ahead carrying its allowance.',
    swatch: ['#fcfdfe', '#0c0e12', '#d51f2a', '#1746b3'],
    swatchDark: ['#0a0f1f', '#edefe8', '#ff4a4d', '#86acff'],
  },
  {
    value: 'safar',
    label: 'Safar',
    tagline:
      'The departures board. Split-flap figures on matte black, signal amber, green and red, coach-blue shell — and a chrome-yellow platform board with heavy black lettering in daylight.',
    swatch: ['#d9d8d1', '#111213', '#f6c400', '#1c4a8a'],
    swatchDark: ['#0c0e11', '#f2ecdc', '#ffb224', '#5bd08a'],
  },
  {
    value: 'naksha',
    label: 'Naksha',
    tagline:
      'The architect’s plan. Prussian-blue cyanotype or tracing paper, 1px linework with crop marks, hatched selections, red-pencil revisions, and every budget dimensioned to scale.',
    swatch: ['#f6f9fc', '#0d2c6b', '#1647a6', '#c4271b'],
    swatchDark: ['#12397a', '#f4f8ff', '#ffe98a', '#ff9684'],
  },
  {
    value: 'ajrakh',
    label: 'Ajrakh',
    tagline:
      'Block-printed cloth. Cotton panels on a madder ground by day, an indigo vat by night, printed borders, and budgets stamped out block by block.',
    swatch: ['#9a2b1f', '#1b2a5c', '#e2a419', '#2c6a2b'],
    swatchDark: ['#121a4e', '#f1e9d6', '#e5553e', '#93d47c'],
  },
  {
    value: 'kolam',
    label: 'Kolam',
    tagline:
      'The threshold drawing. Swept stone and red oxide, one monoline weight, dot-row dividers, and the month drawn as a kolam whose line loops every day gone.',
    swatch: ['#e3ddd1', '#2a1b15', '#8f2d1c', '#1c6436'],
    swatchDark: ['#651e14', '#f8f0e4', '#f4c24f', '#aee8c4'],
  },
  {
    value: 'maidan',
    label: 'Maidan',
    tagline:
      'The cricket ground. Outfield green, a painted-tin scoreboard of black number plates, a worm chart of spend against par, and a red-ball add button.',
    swatch: ['#2a7436', '#141a16', '#b81d24', '#1c7230'],
    swatchDark: ['#0d2217', '#fff1cf', '#ff5aa8', '#5fe07f'],
  },
  {
    value: 'sector',
    label: 'Sector',
    tagline:
      'Concrete and enamel. Flush colour blocks on a jointed grid, one heavy grotesque at poster scale, and accounts drawn as a façade whose bays are sized by their balances.',
    swatch: ['#c8c4ba', '#141413', '#f5c400', '#1846b0'],
    swatchDark: ['#161617', '#f1eee7', '#eab700', '#2450c0'],
  },
  {
    value: 'daak',
    label: 'Daak',
    tagline:
      'India Post stationery. Inland-letter blue with pillar-box red, accounts as perforated stamps, a struck date postmark on the hero and a carmine rubber stamp on a budget near its limit.',
    swatch: ['#c5dcef', '#13213d', '#c4201b', '#3d5412'],
    swatchDark: ['#0d1730', '#d3e4f5', '#f0473c', '#a9c46a'],
  },
  {
    value: 'sargam',
    label: 'Sargam',
    tagline:
      'The audio cassette. A five-stripe run that turns the corner of every label, Michroma faceplate lettering, piano-key controls, equaliser-strip budgets and a tape window whose reels show the month.',
    swatch: ['#ecebe6', '#3a2315', '#f26b1d', '#188078'],
    swatchDark: ['#1a1310', '#f4eadc', '#ff8a3a', '#31b7a8'],
  },
  {
    value: 'takhti',
    label: 'Takhti',
    tagline:
      'Blackboard and chalk (whiteboard and marker by day): the hero writes out the real sum, days are tallied in gates of five, and budgets are hand-hatched bars.',
    swatch: ['#f4f7f9', '#15171c', '#1650cf', '#cf222e'],
    swatchDark: ['#1d2b25', '#f2efe6', '#f7d75e', '#ff93a3'],
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
  parchi: { light: '#e3ddcf', dark: '#0b0d13' },
  mahal: { light: '#f4ecdc', dark: '#0c1f22' },
  ganak: { light: '#e7e1d3', dark: '#16171a' },
  akhbaar: { light: '#f2efe6', dark: '#17181b' },
  khel: { light: '#e0efbe', dark: '#0d0b1e' },
  tareekh: { light: '#fcfdfe', dark: '#0a0f1f' },
  safar: { light: '#d9d8d1', dark: '#0c0e11' },
  naksha: { light: '#f6f9fc', dark: '#12397a' },
  ajrakh: { light: '#9a2b1f', dark: '#121a4e' },
  kolam: { light: '#e3ddd1', dark: '#651e14' },
  maidan: { light: '#2a7436', dark: '#0d2217' },
  sector: { light: '#c8c4ba', dark: '#161617' },
  daak: { light: '#c5dcef', dark: '#0d1730' },
  sargam: { light: '#ecebe6', dark: '#1a1310' },
  takhti: { light: '#f4f7f9', dark: '#1d2b25' },
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
