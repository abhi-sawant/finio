---
name: Finio
description: A calm, paper-like personal finance PWA with one deep-green accent, flat hairline-bordered surfaces and loud numbers.
colors:
  primary: "#146b54"
  primary-foreground: "#fffdf9"
  primary-dark-mode: "#34a582"
  accent-tint: "#e9f1ec"
  accent-ink: "#0f4c3d"
  destructive: "#b3421f"
  destructive-dark-mode: "#e0714a"
  warning-band: "#fbede6"
  warning-band-ink: "#7a2e13"
  paper: "#f7f5f1"
  card: "#fffdf9"
  ink: "#1b1a17"
  secondary-fill: "#f0ece3"
  muted-ink: "#6e695f"
  hairline: "#eae5db"
  night-paper: "#211e1a"
  night-card: "#2a2521"
  night-ink: "#f3efe7"
  night-muted-ink: "#b0a99c"
  chart-gold: "#c79b4f"
  chart-sage: "#6ba292"
  chart-slate: "#6e8fb0"
typography:
  title:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "1rem"
    fontWeight: 500
  body:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
  value:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 600
  label:
    fontFamily: "Geist Variable, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 500
rounded:
  sm: "9.6px"
  md: "12.8px"
  lg: "16px"
  xl: "22.4px"
  full: "9999px"
spacing:
  page-gutter: "12px"
  card-padding: "16px"
  row-y: "12px"
  section-gap: "16px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"
    rounded: "{rounded.full}"
    height: "36px"
    padding: "0 16px"
  button-outline:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.full}"
    height: "36px"
    padding: "0 16px"
  button-destructive:
    textColor: "{colors.destructive}"
    rounded: "{rounded.full}"
    height: "36px"
    padding: "0 16px"
  input:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.sm}"
    height: "40px"
    padding: "4px 12px"
  card:
    backgroundColor: "{colors.card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "16px"
---

# Design System: Finio

## Overview

**Creative North Star: "The Household Ledger"**

Finio looks like a ledger kept at home: warm cream paper, ink-dark text, ruled hairlines between entries and a single green mark where attention is needed. It is calm, flat and paper-like. Nothing glows, blurs or gradients; depth is a hairline border and a barely-there neutral shadow. The mood is private and trustworthy rather than exciting, which fits an app whose data never leaves the device by default.

Density is "tight rows, spacious sections." List rows are compact and share one container; sections breathe with generous page padding and a large bottom inset that keeps content clear of the tab bar and FAB. Numbers are the loudest thing on any screen (semibold, larger), captions are quiet (muted, smaller). Tactility is reserved for press: a 1px press-down on buttons and a slight scale on the main add action.

Dark mode is a warm charcoal "night paper," not an inversion. The accent is re-tuned brighter, borders become alpha-over-background, and every token has a hand-picked dark value.

**Key Characteristics:**
- One accent (deep green), used for the primary action, active state, focus ring and links only.
- Opaque cards with hairline borders; no gradients, glows or backdrop blur on surfaces.
- Warm neutrals (cream, charcoal) instead of clinical white, black or blue-gray.
- Semantic tokens only; components never hold raw hex.
- Pill buttons, softly rounded cards (roughly 13px), small rounded inputs (roughly 10px).
- One typeface (Geist Variable) for everything; hierarchy via size and weight.

## Colors

A warm paper-and-ink neutral field with a single forest-green accent and a rust red for danger. Hues are declared as CSS custom properties on `:root` and `.dark`, mapped into Tailwind's theme by reference.

