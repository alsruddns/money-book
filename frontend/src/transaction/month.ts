export interface SelectedMonth { year: number; month: number }

export function parseSelectedMonth(yearText: string | null, monthText: string | null, today = new Date()): SelectedMonth {
  const fallback = { year: today.getFullYear(), month: today.getMonth() + 1 };
  if (!yearText || !monthText || !/^\d{1,4}$/.test(yearText) || !/^\d{1,2}$/.test(monthText)) return fallback;
  const year = Number(yearText);
  const month = Number(monthText);
  if (year < 1 || year > 9999 || month < 1 || month > 12) return fallback;
  return { year, month };
}

export function shiftMonth(selected: SelectedMonth, offset: -1 | 1): SelectedMonth {
  const next = selected.year * 12 + selected.month - 1 + offset;
  const year = Math.floor(next / 12);
  if (year < 1 || year > 9999) return selected;
  return { year, month: next % 12 + 1 };
}
