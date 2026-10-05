---
name: Finio
description: A privacy-first rupee ledger printed like a banknote — note-paper gradients, frosted glass, guilloche linework and denomination tints, with every figure legible first and ornamental second.
colors:
  primary: "#4b36c7"
  primary-foreground: "#ffffff"
  primary-gradient-light: "#7562ec"
  accent-tint: "#e9e4ff"
  accent-ink: "#3b2a96"
  positive: "#0b7a55"
  warning: "#9a6a00"
  destructive: "#b0125f"
  warning-band: "#ffe1ef"
  warning-band-ink: "#5c0d38"
  ink: "#1d1747"
  muted-ink: "#5b5689"
  background: "#f1eefb"
  card: "#fdfcff"
  secondary-fill: "#ebe7f8"
  paper-lavender: "#f1edff"
  paper-mint: "#ebf6f1"
  paper-peach: "#fff1e3"
  chart-magenta: "#c2185b"
  chart-gold: "#c48a12"
  chart-green: "#0f8f6a"
  chart-blue: "#2f7fd1"
  uv-primary: "#b9adff"
  uv-primary-foreground: "#15103d"
  uv-accent-tint: "#302874"
  uv-accent-ink: "#ddd6ff"
  uv-positive: "#5fe0ae"
  uv-warning: "#f2c55c"
  uv-destructive: "#ff7ab8"
  uv-warning-band: "#4a1534"
  uv-warning-band-ink: "#ffe1ee"
  uv-ink: "#eeeaff"
  uv-muted-ink: "#aaa4d8"
  uv-background: "#161236"
  uv-card: "#221d4d"
  uv-secondary-fill: "#2b2559"
  coin-ink: "#1d1747"
typography:
  display-money:
    fontFamily: "Unbounded Variable, Geist Variable, sans-serif"
    fontSize: "2.75rem"
    fontWeight: 600
    lineHeight: 1.05
    letterSpacing: "-0.03em"
  headline:
    fontFamily: "Unbounded Variable, Geist Variable, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 600
    letterSpacing: "-0.02em"
  money:
    fontFamily: "Unbounded Variable, Geist Variable, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 600
    letterSpacing: "-0.03em"
  title:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
  dialog-title:
    fontFamily: "Unbounded Variable, Geist Variable, sans-serif"
    fontSize: "1rem"
    fontWeight: 500
    lineHeight: 1
  body:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    fontFeature: "\"tnum\""
  row-value:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 600
    fontFeature: "\"tnum\""
  label:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
    letterSpacing: "normal"
rounded:
  chip: "5px"
  sm: "13.2px"
  md: "17.6px"
  lg: "22px"
  note: "25.3px"
  full: "9999px"
spacing:
  page-x-mobile: "12px"
  page-x-desktop: "32px"
  stack: "16px"
  stack-desktop: "24px"
  column-gap-desktop: "32px"
  card-padding: "16px"
  row-padding-y: "12px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"
    rounded: "{rounded.full}"
    padding: "0 16px"
    height: "36px"
  button-outline:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    height: "36px"
  button-destructive:
    textColor: "{colors.destructive}"
    rounded: "{rounded.full}"
    height: "36px"
  header-icon-button:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    size: "40px"
  input:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    height: "40px"
    padding: "4px 12px"
  card:
    backgroundColor: "{colors.card}"
    rounded: "{rounded.md}"
    padding: "{spacing.card-padding}"
  dialog:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.lg}"
    padding: "16px"
  fab-coin:
    textColor: "{colors.coin-ink}"
    rounded: "{rounded.full}"
    size: "56px"
  note-tile:
    rounded: "{rounded.md}"
    padding: "14px 34px 14px 14px"
    height: "108px"
  note-chip:
    rounded: "{rounded.chip}"
    width: "30px"
    height: "20px"
  nav-item-active:
    backgroundColor: "{colors.accent-tint}"
    textColor: "{colors.accent-ink}"
    rounded: "{rounded.full}"
    padding: "12px"
---

# Design System: Finio

Tokens live in [`web/src/index.css`](web/src/index.css) (`:root` for light, `.dark` for dark). Every value
in this file is taken from the shipped build; where a token is a gradient, the frontmatter records
its key stops and the full CSS value is in `.impeccable/design.json`.

## Overview

**Creative North Star: "Mudra" — the money looks like money.**

