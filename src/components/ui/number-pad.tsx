import { useEffect, useRef } from 'react';
import { Delete } from 'lucide-react';
import { formatInputAmount } from '@/utils/formatters';

interface NumberPadProps {
  value: string;
  onChange: (value: string) => void;
}

const BUTTONS = [
  ['7', '8', '9'],
  ['4', '5', '6'],
  ['1', '2', '3'],
  ['.', '0', '⌫'],
] as const;

export function NumberPad({ value, onChange }: NumberPadProps) {
  const handlePress = (key: string) => {
    if (key === '⌫') {
      onChange(value.slice(0, -1));
      return;
    }
    if (key === '.') {
      if (!value.includes('.')) onChange(value ? value + '.' : '0.');
      return;
    }
    const [intPart, dec] = value.split('.');
    if (dec !== undefined && dec.length >= 2) return;
    if (intPart.length >= 10 && dec === undefined) return;
    if (!value || value === '0') {
      onChange(key);
    } else {
      onChange(value + key);
    }
  };

  // Hardware keyboard support. The ref keeps the listener bound once while always seeing the
  // latest `value`/`onChange`.
  const pressRef = useRef(handlePress);
  useEffect(() => {
    pressRef.current = handlePress;
  });
  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey || e.metaKey || e.altKey) return;
      const t = e.target as HTMLElement | null;
      if (t && (t.closest('input, textarea, select') || t.isContentEditable)) return;
      if (e.key >= '0' && e.key <= '9' && e.key.length === 1) {
        e.preventDefault();
        pressRef.current(e.key);
      } else if (e.key === '.' || e.key === ',') {
        e.preventDefault();
        pressRef.current('.');
      } else if (e.key === 'Backspace') {
        e.preventDefault();
        pressRef.current('⌫');
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, []);

  const display = formatInputAmount(value);

  return (
    <div className="space-y-2">
      <div className="bg-grad-surface flex min-h-16 items-center justify-center rounded-md border border-[var(--glass-border)] px-4 py-3 shadow-[var(--shadow-card)]">
        <span
          className={`font-money transition-all ${
            display.length > 10 ? 'text-2xl' : 'text-3xl'
          } ${!value ? 'text-muted-foreground' : ''}`}
        >
          {display}
        </span>
      </div>

      <div className="grid grid-cols-3 gap-2">
        {BUTTONS.flat().map((btn) => (
          <button
            key={btn}
            type="button"
            onClick={() => handlePress(btn)}
            className="active:bg-muted flex h-14 items-center justify-center rounded-md border border-[var(--glass-border)] bg-[var(--glass-strong)] text-xl font-semibold shadow-[inset_0_1px_0_var(--glass-highlight)] transition-all select-none active:scale-95"
          >
            {btn === '⌫' ? (
              <Delete size={20} className="text-muted-foreground" />
            ) : btn === '.' ? (
              <span className="text-muted-foreground text-2xl leading-none">·</span>
            ) : (
              btn
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
