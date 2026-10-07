import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { describe, expect, it } from 'vitest';
import type { FixtureBuilder } from './golden';

// `npm run gen:fixtures` sets UPDATE_FIXTURES=1 and rewrites spec/fixtures/*.json. A plain
// `npm test` instead fails when a committed fixture no longer matches what the TS code produces,
// so a change to money logic can't land without regenerating them (and the Android port then
// failing until it catches up — which is the point).
const UPDATE = process.env.UPDATE_FIXTURES === '1';
const OUT_DIR = fileURLToPath(new URL('../../../spec/fixtures/', import.meta.url));

// Lazy, so `vitest run src/spec -t <module>` only loads that one builder.
const builders = import.meta.glob<{ default: FixtureBuilder }>('./fixtures/*.fixture.ts');

describe('golden fixtures', () => {
  for (const [path, load] of Object.entries(builders)) {
    const name = path.replace('./fixtures/', '').replace('.fixture.ts', '');
    it(`${name} is current`, async () => {
      const cases = await (await load()).default();
      const json =
        JSON.stringify({ module: name, tz: process.env.TZ, cases }, null, 2) + '\n';
      const file = `${OUT_DIR}${name}.json`;
      if (UPDATE) {
        mkdirSync(OUT_DIR, { recursive: true });
        writeFileSync(file, json);
        return;
      }
      expect(existsSync(file), `${file} missing — run npm run gen:fixtures`).toBe(true);
      expect(readFileSync(file, 'utf8'), `${name}.json is stale — run npm run gen:fixtures`).toBe(
        json,
      );
    });
  }
});
