import { useEffect } from 'react';
import { useFinanceStore } from '@/store/useFinanceStore';
import { THEME_COLORS, useVisualThemeStore } from '@/store/useVisualThemeStore';

// index.html carries one theme-color per scheme; the dark one is the Mudra indigo field, which
// would leave a lit bar above a true-black AMOLED page in the installed PWA.
const AMOLED_THEME_COLOR = '#000000';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const theme = useFinanceStore((s) => s.settings.theme);
  const amoledDark = useFinanceStore((s) => s.settings.amoledDark);
  const visualTheme = useVisualThemeStore((s) => s.visualTheme);

  useEffect(() => {
    const root = document.documentElement;

    if (theme === 'dark') {
      root.classList.add('dark');
      return;
    }

    if (theme === 'light') {
      root.classList.remove('dark');
      return;
    }

    // system
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const update = () => {
      root.classList.toggle('dark', mq.matches);
    };
    update();
    mq.addEventListener('change', update);
    return () => mq.removeEventListener('change', update);
  }, [theme]);

  // `.dark.amoled` in index.css is the only consumer, so the class is harmless in light mode and
  // the setting survives a trip through it.
  useEffect(() => {
    document.documentElement.classList.toggle('amoled', amoledDark);
  }, [amoledDark]);

  // Mudra is the un-attributed default, so a landing visitor and the first paint need no attribute.
  useEffect(() => {
    const root = document.documentElement;
    if (visualTheme === 'mudra') delete root.dataset.visualTheme;
    else root.dataset.visualTheme = visualTheme;
  }, [visualTheme]);

  // Keep the installed PWA's status bar in step with whichever world is painted behind it.
  useEffect(() => {
    const colors = THEME_COLORS[visualTheme];
    document
      .querySelector('meta[name="theme-color"][media*="light"]')
      ?.setAttribute('content', colors.light);
    document
      .querySelector('meta[name="theme-color"][media*="dark"]')
      ?.setAttribute('content', amoledDark ? AMOLED_THEME_COLOR : colors.dark);
  }, [visualTheme, amoledDark]);

  return <>{children}</>;
}
