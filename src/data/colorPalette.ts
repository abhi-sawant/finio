/**
 * Shared swatch palette for every color picker (categories, labels, goals, people) — the Mudra
 * set: rupee-denomination hues and deep jewel tones that read as icon/chip colours on both the
 * daylight note paper and the dark-mode indigo field. Accounts don't pick a colour; their tint
 * comes from the account type (see components/accounts/note.ts).
 *
 * Changing this only changes what the picker *offers*. It never touches colors already
 * saved on existing categories/labels/etc.
 */
export const COLOR_PALETTE: string[] = [
  '#4b36c7', // ₹100 lavender (accent)
  '#7562ec', // light lavender
  '#7a4fbf', // violet
  '#5b5689', // muted indigo
  '#2f7fd1', // sky
  '#0d6e8a', // ₹50 teal
  '#0f8f6a', // green
  '#0b7a55', // deep green
  '#5f8a12', // ₹20 green-yellow
  '#b8860b', // ₹200 gold
  '#b06a00', // amber
  '#a0522d', // ₹10 chocolate
  '#c24e2a', // rust
  '#e0418f', // pink
  '#c2185b', // raspberry
  '#b0125f', // ₹2000 magenta
  '#8a1550', // wine
  '#4d5640', // ₹500 stone
];
