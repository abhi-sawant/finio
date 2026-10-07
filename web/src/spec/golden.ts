/**
 * Golden-fixture plumbing. Each `fixtures/<module>.fixture.ts` runs the real TS functions over a
 * grid of inputs; `golden.test.ts` writes the results to `spec/fixtures/<module>.json` at the repo
 * root, and the Android `:core` tests assert the Kotlin port returns the same outputs.
 *
 * Inputs and outputs go through `JSON.stringify`, so a `Date` becomes its ISO string, `undefined`
 * keys disappear and a `Map` must be converted with `Object.fromEntries` first. Keep every input
 * deterministic: pass a fixed `now`, never call `Math.random()` or `new Date()` without arguments.
 */
export interface GoldenCase {
  /** Name of the function under test, as exported from its module. */
  fn: string;
  /** A short tag so a failing Kotlin assertion names the case. */
  name?: string;
  args: unknown[];
  out: unknown;
}

export type FixtureBuilder = () => GoldenCase[] | Promise<GoldenCase[]>;

/** Build one case. `out` is computed eagerly by the caller. */
export function gc(fn: string, args: unknown[], out: unknown, name?: string): GoldenCase {
  return name ? { fn, name, args, out } : { fn, args, out };
}

/** `new Date(...)` in the pinned zone — readable fixture dates. */
export function at(y: number, m: number, d: number, h = 0, min = 0, s = 0, ms = 0): Date {
  return new Date(y, m - 1, d, h, min, s, ms);
}
