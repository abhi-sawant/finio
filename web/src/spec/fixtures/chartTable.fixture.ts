import { sampleForTable } from '@/utils/chartTable';
import { gc, type GoldenCase } from '../golden';

const range = (n: number) => Array.from({ length: n }, (_, i) => i);

export default function cases(): GoldenCase[] {
  const out: GoldenCase[] = [];
  for (const n of [0, 1, 2, 3, 5, 23, 24, 25, 30, 31, 47, 48, 49, 90, 91, 365, 366, 1000]) {
    // Items are the indices 0..n-1, so args carry just `n` (the Kotlin side rebuilds the list)
    // and every sampled row names the index it was taken from.
    const items = range(n);
    out.push(gc('sampleForTable', [n], sampleForTable(items)));
    for (const max of [-1, 0, 1, 2, 3, 5, 7, 12, 24, 100]) {
      out.push(gc('sampleForTable', [n, max], sampleForTable(items, max)));
    }
  }
  return out;
}
