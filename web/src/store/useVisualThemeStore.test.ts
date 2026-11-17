import { describe, expect, it } from 'vitest';
import {
  DEFAULT_VISUAL_THEME,
  THEME_COLORS,
  VISUAL_THEMES,
  isVisualTheme,
} from './useVisualThemeStore';

describe('visual themes', () => {
  it('recognises exactly the themes it offers', () => {
    for (const t of VISUAL_THEMES) expect(isVisualTheme(t.value)).toBe(true);
    expect(isVisualTheme('neon')).toBe(false);
    expect(isVisualTheme(undefined)).toBe(false);
  });

  it('has a status-bar colour for every theme', () => {
    for (const t of VISUAL_THEMES) expect(THEME_COLORS[t.value]).toBeDefined();
  });

  it('keeps Mudra as the first, default theme', () => {
    expect(VISUAL_THEMES[0].value).toBe('mudra');
  });

  it('offers exactly Mudra, Gulal, Shunya, Bagh and Naksha', () => {
    expect(VISUAL_THEMES.map((t) => t.value)).toEqual([
      'mudra',
      'gulal',
      'shunya',
      'bagh',
      'naksha',
    ]);
  });

  it('does not recognise a retired theme, so its wearers fall back to Mudra', () => {
    const retired = [
      'khata',
      'parchi',
      'mahal',
      'ganak',
      'akhbaar',
      'khel',
      'tareekh',
      'safar',
      'ajrakh',
      'kolam',
      'maidan',
      'sector',
      'daak',
      'sargam',
      'takhti',
    ];
    for (const id of retired) expect(isVisualTheme(id)).toBe(false);
    expect(DEFAULT_VISUAL_THEME).toBe('mudra');
  });
});
