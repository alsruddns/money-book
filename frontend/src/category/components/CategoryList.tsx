"use client";

import { useState } from "react";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useCategoryList } from "../hooks/useCategoryList";
import { useDeleteCategory } from "../hooks/useDeleteCategory";
import type { CategoryResponse } from "../dto/res/CategoryResponse";
import CategoryFormDialog from "./CategoryFormDialog";

export default function CategoryList({ moneyBookUid }: { moneyBookUid: number }) {
  const [dialog, setDialog] = useState<{ initial?: CategoryResponse; type: TransactionType } | null>(null);
  const { canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const { categories, isLoading, isError, errorMessage } = useCategoryList(moneyBookUid);
  const deletion = useDeleteCategory(moneyBookUid);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><h1 className="text-2xl font-semibold">카테고리</h1><p className="mt-1 text-sm text-zinc-600">수입과 지출 카테고리를 관리합니다.</p></div>
        {canCreate && <button type="button" onClick={() => setDialog({ type: "EXPENSE" })}
          className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">카테고리 추가</button>}
      </div>
      {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
      {isLoading ? <p role="status">카테고리를 불러오는 중...</p> : isError
        ? <p role="alert" className="text-red-600">{errorMessage}</p>
        : (["EXPENSE", "INCOME"] as const).map((type) => {
          const items = categories.filter((category) => category.transactionType === type);
          return <section key={type} className="space-y-3">
            <h2 className="text-lg font-semibold">{type === "INCOME" ? "수입" : "지출"} 카테고리</h2>
            {items.length === 0 ? <p className="rounded-xl border border-dashed border-zinc-300 bg-white p-5 text-sm text-zinc-600">등록된 카테고리가 없습니다.</p> :
              <div className="grid gap-3 sm:grid-cols-2">
                {items.map((category) => <article key={category.categoryUid} className="rounded-xl border border-zinc-200 bg-white p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div><h3 className="font-medium">{category.name}</h3><p className="mt-1 text-sm text-zinc-600">정렬 순서 {category.sortOrder}</p></div>
                    {(canUpdate || canDelete) && <div className="flex gap-2">
                      {canUpdate && <button type="button" onClick={() => setDialog({ initial: category, type })}
                        className="min-h-11 rounded-lg border border-zinc-300 px-3 text-sm">수정</button>}
                      {canDelete && <button type="button" disabled={deletion.isLoading}
                        onClick={() => void deletion.deleteCategory(category.categoryUid, category.name)}
                        className="min-h-11 rounded-lg border border-red-200 px-3 text-sm text-red-700 disabled:opacity-60">삭제</button>}
                    </div>}
                  </div>
                </article>)}
              </div>}
          </section>;
        })}
      {dialog && <CategoryFormDialog moneyBookUid={moneyBookUid} initial={dialog.initial} defaultType={dialog.type} onClose={() => setDialog(null)} />}
    </div>
  );
}