### Primary
- **Ledger Green** (#146b54): primary buttons, active nav indicator, focus ring (`ring`), links, the FAB and the logo mark. In dark mode it becomes **Lamp Green** (#34a582) with near-black text (#0b1f19) on it, brighter and more saturated so it does not read as disabled.
- **Sage Tint** (#e9f1ec) with **Deep Ink Green** (#0f4c3d): the soft `accent` pair for hover and selected backgrounds and tinted icons. Dark: #24352e with #8fcbb3.

### Secondary
- **Rust** (#b3421f, dark #e0714a): `destructive`, negative balances and overspend. The destructive button is a 10% tint of this, not a solid red fill.
- **Warm Band** (#fbede6, text #7a2e13, accent #b3421f): the one bespoke token pair, for collapsed alert bands that are neither success nor destructive. Dark: #3a2a20, #e8c9b8, #e57a55.

### Neutral
- **Cream Paper** (#f7f5f1): page background. Dark: **Night Paper** (#211e1a).
- **Card Stock** (#fffdf9): cards, popovers, sidebar, tab bar. Dark: #2a2521.
- **Ledger Ink** (#1b1a17): foreground text. Dark: #f3efe7.
- **Oat** (#f0ece3): `secondary` and `muted` fills (chips, disabled inputs). Dark: #35302a.
- **Pencil Gray** (#6e695f): muted captions and secondary labels. Dark: #b0a99c.
- **Hairline** (#eae5db): borders and input outlines. Dark: `rgba(243,239,231,0.10)` borders, `0.14` inputs.
- **Chart set:** green #146b54, rust #b3421f, gold #c79b4f, sage #6ba292, slate #6e8fb0 (dark variants brighten each).

### Named Rules
**The One Voice Rule.** The green accent marks the single most important action or number on a screen. Three "important" colors means none are.

**The Hairline-Not-Shadow Rule.** Separation comes from a 1px hairline border or `divide-y`. Never from a heavier shadow or a tint.

**The Semantic Token Rule.** Components use `bg-card`, `text-muted-foreground`, `bg-warning-band` and the like. No raw hex, no ad-hoc Tailwind palette colors such as `bg-amber-100`.

## Typography

**Display Font:** Geist Variable (with sans-serif fallback), used for headings too (`--font-heading` maps to `--font-sans`)
**Body Font:** Geist Variable
**Label/Mono Font:** none; Geist throughout

**Character:** A single clean, slightly technical grotesque that stays out of the way of the numbers. Hierarchy is made with size and weight, never with a second face.

### Hierarchy
- **Hero value** (semibold or bolder, one bespoke larger size per page): the one total, e.g. total balance on the Dashboard.
- **Title** (500, 1rem): dialog and section titles, in the heading font.
- **Body** (400, 0.875rem): rows, form text, paragraphs.
- **Value** (600, 0.875rem, right-aligned): amounts in a row; flips to the destructive color when negative.
- **Label** (500, 0.75rem, muted): captions and form labels. Small section eyebrows may use uppercase with wide tracking.
- **Micro** (0.6875rem muted): secondary row caption under a value.

Stay inside Tailwind's default scale (`text-xs` to `text-2xl`). Inputs render at base size on mobile (to avoid iOS zoom) and `md:text-sm` on larger screens.

### Named Rules
**The Loud Number Rule.** A monetary value is semibold at a larger size than its muted caption. This pairing repeats everywhere a number appears.

## Layout

Every screen is a `Header` plus a `Main` from `src/components/ui/`, both in a centered `max-w-5xl` column so header and content align. Header is sticky with a 12px gutter (`px-3`, `lg:px-8`) and holds a title or back button plus zero to two icon actions. Main has `space-y-4` (`lg:space-y-6`) and very large bottom padding (`pb-40` mobile, `pb-8` desktop) so nothing sits under the fixed bottom nav or FAB.

Below `lg`, navigation is a fixed bottom tab bar (five items with a small active dot, plus "More") and a floating add button. From `lg` up, a 240px `Sidebar` carries the logo, a full-width pill "add" action and nav groups, and the tab bar disappears. Safe-area insets are respected (`safe-top`, `pb-safe`, `bottom-safe-nav`) and `overscroll-behavior: none` prevents bounce against fixed bars.

A single standalone item (a stat, a form section) is its own card with 16px padding. A homogeneous list is **one** card with `divide-y` and plain rows (`py-3`), never a stack of individually shadowed cards. Row anatomy: icon or avatar, a `min-w-0 flex-1` title (truncating) over a muted subtitle, and a right-aligned value over a micro caption.

### Named Rules
**The One Container Rule.** A list of similar things is one bordered container with dividers, not n cards.

## Elevation & Depth

Flat and tonal. At rest, surfaces are Card Stock on Cream Paper separated by a 1px hairline and `--shadow-card` (`0 1px 2px rgba(27,26,23,0.05)`). Only floating layers (dialogs, popovers, menus, selects) lift, using `--shadow-float` (`0 18px 40px -20px rgba(27,26,23,0.35)`) plus a `ring-1` in the border color. Dark mode uses the same shapes at higher opacity (0.4 and 0.6). Shadows are always neutral, never tinted with the accent.

Modal backdrops are translucent (`bg-foreground/15`) with a light `backdrop-blur-xs`; the dialog surface itself is fully opaque (`bg-popover`). Blur is reserved for backdrops only.

Legacy `bg-grad-*` and `shadow-glow-*` class names survive as a shim that resolves to flat fills and the float shadow. Do not use them in new work.

### Shadow Vocabulary
- **Card rest** (`box-shadow: 0 1px 2px rgba(27,26,23,0.05)`): every `card-elevated` surface.
- **Float** (`box-shadow: 0 18px 40px -20px rgba(27,26,23,0.35)`): dialogs, popovers, dropdowns, selects.

### Named Rules
**The Flat-By-Default Rule.** Surfaces are flat at rest. Only layers that float above the page get the float shadow.

## Shapes

One base `--radius: 1rem`; all steps derive from it: sm 9.6px, md 12.8px, lg 16px, xl 22.4px, 2xl 28.8px. Buttons and pill controls are fully round. Cards and dialogs use `rounded-md`; inputs, menu items and picker tiles use `rounded-sm`. User-chosen entities (category, goal, person) get a 36px circle tinted with the entity's own color. Never use arbitrary one-off radii; pick the nearest step.

## Components

### Buttons
- **Shape:** fully round pills (9999px); default height 36px (`h-9`), sizes xs 24, sm 28, default 36, lg 44; icon-only sizes are square (24/28/32/36).
- **Primary (`default`):** Ledger Green fill with Card Stock text; hover at 85% for link-style anchors.
- **Outline:** Card Stock fill, hairline border, Oat on hover.
- **Secondary / Ghost:** Oat fill, or transparent with Oat hover.
- **Destructive:** 10% rust tint with rust text (20% on hover); solid red only for the final confirm inside an alert dialog.
- **Interaction:** 1px press-down on active, a 3px 50%-opacity focus ring in the border-ring color identical across variants, disabled at half opacity with no pointer events, built-in invalid styling.

### Inputs / Fields
- **Style:** Card Stock fill, 1px hairline border, 10px radius, 40px tall, 12px horizontal padding. Label above in muted 12px medium, 6px gap.
- **Focus:** border shifts to the accent and a 3px 50% accent ring appears.
- **Error / Disabled:** destructive border with a 20% destructive ring; disabled turns Oat and loses pointer events at half opacity.
- **Amount entry:** the primary amount field uses a custom on-screen number pad rather than the OS keyboard, keeping layout stable.

### Cards / Containers
- **Corner Style:** about 13px (`rounded-md`).
- **Background:** Card Stock, opaque.
- **Shadow Strategy:** card rest only; see Elevation.
- **Border:** 1px hairline (`card-elevated`).
- **Internal Padding:** 16px; list rows 12px vertical.

### Navigation
- **Mobile:** fixed tab bar on Card Stock with a top hairline, icon plus 12px medium label, and a 4px green dot marking the active tab (transparent otherwise). A floating add button sits above it; long-press opens a template popover. Some pages hide the FAB where it would cover a primary action.
- **Desktop:** 240px sidebar on Card Stock with a right hairline, green circular "F" mark, full-width pill add action and grouped nav.

### Dialogs and Popovers
Centered, never a drawer, on mobile and desktop (`max-w-[calc(100%-2rem)]`, `sm:max-w-sm`). Fully opaque `bg-popover`, `ring-1` border color, float shadow, 100ms fade and zoom. Footer buttons stack with the primary on top on mobile and sit right-aligned on desktop. Confirmation uses one shared promise-based `useConfirm()`, never `window.confirm`.

### Switches
One `Switch` primitive (a real `button` with `role="switch"`): green track when on, Oat when off, a white thumb with a small shadow in both themes. A `SwitchField` lays out icon, title, description and switch as a settings row.

### Toasts
One Sonner instance at the top center, rich colors and a close button. Reversible actions get a toast with an inline **Undo**; irreversible ones get a confirm dialog beforehand, never both.

### Charts
Recharts using the five-step chart palette. Any chart that is the sole carrier of data is paired with a real table behind a "View data table" disclosure.

## Do's and Don'ts

### Do:
- **Do** use the accent only for the primary action, active states, focus ring and links (The One Voice Rule).
- **Do** reference semantic tokens (`bg-card`, `text-muted-foreground`, `bg-warning-band`) for every color.
- **Do** build every page from `Header` + `Main` and keep `Main`'s large bottom padding.
- **Do** render a list as one `card-elevated divide-y` container of plain rows.
- **Do** make amounts semibold and larger than their muted captions, and flip negatives to the destructive color.
- **Do** truncate titles inside a `min-w-0 flex-1` wrapper so the value column never gets pushed off screen.
- **Do** give every icon-only control an `aria-label`, and gate any non-essential animation behind `prefers-reduced-motion`.
- **Do** pick every color-picker swatch from the shared 18-swatch `COLOR_PALETTE`.
- **Do** keep transitions fast (about 100ms fade and scale); reserve a distinct animation (the wrong-PIN shake) for real error signals.

### Don't:
- **Don't** add gradients, glows or `backdrop-blur` to surfaces; blur belongs to modal backdrops only.
- **Don't** put raw hex or ad-hoc palette colors (`bg-amber-100`) in components.
- **Don't** stack individually shadowed cards for a homogeneous list, and don't add per-row icon medallions.
- **Don't** use native `window.confirm()` or a hand-rolled `role="switch"` span.
- **Don't** introduce one-off radii, a second typeface, or a second accent color.
- **Don't** use new `bg-grad-*` or `shadow-glow-*` classes; they exist only as a flat-fill shim.
- **Don't** invert colors for dark mode; hand-tune each token.
- **Don't** let a chart be the only carrier of its data.
