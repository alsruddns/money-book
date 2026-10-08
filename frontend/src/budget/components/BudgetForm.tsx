"use client";

import { useState, type FormEvent } from "react";
import Link from "../../common/components/MoneyLink";
import { useCategoryList } from "@/category/hooks/useCategoryList";
import type { MonthlyBudgetResponse } from "../dto/res/MonthlyBudgetResponse";
import { useSaveBudget } from "../hooks/useSaveBudget";

export default function BudgetForm({ moneyBookUid, year, month, budget, onSaved, onCancel }: {
  moneyBookUid: number; year: number; month: number; budget?: MonthlyBudgetResponse;
  onSaved: () => void; onCancel: () => void;
}) {
  const [totalText, setTotalText] = useState(budget?.totalBudget?.toString() ?? "");
  const [amounts, setAmounts] = useState<Record<number, string>>({});
  const { categories, isLoading, isError, errorMessage: categoryError } = useCategoryList(moneyBookUid, "EXPENSE");
  const { save, isLoading: isSaving, errorMessage } = useSaveBudget(moneyBookUid, year, month);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const input = categories.map((category) => ({
      categoryUid: category.categoryUid,
      amount: amounts[category.categoryUid] ?? budget?.categories.find((item) => item.categoryUid === category.categoryUid)?.budgetAmount.toString() ?? "",
    }));
    if (await save(totalText, input)) onSaved();
  }

  return <form onSubmit={submit} className="space-y-5 rounded-xl border border-zinc-200 bg-white p-5">
    <div>
      <label htmlFor="total-budget" className="block text-sm font-medium">총 예산</label>
      <input id="total-budget" type="text" inputMode="decimal" value={totalText} onChange={(event) => setTotalText(event.target.value)}
        placeholder="비우면 총 예산 미설정" className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
      <p className="mt-1 text-xs text-zinc-600">총 예산을 비워도 카테고리별 예산을 설정할 수 있습니다.</p>
    </div>
    <div>
      <h3 className="mb-3 font-semibold">지출 카테고리별 예산</h3>
      {isLoading ? <p role="status">카테고리를 불러오는 중...</p> : isError ? <p role="alert" className="text-red-600">{categoryError}</p> :
        categories.length === 0 ? <p className="text-sm text-zinc-600">등록된 지출 카테고리가 없습니다. <Link className="text-blue-700 underline" href={`/books/${moneyBookUid}/categories`}>카테고리 관리</Link></p> :
        <div className="grid gap-3 sm:grid-cols-2">
          {categories.map((category) => <label key={category.categoryUid} className="block text-sm font-medium">
            {category.name}
            <input type="text" inputMode="decimal" value={amounts[category.categoryUid] ?? budget?.categories.find((item) => item.categoryUid === category.categoryUid)?.budgetAmount.toString() ?? ""}
              onChange={(event) => setAmounts((current) => ({ ...current, [category.categoryUid]: event.target.value }))}
              placeholder="설정하지 않음" className="mt-1 min-h-11 w-full rounded-lg border border-zinc-300 px-3" />
          </label>)}
        </div>}
    </div>
    {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
    <div className="flex flex-wrap gap-2">
      <button type="submit" disabled={isSaving || isLoading || isError} className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white disabled:opacity-50">{isSaving ? "저장 중..." : "저장"}</button>
      <button type="button" onClick={onCancel} className="min-h-11 rounded-lg border border-zinc-300 px-4 text-sm">취소</button>
    </div>
  </form>;
}
