import {
  AUTO_LOCK_OPTIONS,
  DEFAULT_AUTO_LOCK_MINUTES,
  FREE_ATTEMPTS,
  autoLockLabel,
  formatLockoutCountdown,
  nextLockoutUntil,
  penaltyForAttempts,
  remainingLockoutMs,
  shouldLockOnResume,
} from '@/utils/appLock';
import { gc, type GoldenCase } from '../golden';

const NOW = new Date('2026-06-15T12:00:00.000Z').getTime();
const MINUTE = 60_000;

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(gc('constants', [], { AUTO_LOCK_OPTIONS, DEFAULT_AUTO_LOCK_MINUTES, FREE_ATTEMPTS }));

  const backgrounded: (number | null)[] = [
    null,
    NOW + MINUTE, // clock moved backwards
    NOW + 1,
    NOW,
    NOW - 1,
    NOW - (5 * MINUTE - 1000),
    NOW - (5 * MINUTE - 1),
    NOW - 5 * MINUTE,
    NOW - 6 * MINUTE,
    NOW - 60 * MINUTE,
    NOW - 60 * MINUTE + 1,
    0,
  ];
  for (const backgroundedAt of backgrounded) {
    for (const autoLockMinutes of [-1, 0, 1, 5, 15, 60]) {
      const args = { backgroundedAt, autoLockMinutes, now: NOW };
      out.push(gc('shouldLockOnResume', [args], shouldLockOnResume(args)));
    }
  }
  for (let i = -1; i <= 25; i += 1) out.push(gc('penaltyForAttempts', [i], penaltyForAttempts(i)));
  out.push(gc('penaltyForAttempts', [500], penaltyForAttempts(500)));
  for (const n of [0, 3, 4, 5, 6, 9, 10, 100]) {
    out.push(gc('nextLockoutUntil', [n, NOW], nextLockoutUntil(n, NOW)));
  }
  for (const until of [null, NOW - 1000, NOW, NOW + 1, NOW + 14_200, NOW + 300_000]) {
    out.push(gc('remainingLockoutMs', [until, NOW], remainingLockoutMs(until, NOW)));
  }
  for (const ms of [
    -61_000, -1000, -500, -1, 0, 1, 999, 1000, 1001, 14_200, 59_000, 59_001, 60_000, 61_000,
    299_999, 300_000, 599_999, 3_600_000, 6_000_000,
  ]) {
    out.push(gc('formatLockoutCountdown', [ms], formatLockoutCountdown(ms)));
  }
  for (const m of [-5, 0, 1, 2, 5, 15, 59, 60, 61, 120]) {
    out.push(gc('autoLockLabel', [m], autoLockLabel(m)));
  }
  return out;
}
