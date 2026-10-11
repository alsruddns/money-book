"use client";

import { useState } from "react";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useCategoryList } from "../hooks/useCategoryList";
import { useCategoryTree } from "../hooks/useCategoryTree";
import { useDeleteCategory } from "../hooks/useDeleteCategory";
import type { CategoryLevel, CategoryResponse, CategoryTreeResponse } from "../dto/res/CategoryResponse";
import CategoryFormDialog from "./CategoryFormDialog";
import { useTranslation } from "@/i18n/useTranslation";

type CategoryDialog = { initial?: CategoryResponse; type: TransactionType; parentCategoryUid: number | null; level: CategoryLevel; parentName?: string };

export default function CategoryList({ moneyBookUid }: { moneyBookUid: number }) {
  const [dialog, setDialog] = useState<CategoryDialog | null>(null);
  const { canCreate, canUpdate, canDelete } = useMoneyBookPermission(moneyBookUid);
  const flat = useCategoryList(moneyBookUid);
  const tree = useCategoryTree(moneyBookUid);
  const deletion = useDeleteCategory(moneyBookUid);
  const { t } = useTranslation();

  function renderNode(node: CategoryTreeResponse, type: TransactionType) {
    const metadata = flat.categories.find((item) => item.categoryUid === node.categoryUid);
    const canAddChild = node.level !== "SMALL";
    const childLevel: CategoryLevel = node.level === "LARGE" ? "MEDIUM" : "SMALL";
    const childLabel = childLevel === "MEDIUM" ? "category.addMedium" : "category.addSmall";
    return <li key={node.categoryUid} className="space-y-2">
      <article className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-zinc-200 bg-white p-4">
        <div className="min-w-0">
          <h3 className="font-medium break-words">{node.name}</h3>
          {metadata && <p className="mt-1 text-sm text-zinc-600">{t("category.order").replace("{order}", String(metadata.sortOrder))}</p>}
        </div>
        {(canUpdate || canDelete || canCreate && canAddChild) && <div className="flex flex-wrap gap-2">
          {canCreate && canAddChild && <button type="button" onClick={() => setDialog({ type, parentCategoryUid: node.categoryUid, level: childLevel, parentName: node.name })}
            className="min-h-11 rounded-lg border border-blue-200 px-3 text-sm text-blue-800">+ {t(childLabel)}</button>}
          {canUpdate && metadata && <button type="button" onClick={() => setDialog({ type, parentCategoryUid: metadata.parentCategoryUid,
            level: metadata.level, initial: metadata, parentName: node.level === "LARGE" ? undefined : findParentName(tree.categories, metadata.parentCategoryUid) })}
            className="min-h-11 rounded-lg border border-zinc-300 px-3 text-sm">{t("common.edit")}</button>}
          {canDelete && <button type="button" disabled={deletion.isLoading}
            onClick={() => void deletion.deleteCategory(node.categoryUid, node.name, node.children.length > 0)}
            className="min-h-11 rounded-lg border border-red-200 px-3 text-sm text-red-700 disabled:opacity-60">{t("common.delete")}</button>}
        </div>}
      </article>
      {canCreate && node.level === "LARGE" && node.children.length === 0 && <p className="px-4 text-sm text-zinc-500">{t("category.noMedium")}</p>}
      {canCreate && node.level === "MEDIUM" && node.children.length === 0 && <p className="px-4 text-sm text-zinc-500">{t("category.noSmall")}</p>}
      {node.children.length > 0 && <ul className="ml-4 space-y-2 border-l-2 border-zinc-200 pl-3 sm:ml-6 sm:pl-4">
        {node.children.map((child) => renderNode(child, type))}
      </ul>}
    </li>;
  }

  return <div className="space-y-6">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <div><h1 className="text-2xl font-semibold">{t("category.title")}</h1><p className="mt-1 text-sm text-zinc-600">{t("category.description")}</p></div>
    </div>
    {deletion.errorMessage && <p role="alert" className="text-sm text-red-600">{deletion.errorMessage}</p>}
    {tree.isLoading || flat.isLoading ? <p role="status">{t("common.loading")}</p> : tree.isError || flat.isError
      ? <p role="alert" className="text-red-600">{tree.errorMessage ?? flat.errorMessage}</p>
      : (["EXPENSE", "INCOME"] as const).map((type) => {
        const roots = tree.categories.filter((node) => flat.categories.some((item) => item.categoryUid === node.categoryUid && item.transactionType === type));
        return <section key={type} className="space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-lg font-semibold">{type === "INCOME" ? t("transactions.income") : t("transactions.expense")} {t("category.title")}</h2>
            {canCreate && <button type="button" onClick={() => setDialog({ type, parentCategoryUid: null, level: "LARGE" })}
              className="min-h-11 rounded-lg border border-blue-200 px-3 text-sm text-blue-800">+ {t("category.add")}</button>}
          </div>
          {roots.length === 0 ? <p className="rounded-xl border border-dashed border-zinc-300 bg-white p-5 text-sm text-zinc-600">{t("category.empty")}</p>
            : <ul className="space-y-3">{roots.map((node) => renderNode(node, type))}</ul>}
        </section>;
      })}
    {dialog && <CategoryFormDialog moneyBookUid={moneyBookUid} initial={dialog.initial} defaultType={dialog.type}
      parentCategoryUid={dialog.parentCategoryUid} level={dialog.level} parentName={dialog.parentName} onClose={() => setDialog(null)} />}
  </div>;
}

function findParentName(roots: CategoryTreeResponse[], uid: number | null) {
  if (uid === null) return undefined;
  for (const root of roots) {
    if (root.categoryUid === uid) return root.name;
    const child = root.children.find((item) => item.categoryUid === uid || item.children.some((leaf) => leaf.categoryUid === uid));
    if (child) return child.name;
  }
  return undefined;
}
