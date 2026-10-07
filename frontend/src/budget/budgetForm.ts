import type { SaveBudgetRequest } from "./dto/req/SaveBudgetRequest";

export interface BudgetCategoryInput { categoryUid: number; amount: string }

function parseAmount(text: string): number | null | undefined {
  const value = text.trim().replaceAll(",", "");
  if (!value) return null;
  if (!/^(?:0|[1-9]\d*)(?:\.\d{1,2})?$/.test(value)) return undefined;
  const number = Number(value);
  return Number.isSafeInteger(Math.round(number * 100)) ? number : undefined;
}

export function validateBudgetForm(totalText: string, categoryInputs: BudgetCategoryInput[]):
  { request?: SaveBudgetRequest; error?: string } {
  const totalBudget = parseAmount(totalText);
  if (totalBudget === undefined) return { error: "총 예산은 0 이상의 금액으로 소수점 둘째 자리까지 입력해 주세요." };
  const categories: SaveBudgetRequest["categories"] = [];
  const seen = new Set<number>();
  for (const input of categoryInputs) {
    const amount = parseAmount(input.amount);
    if (amount === undefined || !Number.isSafeInteger(input.categoryUid) || seen.has(input.categoryUid)) {
      return { error: "카테고리 예산을 다시 확인해 주세요." };
    }
    seen.add(input.categoryUid);
    if (amount !== null) categories.push({ categoryUid: input.categoryUid, amount });
  }
  const categorySum = categories.reduce((sum, category) => sum + Math.round(category.amount * 100), 0);
  if (totalBudget !== null && categorySum > Math.round(totalBudget * 100)) {
    return { error: "카테고리 예산 합계는 총 예산을 초과할 수 없습니다." };
  }
  return { request: { totalBudget, categories } };
}
