import {
  DAILY_LOG_HOUR,
  MAX_NOTIFICATIONS_PER_RUN,
  MAX_NOTIFY_LEAD_DAYS,
  NOTIFICATION_HORIZON_DAYS,
  NOTIFICATION_SYNC_TAG,
  NOTIFY_HOUR,
  selectDueNotifications,
  type ScheduledNotification,
} from '@/utils/notifications';
import { gc, type GoldenCase } from '../golden';

const NOW = new Date('2026-06-15T12:00:00.000Z').getTime();
const HOUR = 60 * 60 * 1000;

function entry(
  partial: Partial<ScheduledNotification> & Pick<ScheduledNotification, 'id'>,
): ScheduledNotification {
  return {
    kind: 'bill',
    fireAt: NOW - HOUR,
    expiresAt: NOW + HOUR,
    title: 'Bill due',
    body: '₹100',
    url: '/recurring',
    ...partial,
  };
}

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  out.push(
    gc('constants', [], {
      NOTIFICATION_SYNC_TAG,
      NOTIFICATION_HORIZON_DAYS,
      MAX_NOTIFY_LEAD_DAYS,
      NOTIFY_HOUR,
      DAILY_LOG_HOUR,
      MAX_NOTIFICATIONS_PER_RUN,
    }),
  );
  const schedule: ScheduledNotification[] = [
    entry({ id: 'bill:r1:2026-06-15', fireAt: NOW - HOUR }),
    entry({
      id: 'budget:cat-1:2026-06-01:near',
      kind: 'budget',
      fireAt: NOW - 5 * HOUR,
      url: '/budgets',
    }),
    entry({ id: 'credit:acc-1:2026-06-20', kind: 'credit', fireAt: NOW, title: 'Card due' }),
    entry({
      id: 'daily:log:2026-06-15',
      kind: 'daily',
      fireAt: NOW + HOUR,
      expiresAt: NOW + 3 * HOUR,
    }),
    entry({ id: 'expired', fireAt: NOW - 9 * HOUR, expiresAt: NOW - 1 }),
    entry({ id: 'expires-now', fireAt: NOW - 2 * HOUR, expiresAt: NOW }),
    entry({ id: 'tie-a', fireAt: NOW - 3 * HOUR }),
    entry({ id: 'tie-b', fireAt: NOW - 3 * HOUR }),
    entry({ id: 'old', fireAt: NOW - 30 * 24 * HOUR, expiresAt: NOW + 24 * HOUR }),
  ];
  const fired = [
    [],
    ['old'],
    ['old', 'budget:cat-1:2026-06-01:near'],
    ['tie-a', 'old', 'bill:r1:2026-06-15'],
    schedule.map((s) => s.id),
  ];
  for (const f of fired) {
    for (const now of [NOW, NOW - 4 * HOUR, NOW + 2 * HOUR, NOW + 100 * HOUR, 0]) {
      out.push(
        gc(
          'selectDueNotifications',
          [schedule, f, now],
          selectDueNotifications(schedule, new Set(f), now),
        ),
      );
    }
  }
  const many = Array.from({ length: 10 }, (_, i) =>
    entry({ id: `e${i}`, fireAt: NOW - (10 - i) * HOUR }),
  );
  out.push(
    gc('selectDueNotifications', [many, [], NOW], selectDueNotifications(many, new Set(), NOW)),
  );
  out.push(
    gc(
      'selectDueNotifications',
      [[...many].reverse(), ['e0'], NOW],
      selectDueNotifications([...many].reverse(), new Set(['e0']), NOW),
    ),
  );
  out.push(gc('selectDueNotifications', [[], [], NOW], selectDueNotifications([], new Set(), NOW)));
  return out;
}
