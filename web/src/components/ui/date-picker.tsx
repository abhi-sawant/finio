import * as React from 'react';
import { format, parse } from 'date-fns';
import { CalendarIcon } from 'lucide-react';

import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Calendar } from '@/components/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover';
import { formatShortDate } from '@/utils/formatters';

export interface DatePickerProps {
  /** Value in "YYYY-MM-DD" format. */
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  className?: string;
  disabled?: boolean;
  /** Earliest selectable day, "YYYY-MM-DD". */
  minDate?: string;
  /** Latest selectable day, "YYYY-MM-DD". */
  maxDate?: string;
}

function parseDateOnly(value: string): Date | null {
  if (!value) return null;
  const d = parse(value, 'yyyy-MM-dd', new Date());
  return Number.isNaN(d.getTime()) ? null : d;
}

export function DatePicker({
  value,
  onChange,
  placeholder = 'Pick a date',
  className,
  disabled,
  minDate,
  maxDate,
}: DatePickerProps) {
  const [open, setOpen] = React.useState(false);
  const date = parseDateOnly(value);
  const min = minDate ? parseDateOnly(minDate) : null;
  const max = maxDate ? parseDateOnly(maxDate) : null;
  const disabledDays = [...(min ? [{ before: min }] : []), ...(max ? [{ after: max }] : [])];

  const handleSelect = (selected: Date | undefined) => {
    if (!selected) return;
    onChange(format(selected, 'yyyy-MM-dd'));
    setOpen(false);
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger
        render={
          <Button
            variant="outline"
            disabled={disabled}
            className={cn(
              'border-input bg-card hover:bg-card h-10 w-full justify-start rounded-sm px-3 py-1 text-left text-base font-normal md:text-sm',
              !date && 'text-muted-foreground',
              className,
            )}
          >
            <CalendarIcon className="mr-2 size-4 shrink-0" />
            {date ? formatShortDate(date) : <span>{placeholder}</span>}
          </Button>
        }
      />
      <PopoverContent align="start" className="w-auto p-0">
        <Calendar
          mode="single"
          selected={date ?? undefined}
          onSelect={handleSelect}
          disabled={disabledDays.length ? disabledDays : undefined}
        />
      </PopoverContent>
    </Popover>
  );
}
