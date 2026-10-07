export interface MonthlyReportResponse {
  year: number; month: number; income: number; expense: number; balance: number; transactionCount: number;
  previousIncome: number; previousExpense: number; incomeChange: number; expenseChange: number;
  incomeChangeRate: number | null; expenseChangeRate: number | null; budgetConfigured: boolean;
  totalBudget: number | null; remainingBudget: number | null; budgetUsageRate: number | null; overBudget: boolean;
}
