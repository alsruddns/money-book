export interface CalendarDayResponse {
  date: string;
  dayOfWeek: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";
  weekend: boolean;
  holiday: boolean;
  holidayName: string | null;
  incomeAmount: number;
  expenseAmount: number;
  transferInAmount: number;
  transferOutAmount: number;
  transactionCount: number;
  transferCount: number;
  hasRecurringGeneratedTransaction: boolean;
}

export interface MonthlyCalendarResponse {
  year: number;
  month: number;
  days: CalendarDayResponse[];
}
