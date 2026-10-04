---
version: 1
slug: "src-pages-dashboard-tsx"
primary_target: "src/pages/Dashboard.tsx"
related_targets: ["src/components/layout/Layout.tsx","src/index.css"]
---

# App shell + Dashboard (Mudra redesign)

Scope: the whole app's visual world, led by the Dashboard. Mode: Operate (people log and check money; the world lends type, palette, density and one signature move, never the layout or controls).

Audience/job: Indians tracking personal money on a phone PWA; glance at "safe to spend", log fast via the FAB, review monthly. Constraints: offline-first, INR only, hideAmounts must keep working, every existing route/behaviour preserved, light + dark both first-class.

User decisions (2026-10-04): Mudra chosen over Kaanch and Gulal; Unbounded on money + page/dialog titles, Geist for body and rows; account note tint is decided by account type; /design-prototype is removed once shipped; the add button is a single-hue ₹100-lavender gradient.

## Direction contract

THESIS: Finio's money looks like money. The screen is printed the way a rupee note is: guilloche linework, microprint, a windowed colour-shift thread, denomination tints. It refuses the category default of flat neutral cards with one accent, and the fintech default of neon-on-black.

OWN-WORLD: Light is note paper in daylight (lavender to mint to peach paper, indigo ink #1d1747, ₹100-lavender accent #4b36c7). Dark is the note under a UV lamp (indigo field #191440, engraving lit lavender, accent #b9adff). Frosted glass panels with white hairlines and lavender-tinted shadows; faint guilloche rosettes; denomination tints per account type; Unbounded for figures and titles.

STORY: The user opens the app and reads one banknote: what they can safely spend today and how much of the month's budget is gone. Below, their accounts sit like a fan of notes, then recent activity and the month. They trust it because every figure is legible first and ornamental second.

FIRST VIEWPORT: Header with name and the privacy line. Full-width hero note card: "Safe to spend today", the figure in Unbounded at ~46px, left/days line, hatched register bar for budget spent, microprint privacy band along its bottom edge, guilloche rosette top-right, colour-shift thread on the right. Below: one alert band, then "Where it sits" with the denomination-note account grid. Glass tab bar with the lavender coin FAB at the bottom.

FORM: Mudra (banknote security printing), my top-ranked grounded candidate (IMPECCABLE'S PICK) from seed 05218c5f; signature move: the security thread's hue shifts as the pointer moves over the hero note (tilt on touch-capable devices is not required).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
