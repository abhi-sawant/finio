import { useEffect } from 'react';
import { useFinanceStore } from '@/store/useFinanceStore';

// index.html carries one theme-color per scheme; the dark one is the indigo field, which would
// leave a lit bar above a true-black AMOLED page in the installed PWA.
const DARK_THEME_COLOR = '#1b1546';
const AMOLED_THEME_COLOR = '#000000';

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const theme = useFinanceStore((s) => s.settings.theme);
  const amoledDark = useFinanceStore((s) => s.settings.amoledDark);

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
    document
      .querySelector('meta[name="theme-color"][media*="dark"]')
      ?.setAttribute('content', amoledDark ? AMOLED_THEME_COLOR : DARK_THEME_COLOR);
  }, [amoledDark]);

  return <>{children}</>;
}
