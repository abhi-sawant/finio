// Generates the Android app's vector assets from the web's own sources, so the two apps can never
// drift apart on an icon, the page rosette or the launcher mark:
//
//   1. ui/icons/LucideIcons.kt — every lucide icon the PWA uses (named imports from 'lucide-react'
//      anywhere in src/, the kebab-case keys of every *_ICON_MAP, every `icon: '…'` literal in
//      src/data, plus the few Android-only extras below), converted to stroke-only path data.
//   2. ui/mudra/PageRosetteData.kt — public/guilloche.svg (the body::before engraving) verbatim.
//   3. res/drawable/ic_launcher_{background,foreground,monochrome}.xml — the Mudra app mark, the
//      same geometry scripts/gen-icons.mjs rasterises for the PWA.
//
//   (from web/) node scripts/gen-android-icons.mjs
//
// Output is deterministic; rerun after adding an icon to the web app and commit the result.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const WEB = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const ANDROID_MAIN = path.resolve(WEB, '../android/app/src/main');
const KOTLIN = path.join(ANDROID_MAIN, 'kotlin/com/slowatcoding/finio/ui');
const LUCIDE = path.join(WEB, 'node_modules/lucide-react/dist/esm');

/** Icons the Android UI needs that the web gets elsewhere (Sonner's built-in toast glyphs). */
const ANDROID_EXTRAS = [
  'circle-check',
  'circle-x',
  'info',
  'triangle-alert',
  'check',
  'x',
  'delete',
  'calendar',
  'clock',
  'chevron-left',
  'chevron-right',
  'chevron-down',
  'chevron-up',
  'plus',
  'fingerprint-pattern',
];

// ───────────── 1. Collect every icon name the web uses ─────────────

function walk(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(p, out);
    else if (/\.(tsx?|mjs)$/.test(entry.name)) out.push(p);
  }
  return out;
}

const sources = walk(path.join(WEB, 'src')).map((f) => [f, fs.readFileSync(f, 'utf8')]);

const pascalNames = new Set();
const kebabNames = new Set(ANDROID_EXTRAS);
for (const [file, text] of sources) {
  for (const m of text.matchAll(/import\s*\{([^}]*)\}\s*from\s*['"]lucide-react['"]/g)) {
    for (let spec of m[1].split(',')) {
      spec = spec.trim();
      if (!spec || spec.startsWith('type ')) continue;
      const name = spec.split(/\s+as\s+/)[0].trim();
      if (/^Lucide(Icon|Props)$/.test(name)) continue;
      pascalNames.add(name);
    }
  }
  if (/_ICON_MAP/.test(text)) {
    for (const m of text.matchAll(/^\s*'?([a-z0-9-]+)'?\s*:\s*[A-Z][A-Za-z0-9]*,/gm)) kebabNames.add(m[1]);
  }
  if (file.includes(`${path.sep}data${path.sep}`)) {
    for (const m of text.matchAll(/\bicon:\s*'([a-z0-9-]+)'/g)) kebabNames.add(m[1]);
  }
}

// ───────────── 2. Resolve names to lucide icon modules ─────────────

/** PascalCase export name → icon module basename, from lucide-react's own export index. */
const exportToModule = new Map();
const index = fs.readFileSync(path.join(LUCIDE, 'lucide-react.mjs'), 'utf8');
for (const m of index.matchAll(/export \{([^}]*)\} from '\.\/icons\/([a-z0-9-]+)\.mjs'/g)) {
  for (const part of m[1].split(',')) {
    const alias = part.trim().split(/\s+as\s+/)[1];
    if (alias) exportToModule.set(alias.trim(), m[2]);
  }
}

/** Follows `export { default } from './house.mjs'` alias modules to the real icon. */
function canonicalModule(kebab) {
  let name = kebab;
  for (let i = 0; i < 4; i++) {
    const file = path.join(LUCIDE, 'icons', `${name}.mjs`);
    if (!fs.existsSync(file)) return null;
    const text = fs.readFileSync(file, 'utf8');
    const re = text.match(/export \{ default \} from '\.\/([a-z0-9-]+)\.mjs'/);
    if (!re) return name;
    name = re[1];
  }
  return name;
}

