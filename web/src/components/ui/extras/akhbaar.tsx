const clampPct = (n: number) => Math.min(Math.max(n, 0), 100);

/**
 * Akhbaar: the halftone pace chart under the hero — budget used against month gone, each a bar of
 * printed dots, with one red rule standing at today across both. Restates the budget line above
 * it, so it is decorative to assistive tech.
 */
export function AkhbaarPace({ elapsed, spent }: { elapsed: number; spent: number }) {
  const used = clampPct(spent);
  const gone = clampPct(elapsed * 100);
  const behind = spent <= gone;
  const rows: { label: string; value: number; light?: boolean }[] = [
    { label: 'Budget used', value: used },
    { label: 'Month gone', value: gone, light: true },
  ];
  return (
    <div className="only-akhbaar akh-pace" aria-hidden>
      {rows.map((r) => (
        <div key={r.label} className="akh-pace-row">
          <span className="akh-pace-label">{r.label}</span>
          <div className="akh-pace-bar">
            <i
              className={r.light ? 'akh-dots akh-dots-light' : 'akh-dots'}
              style={{ width: `${r.value}%` }}
            />
            <u style={{ left: `${gone}%` }} />
          </div>
          <b>{Math.round(r.label === 'Budget used' ? spent : gone)}%</b>
        </div>
      ))}
      <p className="akh-pace-caption">
        {behind ? 'Spending runs behind the calendar.' : 'Spending runs ahead of the calendar.'}
      </p>
    </div>
  );
}
