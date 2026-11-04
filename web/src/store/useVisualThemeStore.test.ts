import { describe, expect, it } from 'vitest';
import { THEME_COLORS, VISUAL_THEMES, isVisualTheme } from './useVisualThemeStore';

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
});
