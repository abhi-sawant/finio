/**
 * What a round-three theme's hero piece is given. One component per theme
 * (`extras/<theme>.tsx`, `<Theme>Extra`) is rendered in three places inside the Dashboard hero,
 * and returns null for the slots it does not use:
 *
 * - `scene`  — the hero's backdrop slot, before the label (absolutely positioned art);
 * - `figure` — straight after `.hero-fig`, for a piece that stands in for the figure (the theme
 *   then hides `.hero-fig` visually, keeping it for screen readers);
 * - `footer` — after the budget bar and its caption, only when something has been spent.
 *
 * Every piece is wrapped in `.only-<theme>` and is `aria-hidden`: it restates what the hero
 * already says in text.
 */
export interface HeroInfo {
  /** 0–1, how far through the budget period now is. */
  elapsed: number;
  /** 0–100+, share of the overall budget spent. */
  spentPercent: number;
  isOver: boolean;
  daysLeft: number;
  /** Whole days in the period, and which of them today is (1-based). */
  totalDays: number;
  dayOfPeriod: number;
  /** First day of the budget period (local midnight). */
  periodStart: Date;
  /** Formatted money, already honouring hide-amounts. */
  figure: string;
  spentText: string;
  limitText: string;
  remainingText: string;
}

export interface HeroExtraProps {
  slot: 'scene' | 'figure' | 'footer';
  info: HeroInfo;
}
