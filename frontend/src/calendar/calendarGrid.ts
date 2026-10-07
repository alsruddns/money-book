import type { CalendarDayResponse } from "./dto/res/MonthlyCalendarResponse";

const weekdayIndex: Record<CalendarDayResponse["dayOfWeek"], number> = {
  SUNDAY: 0, MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6,
};

export type WeekStartDay = "SUNDAY" | "MONDAY";

export function buildCalendarGrid(days: CalendarDayResponse[], weekStartDay: WeekStartDay = "SUNDAY"): (CalendarDayResponse | null)[] {
  if (!days.length) return [];
  const startIndex = weekStartDay === "MONDAY" ? 1 : 0;
  const leading = (weekdayIndex[days[0].dayOfWeek] - startIndex + 7) % 7;
  const cells: (CalendarDayResponse | null)[] = [...Array<null>(leading).fill(null), ...days];
  const trailing = (7 - cells.length % 7) % 7;
  return [...cells, ...Array<null>(trailing).fill(null)];
}

export const weekdayLabels = ["일", "월", "화", "수", "목", "금", "토"];
export function getWeekdayLabels(weekStartDay: WeekStartDay): string[] {
  const offset = weekStartDay === "MONDAY" ? 1 : 0;
  return [...weekdayLabels.slice(offset), ...weekdayLabels.slice(0, offset)];
}
export function dayNumber(date: string): number { return Number(date.slice(8, 10)); }
