import { build } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import type { Plugin } from 'vite';

const PLACEHOLDER = '<!--landing-->';

/**
 * Renders the landing page to a string at build time and writes it into index.html's `#root`, so
 * crawlers (and the first paint) get real content with no JavaScript. It runs in
 * `transformIndexHtml`, which is before the service worker's precache manifest is computed — so
 * the precached index.html carries the landing markup and its revision changes with it.
 *
 * The page is compiled by a nested SSR build into node_modules/.cache, then imported and called.
 * Landing.tsx takes no CSS import (landing.css ships in the main stylesheet), so the SSR build
 * has no styles to deal with.
 */
export function landingPrerender(): Plugin {
  let root = process.cwd();
  return {
    name: 'finio-landing-prerender',
    apply: 'build',
    configResolved(config) {
      root = config.root;
    },
    async transformIndexHtml(html) {
      if (!html.includes(PLACEHOLDER)) return html;
      const outDir = path.resolve(root, 'node_modules/.cache/finio-landing-ssr');
      await build({
        root,
        configFile: false,
        logLevel: 'warn',
        plugins: [react()],
        resolve: { alias: { '@': path.resolve(root, 'src') } },
        build: {
          ssr: path.resolve(root, 'src/landing/entry-server.tsx'),
          outDir,
          emptyOutDir: true,
          write: true,
          copyPublicDir: false,
          rollupOptions: { output: { format: 'es', entryFileNames: 'entry-server.mjs' } },
        },
      });
      const mod = (await import(
        `${pathToFileURL(path.join(outDir, 'entry-server.mjs')).href}?t=${Date.now()}`
      )) as { render: () => string };
      return html.replace(PLACEHOLDER, mod.render());
    },
  };
}
