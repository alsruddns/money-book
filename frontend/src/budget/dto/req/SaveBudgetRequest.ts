export interface CategoryBudgetRequest { categoryUid: number; amount: number }

export interface SaveBudgetRequest {
  totalBudget: number | null;
  categories: CategoryBudgetRequest[];
}
