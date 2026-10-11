"use client";

import { useState, type FormEvent } from "react";
import DialogShell from "@/common/components/DialogShell";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { CategoryResponse } from "../dto/res/CategoryResponse";
import type { CategoryLevel } from "../dto/res/CategoryResponse";
import { useCreateCategory } from "../hooks/useCreateCategory";
import { useUpdateCategory } from "../hooks/useUpdateCategory";
import { useTranslation } from "@/i18n/useTranslation";

export default function CategoryFormDialog({ moneyBookUid, initial, defaultType = "EXPENSE", parentCategoryUid = null,
  level = "LARGE", parentName, onClose }: {
  moneyBookUid: number; initial?: CategoryResponse; defaultType?: TransactionType; parentCategoryUid?: number | null;
  level?: CategoryLevel; parentName?: string; onClose: () => void;
}) {
  const [name, setName] = useState(initial?.name ?? "");
  const [transactionType, setTransactionType] = useState<TransactionType>(initial?.transactionType ?? defaultType);
  const [sortOrder, setSortOrder] = useState(String(initial?.sortOrder ?? 0));
  const create = useCreateCategory(moneyBookUid);
  const update = useUpdateCategory(moneyBookUid, initial?.categoryUid ?? 0);
  const isLoading = initial ? update.isLoading : create.isLoading;
  const errorMessage = initial ? update.errorMessage : create.errorMessage;
  const { t } = useTranslation();

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (isLoading) return;
    const order = sortOrder.trim() === "" ? Number.NaN : Number(sortOrder);
    const saved = initial
      ? await update.updateCategory({ name, sortOrder: order })
      : await create.createCategory({ name, transactionType, sortOrder: order, parentCategoryUid });
    if (saved) onClose();
  }

  return (
    <DialogShell title={initial ? t("category.editTitle") : t("category.createTitle")} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <p className="text-sm text-zinc-600">{t(`category.${initial?.level.toLowerCase() ?? level.toLowerCase()}`)}</p>
        {parentName && <p className="text-sm text-zinc-600">{t("category.parent")}: {parentName}</p>}
        <div>
          <label htmlFor="category-name" className="mb-1 block text-sm font-medium">{t("category.name")}</label>
          <input id="category-name" required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} autoFocus
            className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" />
        </div>
        {!initial && <div>
          <label htmlFor="category-type" className="mb-1 block text-sm font-medium">{t("category.type")}</label>
          <select id="category-type" value={transactionType} onChange={(event) => setTransactionType(event.target.value as TransactionType)}
            className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-2.5">
            <option value="EXPENSE">{t("transactions.expense")}</option><option value="INCOME">{t("transactions.income")}</option>
          </select>
        </div>}
        {initial && <p className="text-sm text-zinc-600">{t("category.type")}: {initial.transactionType === "INCOME" ? t("transactions.income") : t("transactions.expense")}</p>}
        <div>
          <label htmlFor="category-order" className="mb-1 block text-sm font-medium">{t("category.order").replace(" {order}", "")}</label>
          <input id="category-order" type="number" min="0" step="1" required value={sortOrder}
            onChange={(event) => setSortOrder(event.target.value)} className="w-full rounded-lg border border-zinc-300 px-3 py-2.5" />
        </div>
        {errorMessage && <p role="alert" className="text-sm text-red-600">{errorMessage}</p>}
        <button type="submit" disabled={isLoading} className="min-h-11 w-full rounded-lg bg-blue-600 px-4 font-medium text-white disabled:opacity-60">
          {isLoading ? t("category.saving") : t("category.save")}
        </button>
      </form>
    </DialogShell>
  );
}
