export interface CategoryBudgetResponse {
  categoryUid: number;
  categoryName: string;
  budgetAmount: number;
  expenseAmount: number;
  remainingAmount: number;
  usageRate: number;
  overBudget: boolean;
}

export interface MonthlyBudgetResponse {
  configured: boolean;
  budgetUid: number | null;
  moneyBookUid: number;
  year: number;
  month: number;
  totalBudget: number | null;
  totalExpense: number;
  remainingBudget: number | null;
  usageRate: number | null;
  overBudget: boolean;
  categories: CategoryBudgetResponse[];
}
