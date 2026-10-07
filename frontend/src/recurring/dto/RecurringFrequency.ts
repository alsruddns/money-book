export type RecurringFrequency = "MONTHLY" | "WEEKLY";

// Backend uses ISO weekdays: Monday=1 through Sunday=7.
export const recurringWeekdays = [
  { value: 1, label: "월요일" }, { value: 2, label: "화요일" }, { value: 3, label: "수요일" },
  { value: 4, label: "목요일" }, { value: 5, label: "금요일" }, { value: 6, label: "토요일" },
  { value: 7, label: "일요일" },
] as const;

export function recurringSchedule(frequency: RecurringFrequency, dayOfMonth: number | null, dayOfWeek: number | null): string {
  if (frequency === "MONTHLY") return `매월 ${dayOfMonth}일`;
  return `매주 ${recurringWeekdays.find((day) => day.value === dayOfWeek)?.label ?? "요일 미설정"}`;
}