function iconNode(module) {
  const text = fs.readFileSync(path.join(LUCIDE, 'icons', `${module}.mjs`), 'utf8');
  const m = text.match(/const __iconNode = (\[[\s\S]*?\]);\n/);
  if (!m) throw new Error(`no __iconNode in ${module}`);
  return new Function(`return ${m[1]}`)();
}

const pascal = (kebab) =>
  kebab.replace(/(^|-)([a-z0-9])/g, (_, __, c) => c.toUpperCase());

/** module → set of Kotlin property names; alias kebab → module. */
const modules = new Map();
const kebabAliases = new Map();
const addModule = (module, propName) => {
  if (!modules.has(module)) modules.set(module, new Set());
  modules.get(module).add(propName);
};

const unresolved = [];
for (const name of pascalNames) {
  const module = exportToModule.get(name);
  if (!module) {
    unresolved.push(name);
    continue;
  }
  // `CalendarIcon`, `XIcon`, `CheckIcon` are the same icons as `Calendar`, `X`, `Check`.
  const prop = name.endsWith('Icon') && exportToModule.get(name.slice(0, -4)) === module
    ? name.slice(0, -4)
    : name;
  addModule(module, prop);
}
for (const kebab of kebabNames) {
  const module = canonicalModule(kebab);
  if (!module) {
    unresolved.push(kebab);
    continue;
  }
  addModule(module, pascal(kebab));
  if (kebab !== module) kebabAliases.set(kebab, module);
}
// Every module is also reachable under its own canonical PascalCase name.
for (const [module, props] of modules) props.add(pascal(module));
// A web PascalCase alias (`Home`) implies its kebab alias (`home`) for byKebab lookups.
for (const [module, props] of modules) {
  for (const prop of props) {
    const kebab = prop
      .replace(/([a-z])([A-Z0-9])/g, '$1-$2')
      .replace(/([0-9])([A-Z])/g, '$1-$2')
      .toLowerCase();
    if (kebab !== module && canonicalModule(kebab) === module) kebabAliases.set(kebab, module);
  }
}
if (unresolved.length) {
  console.error('Unresolved lucide names:', unresolved.join(', '));
  process.exit(1);
}

// ───────────── 3. Convert SVG elements to path data ─────────────

const n = (v) => {
  const x = Math.round(Number(v) * 1000) / 1000;
  return Object.is(x, -0) ? '0' : String(x);
};

function elementToPath([tag, a]) {
  switch (tag) {
    case 'path':
      return a.d;
    case 'circle':
    case 'ellipse': {
      const rx = Number(tag === 'circle' ? a.r : a.rx);
      const ry = Number(tag === 'circle' ? a.r : a.ry);
      const cx = Number(a.cx);
      const cy = Number(a.cy);
      return `M${n(cx - rx)} ${n(cy)}a${n(rx)} ${n(ry)} 0 1 0 ${n(2 * rx)} 0a${n(rx)} ${n(ry)} 0 1 0 ${n(-2 * rx)} 0`;
    }
    case 'rect': {
      const x = Number(a.x ?? 0);
      const y = Number(a.y ?? 0);
      const w = Number(a.width);
      const h = Number(a.height);
      let rx = a.rx !== undefined ? Number(a.rx) : a.ry !== undefined ? Number(a.ry) : 0;
      let ry = a.ry !== undefined ? Number(a.ry) : rx;
      rx = Math.min(rx, w / 2);
      ry = Math.min(ry, h / 2);
      if (!rx || !ry) return `M${n(x)} ${n(y)}h${n(w)}v${n(h)}h${n(-w)}Z`;
      return (
        `M${n(x + rx)} ${n(y)}h${n(w - 2 * rx)}a${n(rx)} ${n(ry)} 0 0 1 ${n(rx)} ${n(ry)}` +
        `v${n(h - 2 * ry)}a${n(rx)} ${n(ry)} 0 0 1 ${n(-rx)} ${n(ry)}` +
        `h${n(-(w - 2 * rx))}a${n(rx)} ${n(ry)} 0 0 1 ${n(-rx)} ${n(-ry)}` +
        `v${n(-(h - 2 * ry))}a${n(rx)} ${n(ry)} 0 0 1 ${n(rx)} ${n(-ry)}Z`
      );
    }
    case 'line':
      return `M${n(a.x1)} ${n(a.y1)}L${n(a.x2)} ${n(a.y2)}`;
    case 'polyline':
    case 'polygon': {
      const pts = a.points.trim().split(/[\s,]+/).map(Number);
      let d = '';
      for (let i = 0; i < pts.length; i += 2) d += `${i ? 'L' : 'M'}${n(pts[i])} ${n(pts[i + 1])}`;
      return tag === 'polygon' ? `${d}Z` : d;
    }
    default:
      throw new Error(`unsupported element <${tag}>`);
  }
}