Finio is printed the way a rupee note is printed. Light mode is note paper in daylight: a fixed
lavender-to-mint-to-peach paper gradient, indigo ink, and the ₹100 lavender as the one accent.
Dark mode is the same note under a UV lamp: an indigo field, the engraving lit in lavender, and
security fibres that only fluoresce in the dark. The security-printing vocabulary — guilloche
rosettes, a windowed colour-shift thread, microprint, denomination tints — is the house material,
used on the surfaces that carry money and kept out of the reading.

The world lends type, palette and one signature move; it never takes the layout or the controls.
Lists, forms and settings stay quiet frosted-glass panels of plain rows in Geist. The banknote face
(Unbounded) is reserved for headline figures and titles, so ornament never competes with a figure
the user has to read. Every figure is legible first and ornamental second.

Light and dark are both first-class: every token has a hand-tuned pair, including every
denomination tint.

**Key Characteristics:**
- Fixed note-paper gradient behind every screen, with a faint engraved guilloche rosette top-right.
- Frosted glass cards and chrome: translucent white over the paper, white hairline, lavender-tinted shadow.
- Unbounded for headline money, page titles and dialog titles; Geist for everything read.
- One hero banknote per money screen (NoteCard), with a pointer-driven colour-shift thread.
- Accounts printed in their type's rupee denomination tint.
- Filled actions are a ₹100-lavender gradient; the add button is a single-hue lavender coin.

## Colors

Indigo ink on lavender note paper, one ₹100-lavender accent, and semantic colours borrowed from
other denominations (₹2000 magenta for danger, a mint green for income, amber for caution).
Dark-mode pairs carry the `uv-` prefix in the frontmatter.

### Primary
- **₹100 Lavender** (`primary`, dark: `uv-primary`): links, active tab, focus ring, caret, native
  control accents, selection tint (28% mix) and scrollbar thumb (35% mix). Filled buttons do not
  use it flat — they use the lavender gradient (`primary-gradient-light` → `primary`, 135deg),
  which is deep enough to carry white text in both modes.
- **Lavender Tint / Lavender Ink** (`accent-tint` / `accent-ink`, dark: `uv-accent-tint` /
  `uv-accent-ink`): the active sidebar item and hover fills.

### Secondary
- **Mint Ledger Green** (`positive`, dark: `uv-positive`): income, positive figures, "owes you".
- **Note Amber** (`warning`, dark: `uv-warning`): caution text (near a limit, due soon).
- **₹2000 Magenta** (`destructive`, dark: `uv-destructive`): overspend, negative balances,
  delete. Destructive buttons are a 10% (dark 20%) tint of it, not a solid fill.
- **Magenta Band** (`warning-band` / `warning-band-ink`, accent `#b0125f` / dark `#ff8cc2`): the
  one collapsed-alert band on a screen, a soft ₹2000 tint.

### Tertiary — denomination tints
Each account type is printed as a rupee note (`web/src/components/accounts/note.ts`). Tints are
135deg gradients with a matching ink; full light and dark values are in `--note-<value>` and the
sidecar.

| Account type | Denomination | Light (from → to, ink) | Dark (from → to, ink) |
| --- | --- | --- | --- |
| Bank account (`checking`) | ₹100 lavender | `#ece6ff` → `#d9cffc`, `#3b2a86` | `#3a2f86` → `#251c5c`, `#dcd3ff` |
| Savings | ₹500 stone | `#eef0e6` → `#d8dccb`, `#3d4630` | `#3a4030` → `#262a1f`, `#e2e6d2` |
| Credit card | ₹2000 magenta | `#ffe3f1` → `#fbc6e0`, `#8a1550` | `#6a1846` → `#43102d`, `#ffd0e6` |
| Fixed / recurring deposit | ₹200 yellow | `#fff4c4` → `#ffe48a`, `#6b4d00` | `#5e4a0c` → `#3c2f07`, `#ffe9a3` |
| Cash | ₹10 chocolate | `#f6e8dc` → `#e6c9b0`, `#5a3215` | `#4a2c18` → `#2f1c0f`, `#f6d9c0` |
| Wallet | ₹50 cyan | `#e0f6fb` → `#b8e8f2`, `#0d4f60` | `#0f4652` → `#0a2f37`, `#c4f1fa` |
| Investment | ₹20 green-yellow | `#f2f8d8` → `#dcebaa`, `#485a0c` | `#3f4c0f` → `#29320a`, `#e8f5b8` |

