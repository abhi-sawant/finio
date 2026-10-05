import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';

// Pinned to a zone east of UTC: transaction dates are UTC instants, so day/month bucketing
// bugs (a local-midnight entry filed under the previous UTC day) only show up there.
process.env.TZ = 'Asia/Kolkata';

// Deliberately separate from vite.config.ts: the unit suite covers pure money logic and
// has no need for the React, Tailwind or PWA plugins.
export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
