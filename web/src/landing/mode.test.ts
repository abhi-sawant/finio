import { describe, expect, it } from 'vitest';
import { hasUsedApp, shouldShowLanding } from './mode';

const base = { pathname: '/', standalone: false, entered: false, usedApp: false };

describe('shouldShowLanding', () => {
  it('shows the marketing page to a first-time visitor in a browser tab', () => {
    expect(shouldShowLanding(base)).toBe(true);
  });
  it('never shows it in the installed PWA', () => {
    expect(shouldShowLanding({ ...base, standalone: true })).toBe(false);
  });
  it('never shows it once the app has been set up', () => {
    expect(shouldShowLanding({ ...base, usedApp: true })).toBe(false);
  });
  it('stays in the app after "Open Finio" in the same tab', () => {
    expect(shouldShowLanding({ ...base, entered: true })).toBe(false);
  });
  it('leaves every deep link alone: share target, shortcuts, notification clicks', () => {
    for (const pathname of ['/add-transaction', '/share-target', '/budgets', '/privacy']) {
      expect(shouldShowLanding({ ...base, pathname })).toBe(false);
    }
  });
});

describe('hasUsedApp', () => {
  it('is false with nothing stored', () => {
    expect(hasUsedApp(null)).toBe(false);
  });
  it('is true once onboardedAt is set', () => {
    const raw = JSON.stringify({ version: 17, state: { settings: { onboardedAt: '2026-10-01' } } });
    expect(hasUsedApp(raw)).toBe(true);
  });
  it('is false for a current-version state that never finished onboarding', () => {
    const raw = JSON.stringify({ version: 17, state: { settings: {} } });
    expect(hasUsedApp(raw)).toBe(false);
  });
  it('is true for a pre-onboarding state, which the store backdates on load', () => {
    const raw = JSON.stringify({ version: 5, state: { accounts: [{ id: 'a' }] } });
    expect(hasUsedApp(raw)).toBe(true);
  });
  it('treats unreadable data as used: never hide someone’s app behind a marketing page', () => {
    expect(hasUsedApp('{not json')).toBe(true);
    expect(hasUsedApp('null')).toBe(true);
  });
});