Charts use `primary`, `chart-magenta`, `chart-gold`, `chart-green`, `chart-blue` in light and
`#a495ff`, `#ff6fae`, `#f2c55c`, `#4fd6a2`, `#6fb4ff` in dark.

### Neutral
- **Indigo Ink** (`ink`, dark: `uv-ink`): all body text and headline figures.
- **Muted Indigo** (`muted-ink`, dark: `uv-muted-ink`): captions, labels, secondary row text.
- **Note Paper** (`background` under a fixed 172deg gradient `paper-lavender` → `paper-mint` →
  `paper-peach`; dark: `uv-background` under `#1b1546` → `#15113a` → `#171238` → `#22143a`, a low
  maroon UV glow only at the bottom edge).
- **Card White** (`card`, dark: `uv-card`): opaque surfaces — dialogs, popovers, inputs.
- **Secondary Fill** (`secondary-fill`, dark: `uv-secondary-fill`): track backgrounds, secondary
  buttons, muted fills.
- **Hairlines**: borders are ink at 10% (`rgb(29 23 71 / 0.1)`; dark `rgb(238 234 255 / 0.1)`),
  input strokes at 16%. Glass edges are white: 90% in light, 11% in dark.
- **Engraving**: the page rosette is `rgb(90 70 190 / 0.075)` (dark `rgb(170 150 255 / 0.09)`).

### Named Rules
**The One Lavender Rule.** Lavender is the only accent. Every other hue on screen is either a
semantic signal or a denomination tint that means an account type.

**The Denomination-by-Type Rule.** An account's note tint is decided by its type, never by its
user-chosen colour, so a credit card is always the ₹2000 magenta and a deposit always the ₹200
yellow. User colours stay on categories, goals and people.

**The Green-Means-Money-In Rule.** Income and positive figures are `positive`, never `primary`;
caution is `warning`. Lavender means "you can act here", not "good".

**The Semantic-Token Rule.** Components use the semantic tokens and the `--note-*`, `--glass-*`,
`--grad-*` variables; never raw hex or Tailwind palette colours (`bg-amber-100`).

## Typography

**Display Font:** Unbounded Variable (with Geist Variable, sans-serif)
**Body Font:** Geist Variable (with sans-serif)

**Character:** Unbounded is the wide banknote numeral — a printed face for the figure and the
title. Geist does all the reading. Tabular figures are on for the whole body so listed money aligns.

### Hierarchy
- **Display money** (600, 2.75rem, line-height 1.05, -0.03em, Unbounded): the hero figure on the
  banknote — "Safe to spend today", net balance on Accounts.
- **Headline** (600–700, 1.5rem, -0.02em, Unbounded): every `h1` page title, set globally.
- **Money** (600, -0.03em, Unbounded via `font-money`): headline figures below the hero — note
  tiles (1.125rem), total balance (1.25rem), month stats (1rem), number-pad display (1.875rem, 1.5rem
  past 10 characters).
- **Dialog title** (500, 1rem, line-height 1, Unbounded).
- **Title** (600, 1rem, Geist): section headings ("Where it sits", "Latest").
- **Body** (400, 0.875rem, Geist, tabular): row names and sentences. Row values are 600 Geist,
  never Unbounded.
- **Label** (500, 0.75rem, Geist, sentence case, muted): field labels, stat captions, "See all".

### Named Rules
**The Figure-First Rule.** Unbounded only on headline money and titles. Rows, lists and body
figures stay in Geist — a list of wide numerals reads as decoration.

**The Sentence-Case Label Rule.** Labels are sentence-case 0.75rem 500 in muted ink. No uppercase
tracked microlabels and no eyebrows above headings. The banknote microprint is ornament, not a
label (see Components).

## Layout

One centred content column, `max-w-5xl` (64rem), padded 12px on mobile and 32px from `lg`
(1024px). Every page is `<Header>` + `<Main>`; `Main` stacks at 16px (24px on desktop) and carries
160px bottom padding on mobile so content clears the tab bar and coin.

- **Mobile:** a glass bottom tab bar (5 tabs including Tools), with the 56px lavender coin fixed
  bottom-right, 5.5rem above the safe-area inset. The coin is hidden on Accounts, Tools and
  Settings, which have their own primary action.
- **Desktop (`lg`+):** a fixed 240px glass sidebar on the left (coin "F" mark, full-width gradient
  "Add Transaction", primary nav, "Tools" group); the content column is offset by it.
