export interface DashboardResponse {
  year: number;
  month: number;
  summary: {
    totalIncome: number;
    totalExpense: number;
    balance: number;
    transactionCount: number;
    incomeCount: number;
    expenseCount: number;
  };
  comparison: {
    previousMonthIncome: number;
    previousMonthExpense: number;
    incomeChangeRate: number | null;
    expenseChangeRate: number | null;
  };
  categoryExpenses: Array<{
    categoryUid: number;
    categoryName: string;
    amount: number;
    transactionCount: number;
    ratio: number;
  }>;
  monthlyTrend: Array<{ year: number; month: number; income: number; expense: number; balance: number }>;
  budget: { totalBudget: number | null; actualExpense: number; remaining: number | null; usageRate: number | null; overBudget: boolean } | null;
  topExpenses: Array<{
    rank: number; transactionUid: number; transactionDate: string; categoryUid: number;
    categoryName: string; accountUid: number; accountName: string; memo: string | null; amount: number;
  }>;
}