const kstr = (s) => `"${s.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\$/g, '\\$')}"`;

const sortedModules = [...modules.keys()].sort();
const dataLines = sortedModules.map((module) => {
  const parts = iconNode(module).map((el) => {
    // '!' marks the rare element lucide fills with currentColor (palette dots), not just strokes.
    const filled = el[1].fill && el[1].fill !== 'none';
    return kstr(`${filled ? '!' : ''}${elementToPath(el)}`);
  });
  return `        ${kstr(module)} to arrayOf(${parts.join(', ')}),`;
});

const props = [];
for (const module of sortedModules) {
  for (const prop of [...modules.get(module)].sort()) props.push([prop, module]);
}
props.sort((a, b) => a[0].localeCompare(b[0]));
const seenProps = new Set();
const propLines = props
  .filter(([p]) => (seenProps.has(p) ? false : seenProps.add(p)))
  .map(([prop, module]) => `    val ${prop}: ImageVector get() = icon(${kstr(module)})`);

const aliasLines = [...kebabAliases.entries()]
  .sort((a, b) => a[0].localeCompare(b[0]))
  .map(([alias, module]) => `        ${kstr(alias)} to ${kstr(module)},`);

const lucideVersion = JSON.parse(
  fs.readFileSync(path.join(WEB, 'node_modules/lucide-react/package.json'), 'utf8'),
).version;

const kotlin = `// GENERATED by web/scripts/gen-android-icons.mjs from lucide-react ${lucideVersion} — do not edit.
// Rerun (from web/: node scripts/gen-android-icons.mjs) after the web app starts using a new icon.
@file:Suppress("unused")

package com.slowatcoding.finio.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import java.util.concurrent.ConcurrentHashMap

/**
 * Every lucide icon the PWA uses, as 24×24 stroke-based [ImageVector]s (stroke 2, round caps and
 * joins, black so \`Icon(tint = …)\` recolours them — lucide's \`currentColor\`).
 *
 * Look icons up by the same kebab-case name the web stores on categories, goals and people
 * ([byKebab]), or use a typed property. A thicker stroke (the active tab's 2.4) is
 * \`byKebab(name, strokeWidth = 2.4f)\`.
 */
object LucideIcons {
    /** Kebab-case icon name → path data. A leading '!' marks an element lucide also fills. */
    private val data: Map<String, Array<String>> = mapOf(
${dataLines.join('\n')}
    )

    /** Alternate names (lucide's own renames, e.g. home → house) → canonical icon. */
    private val aliases: Map<String, String> = mapOf(
${aliasLines.join('\n')}
    )

    private val cache = ConcurrentHashMap<String, ImageVector>()

    /** Every canonical kebab-case name available. */
    val names: Set<String> get() = data.keys

    /** The icon a kebab-case name refers to (canonical or alias), or null when it is not bundled. */
    fun byKebab(name: String, strokeWidth: Float = 2f): ImageVector? {
        val key = if (data.containsKey(name)) name else aliases[name] ?: return null
        return icon(key, strokeWidth)
    }

    private fun icon(name: String, strokeWidth: Float = 2f): ImageVector =
        cache.getOrPut(if (strokeWidth == 2f) name else "$name@$strokeWidth") {
            build(name, data.getValue(name), strokeWidth)
        }

    private fun build(name: String, paths: Array<String>, strokeWidth: Float): ImageVector {
        val builder = ImageVector.Builder(
            name = "lucide.$name",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        val ink = SolidColor(Color.Black)
        for (raw in paths) {
            val filled = raw.startsWith('!')
            builder.addPath(
                pathData = addPathNodes(if (filled) raw.substring(1) else raw),
                fill = if (filled) ink else null,
                stroke = ink,
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }

${propLines.join('\n')}
}
`;