- **Dashboard grid:** at `lg` a two-column grid (32px column gap, 24px row gap). The hero note
  spans both columns; the alert band spans both; below, sections take explicit column and row
  placement. Source order equals reading order on mobile — desktop placement is done with grid
  lines, never by reordering the DOM.
- **Note tiles:** a horizontal snap strip on mobile (160px tiles, 12px gap, bleeding to the screen
  edge) that becomes a 3-across grid at `lg`.
- Content inside cards pads 16px; plain list rows pad 12px vertically.

## Elevation & Depth

Depth is layered glass over a printed paper, and every shadow is lavender-tinted in light mode
(pure black in dark). Cards sit on the paper as frosted panels (18px blur, 170% saturation, white
inner top highlight, a long soft drop shadow). Chrome — header on scroll, tab bar, sidebar — is a
stronger glass (24px blur, 180% saturation). Dialogs and popovers are opaque and float on the
strongest shadow. The hero banknote alone tilts in 3D.

### Shadow Vocabulary
- **Card** (`0 18px 36px -26px rgb(60 40 140 / 0.42)`; dark `0 20px 40px -26px rgb(0 0 0 / 0.7)`),
  always paired with `inset 0 1px 0` glass highlight: glass cards, number-pad display, alert band.
- **Float** (`0 28px 56px -26px rgb(40 26 110 / 0.5)`; dark `0 28px 56px -24px rgb(0 0 0 / 0.75)`):
  the hero note, dialogs, selected-option rings.
- **Primary glow** (`inset 0 1px 0 rgb(255 255 255 / 0.25), 0 10px 22px -10px rgb(75 54 199 / 0.65)`;
  dark `0 10px 24px -8px rgb(108 87 214 / 0.7)`): under gradient-filled buttons only. Success and
  danger glows exist for the matching gradient fills.
- **Coin** (`inset 0 1px 1px rgb(255 255 255 / 0.6), 0 10px 24px -6px rgb(108 87 214 / 0.7)`).
- **Tile** (`0 14px 28px -20px rgb(40 26 110 / 0.5)`): denomination note tiles.
- **Tab bar** (`0 -12px 30px -22px rgb(40 26 110 / 0.45)`): casts upward onto content.
- **Stock Tailwind scale** (`shadow-xs` … `shadow-2xl`, plus bare `shadow`): redefined in
  `index.css` `@theme` from `--shadow-tint` (lavender `60 40 140` in light, black in dark), so any
  utility shadow follows the Lavender-Shadow rule without a per-call-site token.

### Named Rules
**The Lavender-Shadow Rule.** Shadows in light mode are tinted indigo/lavender, never neutral grey.

**The Transparent-At-Rest Rule.** The header sits transparent on the paper and frosts over only
once content scrolls beneath it (scrollY > 4); a solid band across the top would hide the paper.

## Shapes

Soft, generous corners from a 22px base radius (`--radius: 1.375rem`): inputs and number keys at
13.2px (`sm`), cards, list containers and note tiles at 17.6px (`md`), dialogs and alert bands at
22px (`lg`), the hero note at 25.3px. Every button, icon button, nav item, tab indicator, coin and
progress track is fully round. The one sharp shape is the account note chip (30×20px, 5px) — a
miniature banknote with its windowed thread.

Recurring geometry: the guilloche rosette (procedural, `currentColor` strokes at 0.55px), and the
windowed thread — a vertical band masked into dashes, repeated at every scale (hero thread 9px
with 16/10px windows, tile thread 4px with 9/6px windows, chip thread 2px with 3/2px windows,
goal progress masked into 10/3px horizontal windows).

## Components

### Buttons
Pill-shaped and lit from above.
- **Shape:** fully round (9999px). Heights 24 / 28 / 36 / 44px (`xs`/`sm`/default/`lg`), icon
  buttons 32px, header icon buttons 40px.
- **Primary:** the lavender gradient (135deg, `primary-gradient-light` → `primary`; dark starts at
  `#7d6af2`) with white text and the primary glow; 14px 500 Geist, 16px horizontal padding.
  Hover brightens 110%; press nudges down 1px.
- **Outline:** strong glass fill with a white glass hairline and 12px backdrop blur; hover fills muted.
- **Destructive:** magenta at 10% (dark 20%) with magenta text — a tint, never a solid red slab.
- **Ghost / Secondary / Link:** muted hover fill; secondary fill; lavender underline-on-hover.
- **Focus:** ring-coloured border plus a 3px ring at 50%.
- **Header icon button:** 40px round outline button, 18px icon; a pressed toggle switches to the
  lavender gradient with white icon.

