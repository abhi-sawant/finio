import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

// The repo-root VERSION file is the single source: vite.config.ts inlines it as __APP_VERSION__
// and android/app/build.gradle.kts derives versionName/versionCode from it. package.json carries
// a copy for npm's sake, so keep the two from drifting.
describe('version', () => {
  it('package.json matches the repo-root VERSION file', () => {
    const version = readFileSync(new URL('../../../VERSION', import.meta.url), 'utf8').trim();
    const pkg = JSON.parse(readFileSync(new URL('../../package.json', import.meta.url), 'utf8'));
    expect(version).toMatch(/^\d+\.\d+\.\d+$/);
    expect(pkg.version).toBe(version);
  });
});
