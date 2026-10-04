/**
 * Size step for a NoteCard's headline figure: a crore-scale amount must shrink rather than run
 * under the thread or get clipped at the card edge.
 */
export function noteFigureClass(text: string): string {
  const len = text.length;
  if (len <= 8) return 'text-[2.75rem]';
  if (len <= 11) return 'text-[2.25rem]';
  return 'text-[1.75rem]';
}