### Coin (add button)
A single-hue lavender coin lit from the top-left: radial gradient `#f8f5ff` (0–8%) → `#d8ceff` →
`#a594f2` → `#6c57d6` at 30% 25%, indigo ink `+` (26px, stroke 2.4), coin shadow. 56px on the
mobile FAB, 36px as the sidebar's "F" mark. Same in both modes. Long-press opens templates.

### Cards / Containers
- **Corner Style:** 17.6px (`md`).
- **Background:** frosted glass — white at 60% (dark: white at 6%) with 18px blur.
- **Border:** 1px white glass hairline (90%; dark 11%) and an inset white top highlight.
- **Shadow Strategy:** Card shadow (see Elevation).
- **Internal Padding:** 16px.
- A homogeneous list is **one** glass card divided by hairlines, holding plain rows — not a stack
  of cards, and no per-row icon medallions.

### Inputs / Fields
- **Style:** opaque card fill, 1px input stroke (ink 16%), 13.2px radius, 40px tall, 16px text on
  mobile / 14px from `md`.
- **Focus:** ring-coloured border plus a 3px lavender ring at 50%.
- **Error / Disabled:** magenta border with a 20% magenta ring; disabled is muted fill at 50% opacity.

### Number pad and PIN pad
The entry display is a `grad-surface` panel (light `#fefdff` → `#efe9ff` → `#e2f3ec`; dark
`#2c2468` → `#1e1a4c` → `#12302a`) at `md` radius, with the figure in the money face. Keys are
56px strong-glass tiles at `sm` radius with a white hairline and inset highlight, 20px 600 digits,
scaling to 95% on press. PIN dots differ by shape (solid disc vs 2px ring), not only colour; a
wrong PIN shakes for 320ms (none under reduced motion).

### Dialogs and popovers
Opaque card colour, 22px radius, 16px padding, 1px border ring, Float shadow, over the `--scrim`
token with a light backdrop blur. The scrim always *dims* — indigo at 24% in light, near-black at 60%
in dark — never a foreground-tinted wash that would lighten a dark page. Titles are in Unbounded,
sentence case. Opaque on purpose: forms are read, not admired. Dialog buttons are pills; dialogs
never override the 22px radius.

### Toasts
Sonner, top-centre, following the app theme (`theme` = the Settings value, incl. system). Each toast
is strong glass with a white hairline, the Float shadow and a 24px blur; text is ink, descriptions
muted. Type is carried by the icon colour only — success `positive`, error `destructive`, warning
`warning`, info lavender — never Sonner's built-in rich colours. The Undo/action button is a 28px
lavender-gradient pill. Toasts render above dialogs: nothing between `<body>` and the Toaster may
create a stacking context (the background rosette sits at `z-index: -1` for that reason).

### Navigation
- **Mobile tab bar:** strong glass, white top hairline, upward shadow. Inactive tabs muted;
  the active tab turns lavender, thickens its icon stroke (2 → 2.4) and shows a 16×4px gradient pill
  between icon and 12px 500 label.
- **Desktop sidebar:** 240px strong glass with right hairline, `fixed`, present on every app route
  — Layout routes and the full-screen ones (forms, Tools pages) via `DesktopShell`; only the auth
  and legal pages stand alone. Items are full-round 14px 500 rows;
  active is lavender tint with lavender ink; hover is muted at 60%. Group label "Tools" is a
  sentence-case muted label.
- **Header:** transparent at rest; glass with a hairline once scrolled. `h1` in Unbounded.

### App icon and favicon
A ₹100-lavender tile (radial `#a594f2 → #6c57d6 → #3d2bb0`, lit top-left like the coin) carrying
a heavy white "F" and, down its right side, the windowed colour-shift security thread
(`#4fe0a8 → #6fb4ff → #d8ceff`, one continuous gradient through the windows). Sizes ≥96px add
faint white guilloche engraving at 16%; the favicon (`favicon.svg`, 48px `.ico`, 64px) drops it to
stay crisp. The maskable and Apple icons are full-bleed with the mark inside the 80% safe zone.
Every size is rendered from one vector source by `scripts/gen-icons.mjs`; never hand-edit a PNG.

### Note chip
A 30×20px miniature banknote in the account type's denomination tint, with a 2px dashed thread
near its right edge in the note's ink at 40%. Leads every account row; archived accounts show it
greyscale at 50%.

