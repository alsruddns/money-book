export interface YearlyMonthResponse { month: number; income: number; expense: number; balance: number; transactionCount: number }
export interface YearlyReportResponse { year: number; totalIncome: number; totalExpense: number; balance: number; months: YearlyMonthResponse[] }
