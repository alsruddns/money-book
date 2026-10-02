"use client";
import { useMemo, useState } from "react";
import { useCategoryList } from "@/category/hooks/useCategoryList";
import { useAccountList } from "@/account/hooks/useAccountList";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useTransactionExport } from "@/export/hooks/useTransactionExport";
import { isValidSearchDateRange } from "@/transaction/search";

function localToday() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-${String(now.getDate()).padStart(2,"0")}`; }
function defaultStart() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,"0")}-01`; }
export default function DataExportSection({ moneyBookUid }: { moneyBookUid: number }) {
  const { canRead } = useMoneyBookPermission(moneyBookUid);
  const [startDate, setStartDate] = useState(defaultStart);
  const [endDate, setEndDate] = useState(localToday);
  const [type, setType] = useState<""|"INCOME"|"EXPENSE">("");
  const [categoryUid, setCategoryUid] = useState(""); const [accountUid, setAccountUid] = useState(""); const [keyword, setKeyword] = useState("");
  const categories = useCategoryList(moneyBookUid, type || undefined, canRead); const accounts = useAccountList(moneyBookUid, canRead);
  const { download, isLoading, errorMessage } = useTransactionExport();
  const dateValid = useMemo(() => isValidSearchDateRange(startDate, endDate), [startDate, endDate]);
  const categoryValid = categories.categories.some((category) => String(category.categoryUid) === categoryUid);
  async function run(format: "csv"|"xlsx") {
    if (!canRead || !dateValid || keyword.length > 200 || (categoryUid && !categoryValid)) return;
    await download({ moneyBookUid, startDate, endDate, ...(type ? { transactionType: type } : {}), ...(categoryUid ? { categoryUid: Number(categoryUid) } : {}), ...(accountUid ? { accountUid: Number(accountUid) } : {}), ...(keyword.trim() ? { keyword: keyword.trim() } : {}) }, format);
  }
  const invalid = !dateValid || keyword.length > 200 || Boolean(categoryUid && !categoryValid);
  return <section className="space-y-4 rounded-xl border border-zinc-200 bg-white p-5">
    <div><h2 className="text-lg font-semibold">데이터 내보내기</h2><p className="text-sm text-zinc-600">선택한 조건의 거래를 파일로 저장합니다. 기간은 최대 2년까지 선택할 수 있습니다.</p></div>
    <div className="grid gap-4 sm:grid-cols-2">
      <label className="space-y-1 text-sm">시작일<input aria-label="내보내기 시작일" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} className="min-h-11 w-full rounded-lg border px-3" /></label>
      <label className="space-y-1 text-sm">종료일<input aria-label="내보내기 종료일" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} className="min-h-11 w-full rounded-lg border px-3" /></label>
      <label className="space-y-1 text-sm">거래 구분<select value={type} onChange={(e) => { setType(e.target.value as typeof type); setCategoryUid(""); }} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체</option><option value="INCOME">수입</option><option value="EXPENSE">지출</option></select></label>
      <label className="space-y-1 text-sm">카테고리<select value={categoryUid} onChange={(e) => setCategoryUid(e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체</option>{categories.categories.map((c) => <option key={c.categoryUid} value={c.categoryUid}>{c.name}</option>)}</select></label>
      <label className="space-y-1 text-sm">계좌/결제수단<select value={accountUid} onChange={(e) => setAccountUid(e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체</option>{accounts.accounts.map((a) => <option key={a.accountUid} value={a.accountUid}>{a.name}</option>)}</select></label>
      <label className="space-y-1 text-sm">검색어<input value={keyword} maxLength={200} onChange={(e) => setKeyword(e.target.value)} className="min-h-11 w-full rounded-lg border px-3" placeholder="메모 등" /></label>
    </div>
    {!dateValid && <p role="alert" className="text-sm text-red-700">날짜를 확인해 주세요. 시작일은 종료일보다 늦을 수 없으며 조회 기간은 최대 2년입니다.</p>}
    {errorMessage && <p role="alert" className="text-sm text-red-700">{errorMessage}</p>}
    <div className="flex flex-wrap gap-3"><button disabled={!canRead || invalid || isLoading} onClick={() => run("csv")} className="min-h-11 rounded-lg bg-blue-700 px-5 font-medium text-white disabled:opacity-50">{isLoading ? "다운로드 준비 중..." : "CSV 다운로드"}</button><button disabled={!canRead || invalid || isLoading} onClick={() => run("xlsx")} className="min-h-11 rounded-lg border border-zinc-300 px-5 font-medium disabled:opacity-50">{isLoading ? "다운로드 준비 중..." : "Excel 다운로드"}</button></div>
    {(categories.isError || accounts.isError) && <p role="alert" className="text-sm text-red-700">필터 선택 항목을 불러오지 못했습니다.</p>}
  </section>;
}