fs.mkdirSync(path.join(KOTLIN, 'icons'), { recursive: true });
fs.writeFileSync(path.join(KOTLIN, 'icons/LucideIcons.kt'), kotlin);
console.log(`wrote LucideIcons.kt — ${sortedModules.length} icons, ${seenProps.size} properties`);

// ───────────── 4. The page rosette (public/guilloche.svg) ─────────────

const rosetteSvg = fs.readFileSync(path.join(WEB, 'public/guilloche.svg'), 'utf8');
const viewBox = rosetteSvg.match(/viewBox="([^"]+)"/)[1].split(/\s+/).map(Number);
const strokeWidth = Number(rosetteSvg.match(/<svg[^>]*stroke-width="([^"]+)"/)[1]);
const rosettePaths = [...rosetteSvg.matchAll(/<path d="([^"]+)" opacity="([^"]+)"\/>/g)];
const rosetteKotlin = `// GENERATED by web/scripts/gen-android-icons.mjs from web/public/guilloche.svg — do not edit.
package com.slowatcoding.finio.ui.mudra

/** The engraved rosette behind every screen (CSS \`body::before\`), path for path. */
internal object PageRosetteData {
    const val VIEWBOX = ${n(viewBox[2])}f
    const val STROKE_WIDTH = ${n(strokeWidth)}f
    val opacities = floatArrayOf(${rosettePaths.map((m) => `${n(m[2])}f`).join(', ')})
    val paths = arrayOf(
${rosettePaths.map((m) => `        ${kstr(m[1])},`).join('\n')}
    )
}
`;
fs.mkdirSync(path.join(KOTLIN, 'mudra'), { recursive: true });
fs.writeFileSync(path.join(KOTLIN, 'mudra/PageRosetteData.kt'), rosetteKotlin);
console.log(`wrote PageRosetteData.kt — ${rosettePaths.length} paths`);

// ───────────── 5. Launcher icon layers (port of scripts/gen-icons.mjs) ─────────────
//
// The adaptive icon canvas is 108dp; launchers mask it to as little as a 66dp circle. The tile
// (radial gradient + engraving) fills the background layer edge to edge, like the maskable PWA
// icon. The F + thread live in the foreground, scaled so the whole mark sits inside that circle.

const r1 = (v) => Math.round(v * 10) / 10;

function rosette(cx, cy, rings, base, step, petals) {
  const out = [];
  for (let k = 0; k < rings; k++) {
    const R = base + k * step;
    const amp = 7 + (k % 3) * 3;
    const pet = petals + (k % 2) * 6;
    let d = '';
    for (let i = 0; i <= 360; i += 2) {
      const t = (i * Math.PI) / 180;
      const r = R + amp * Math.sin(pet * t + k * 0.55);
      d += `${i ? 'L' : 'M'}${r1(cx + r * Math.cos(t))} ${r1(cy + r * Math.sin(t))}`;
    }
    out.push({ d: `${d}Z`, opacity: 0.9 - k * 0.08 });
  }
  return out;
}

const F_PATH =
  'M128 118h196a24 24 0 0 1 24 24v32a24 24 0 0 1-24 24H212v40h92a22 22 0 0 1 22 22v28a22 22 0 0 1-22 22h-92v56a24 24 0 0 1-24 24h-36a24 24 0 0 1-24-24V142a24 24 0 0 1 24-24z';

function roundedRect(x, y, w, h, r) {
  return (
    `M${r1(x + r)} ${r1(y)}h${r1(w - 2 * r)}a${r} ${r} 0 0 1 ${r} ${r}v${r1(h - 2 * r)}` +
    `a${r} ${r} 0 0 1 -${r} ${r}h${r1(-(w - 2 * r))}a${r} ${r} 0 0 1 -${r} -${r}v${r1(-(h - 2 * r))}` +
    `a${r} ${r} 0 0 1 ${r} -${r}z`
  );
}