### NoteCard (signature hero)
The one banknote per money screen (Dashboard "Safe to spend today", Accounts net balance).
- `grad-surface` fill, glass hairline, inset highlight, Float shadow, 25.3px radius; padding
  22/22/34px on mobile, 28/32/40px from `lg`.
- A guilloche rosette top-right in lavender at 26% (250px; at `lg` it becomes a 380px watermark
  window on the right half).
- A 9px windowed **security thread** on the right whose hue runs 150° → 200° → 255° and shifts by
  up to 140° as the pointer crosses the note; the note also tilts up to ±2°/±3° (perspective 900px,
  0.5s ease-out-expo). Touch input does not tilt; reduced motion removes the transform.
- A thin-film conic sheen over the note (soft-light in light, screen at 30% in dark).
- **Microprint**: 6px 600 tracked uppercase text repeating the privacy promise ("FINIO · KEPT ON
  THIS DEVICE · NOT A BANK · YOUR DATA STAYS YOURS") along the bottom edge at 36% ink,
  `aria-hidden`. It is engraving texture, not a label.
- **UV fibres**: 26 seeded curved strokes (cyan, pink, yellow) on the right half only, so none
  crosses a figure. Invisible in light; at 85% with a 2px glow in dark.
- Content keeps clear of the thread with 80px right padding on mobile.
- The budget-spent bar inside it is the **register**: a 115deg engraved hatch (light `#3d2bb8` /
  `#6b58e8`; dark `#8f7dff` / `#bdb1ff`), not a flat fill.

### Note tile
Each open account printed as its denomination: 108px minimum height, `md` radius, 14px padding
(34px right, clear of its thread), glass hairline and inset highlight, Tile shadow, a small
guilloche rosette bottom-right at 45%, and a 4px windowed thread in the note's ink at 35%. Content:
name (14px 600, two lines max), type label (12px at 80%), balance in the money face pinned to the
bottom. Press scales to 97%; focus is a 2px ring outline offset 2px.

### Thread progress
Goals and other toward-a-target bars fill with the colour-shift thread (light hsl 150/70/38 →
200/78/46 → 255/70/56; dark hsl 150/65/52 → 200/80/60 → 255/85/75) masked into 10px windows with
3px gaps, inside an 8px round muted track.

### Alert band
At most one surfaced alert per screen: the magenta band fill with band ink, glass hairline, Card
shadow, 22px radius, 16px padding, spanning the full grid width.

## Do's and Don'ts

### Do:
- **Do** put one NoteCard on a money screen and let it carry the ornament; keep everything below it
  quiet glass and plain rows.
- **Do** set headline money, page titles and dialog titles in Unbounded (`font-money` /
  `font-heading`), and every row, list and body figure in Geist with tabular figures.
- **Do** tint accounts by type through `noteStyle(type)` and `--note-*`, with each tint's ink.
- **Do** use `positive` for income and positive figures, `warning` for caution, `destructive` for
  overspend and deletion.
- **Do** use frosted glass for cards and chrome, opaque card colour for dialogs, popovers and inputs.
- **Do** keep every ornament `aria-hidden` and `pointer-events: none`, and keep fibres, rosettes and
  threads clear of figures.
- **Do** give every motion a reduced-motion fallback (the note tilt and PIN shake both stop).
- **Do** design both modes: every new token needs a light value and a hand-tuned dark (UV) value.
- **Do** offer `COLOR_PALETTE` from `web/src/data/colorPalette.ts` (18 Mudra swatches: denomination
  hues and deep jewel tones) in every colour picker. Accounts have no colour picker — their tint is
  the account type's denomination.

### Don't:
- **Don't** put a large denomination numeral on a note tile or the hero. A ghost "2000" beside a real
  balance reads as money; the only numerals on a note are the user's figures.
- **Don't** use the account's user-chosen colour for its note tint.
- **Don't** use `primary` lavender for income or "good" states.
- **Don't** add uppercase tracked microlabels or eyebrow kickers above headings; labels are
  sentence case.
- **Don't** set list rows or body figures in Unbounded.
- **Don't** use neutral grey shadows in light mode, or solid filled red buttons for destructive actions.
- **Don't** give the header a solid background at rest.
- **Don't** use raw hex or Tailwind palette colours in components.
- **Don't** stack cards for a homogeneous list, or bring back per-row icon medallions.
