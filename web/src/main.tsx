import { StrictMode } from 'react';
import { createRoot, hydrateRoot, type Root } from 'react-dom/client';
import './index.css';
import './landing/landing.css';
import { Landing } from './landing/Landing';
import { detectLanding, rememberEntered } from './landing/mode';

const container = document.getElementById('root')!;

// The app (and the store behind it) is a separate chunk: a first-time visitor reading the
// marketing page never downloads it, and returning users / the installed PWA load it at once.
async function mountApp(existing?: Root) {
  const [{ default: App }, { ThemeProvider }] = await Promise.all([
    import('./App.tsx'),
    import('@/components/ThemeProvider'),
  ]);
  document.documentElement.dataset.mode = 'app';
  (existing ?? createRoot(container)).render(
    <StrictMode>
      <ThemeProvider>
        <App />
      </ThemeProvider>
    </StrictMode>,
  );
}

if (detectLanding()) {
  let root: Root;
  const openApp = () => {
    rememberEntered();
    void mountApp(root);
  };
  const page = (
    <StrictMode>
      <Landing onOpenApp={openApp} />
    </StrictMode>
  );
  // The production build prerenders the landing page into #root; adopt that markup rather than
  // rebuilding it. In dev there is none, so render from scratch.
  if (container.querySelector('.lp')) {
    root = hydrateRoot(container, page);
  } else {
    root = createRoot(container);
    root.render(page);
  }
} else {
  void mountApp();
}
