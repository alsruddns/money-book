import type { CalendarDayResponse } from "./dto/res/MonthlyCalendarResponse";

const weekdayIndex: Record<CalendarDayResponse["dayOfWeek"], number> = {
  SUNDAY: 0, MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6,
};

export function buildCalendarGrid(days: CalendarDayResponse[]): (CalendarDayResponse | null)[] {
  if (!days.length) return [];
  const leading = weekdayIndex[days[0].dayOfWeek];
  const cells: (CalendarDayResponse | null)[] = [...Array<null>(leading).fill(null), ...days];
  const trailing = (7 - cells.length % 7) % 7;
  return [...cells, ...Array<null>(trailing).fill(null)];
}

export const weekdayLabels = ["일", "월", "화", "수", "목", "금", "토"];
export function dayNumber(date: string): number { return Number(date.slice(8, 10)); }
