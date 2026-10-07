export default function BudgetProgress({ usageRate, overBudget }: { usageRate: number | null; overBudget: boolean }) {
  if (usageRate === null) return <p className="text-sm text-zinc-600">총 예산을 설정하면 사용률이 표시됩니다.</p>;
  const width = Math.min(100, Math.max(0, Number(usageRate) || 0));
  return <div>
    <div className="mb-1 flex justify-between gap-2 text-sm"><span>사용률</span><span>{usageRate}%{overBudget && " · 예산 초과"}</span></div>
    <div role="progressbar" aria-label="예산 사용률" aria-valuenow={Number(usageRate)} aria-valuemin={0} aria-valuemax={Math.max(100, Number(usageRate))}
      className="h-2.5 overflow-hidden rounded-full bg-zinc-200">
      <div className={`h-full rounded-full ${overBudget ? "bg-red-600" : "bg-blue-600"}`} style={{ width: `${width}%` }} />
    </div>
  </div>;
}
