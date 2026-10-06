import { formatMoney } from "@/common/format/money";
import type { MonthlyBudgetResponse } from "../dto/res/MonthlyBudgetResponse";
import BudgetProgress from "./BudgetProgress";

export default function BudgetOverview({ budget }: { budget: MonthlyBudgetResponse }) {
  return <div className="space-y-6">
    <section className="grid gap-3 sm:grid-cols-3">
      <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">총 예산</p><p className="mt-2 text-xl font-semibold">{budget.totalBudget === null ? "미설정" : formatMoney(budget.totalBudget)}</p></div>
      <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">실제 지출</p><p className="mt-2 text-xl font-semibold">{formatMoney(budget.totalExpense)}</p></div>
      <div className="rounded-xl border border-zinc-200 bg-white p-4"><p className="text-sm text-zinc-600">남은 예산</p><p className="mt-2 text-xl font-semibold">{budget.remainingBudget === null ? "총 예산 미설정" : budget.overBudget ? `${formatMoney(Math.abs(budget.remainingBudget))} 초과` : formatMoney(budget.remainingBudget)}</p></div>
    </section>
    <section className="rounded-xl border border-zinc-200 bg-white p-5"><BudgetProgress usageRate={budget.usageRate} overBudget={budget.overBudget} /></section>
    <section>
      <h2 className="mb-3 text-lg font-semibold">카테고리별 예산</h2>
      {budget.categories.length === 0 ? <p className="rounded-xl border border-dashed border-zinc-300 bg-white p-5 text-sm text-zinc-600">설정된 카테고리 예산이 없습니다.</p> :
        <ul className="grid gap-3 lg:grid-cols-2">
          {budget.categories.map((category) => <li key={category.categoryUid} className="min-w-0 rounded-xl border border-zinc-200 bg-white p-4">
            <h3 className="font-semibold">{category.categoryName}</h3>
            <p className="mt-2 text-sm text-zinc-600">예산 {formatMoney(category.budgetAmount)} · 사용 {formatMoney(category.expenseAmount)}</p>
            <p className="mb-3 text-sm">{category.overBudget ? `${formatMoney(Math.abs(category.remainingAmount))} 초과` : `${formatMoney(category.remainingAmount)} 남음`}</p>
            <BudgetProgress usageRate={category.usageRate} overBudget={category.overBudget} />
          </li>)}
        </ul>}
    </section>
  </div>;
}