// The 'full' variant's seven 26-unit windows (gen-icons.mjs) — the launcher is always ≥ 96px.
const THREAD = Array.from({ length: 7 }, (_, i) => {
  const count = 7;
  const gap = 16;
  const h = (512 - 2 * 70 - (count - 1) * gap) / count;
  return roundedRect(372, 70 + i * (h + gap), 26, h, 6);
}).join('');

// 512-unit icon space → 108dp canvas, and the mark scaled into the 66dp safe circle: its farthest
// point from the centre (the thread's corner, ≈234 units out) lands at 33dp.
const CANVAS = 108;
const MARK_SCALE = 33 / 234;
const MARK_OFFSET = CANVAS / 2 - 256 * MARK_SCALE;

const AAPT = 'xmlns:aapt="http://schemas.android.com/aapt"';
const header = '<?xml version="1.0" encoding="utf-8"?>\n<!-- GENERATED by web/scripts/gen-android-icons.mjs — do not edit. -->\n';

const background = `${header}<vector xmlns:android="http://schemas.android.com/apk/res/android"
    ${AAPT}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="512"
    android:viewportHeight="512">
    <!-- The ₹100-lavender tile, lit from the top-left like the coin. -->
    <path android:pathData="M0 0h512v512h-512z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="153.6"
                android:centerY="128"
                android:gradientRadius="486.4">
                <item android:offset="0" android:color="#FFA594F2" />
                <item android:offset="0.5" android:color="#FF6C57D6" />
                <item android:offset="1" android:color="#FF3D2BB0" />
            </gradient>
        </aapt:attr>
    </path>
    <!-- Faint white guilloche engraving at 16%. -->
${rosette(400, 120, 6, 70, 22, 14)
  .map(
    (p) =>
      `    <path android:pathData="${p.d}" android:strokeColor="#FFFFFFFF" android:strokeWidth="2.2" android:strokeAlpha="${(0.16 * p.opacity).toFixed(4)}" />`,
  )
  .join('\n')}
</vector>
`;

const markGroup = (threadPath) => `    <group
        android:scaleX="${MARK_SCALE.toFixed(5)}"
        android:scaleY="${MARK_SCALE.toFixed(5)}"
        android:translateX="${MARK_OFFSET.toFixed(4)}"
        android:translateY="${MARK_OFFSET.toFixed(4)}">
${threadPath}
        <path android:pathData="${F_PATH}" android:fillColor="#FFFFFFFF" />
    </group>`;

// One continuous colour-shift gradient through all seven windows.
const gradientThread = `        <path android:pathData="${THREAD}">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:type="linear"
                    android:startX="0"
                    android:startY="70"
                    android:endX="0"
                    android:endY="442">
                    <item android:offset="0" android:color="#FF4FE0A8" />
                    <item android:offset="0.5" android:color="#FF6FB4FF" />
                    <item android:offset="1" android:color="#FFD8CEFF" />
                </gradient>
            </aapt:attr>
        </path>`;

const foreground = `${header}<vector xmlns:android="http://schemas.android.com/apk/res/android"
    ${AAPT}
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${markGroup(gradientThread)}
</vector>
`;

// Themed (Material You) icons: the system tints a single-colour silhouette.
const monochrome = `${header}<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
${markGroup(`        <path android:pathData="${THREAD}" android:fillColor="#FFFFFFFF" />`)}
</vector>
`;

const drawable = path.join(ANDROID_MAIN, 'res/drawable');
fs.mkdirSync(drawable, { recursive: true });
fs.writeFileSync(path.join(drawable, 'ic_launcher_background.xml'), background);
fs.writeFileSync(path.join(drawable, 'ic_launcher_foreground.xml'), foreground);
fs.writeFileSync(path.join(drawable, 'ic_launcher_monochrome.xml'), monochrome);
console.log('wrote ic_launcher_{background,foreground,monochrome}.xml');
