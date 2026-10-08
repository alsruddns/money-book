"use client";

import { useState } from "react";
import { useMoneyBookList } from "../hooks/useMoneyBookList";
import MoneyBookCard from "./MoneyBookCard";
import CreateMoneyBookDialog from "./CreateMoneyBookDialog";
import { useTranslation } from "@/i18n/useTranslation";

export default function MoneyBookList() {
  const { t } = useTranslation();
  const [isCreateOpen, setCreateOpen] = useState(false);
  const { moneyBooks, isLoading, isError, errorMessage } = useMoneyBookList();

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold">{t("books.title")}</h1>
          <p className="mt-1 text-sm text-zinc-600">{t("books.description")}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button type="button" onClick={() => setCreateOpen(true)}
            className="min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white hover:bg-blue-700">
            {t("books.create")}
          </button>
        </div>
      </div>
      {isLoading ? <p role="status">{t("books.loading")}</p> :
        isError ? <p role="alert" className="text-red-600">{errorMessage}</p> :
        moneyBooks.length === 0 ? (
          <div className="rounded-xl border border-dashed border-zinc-300 bg-white p-8 text-center">
            <p className="font-medium">{t("books.empty")}</p>
            <p className="mt-1 text-sm text-zinc-600">{t("books.emptyHint")}</p>
            <button type="button" onClick={() => setCreateOpen(true)}
              className="mt-5 min-h-11 rounded-lg bg-blue-600 px-4 text-sm font-medium text-white">
              {t("books.create")}
            </button>
          </div>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {moneyBooks.map((moneyBook) => <MoneyBookCard key={moneyBook.moneyBookUid} moneyBook={moneyBook} />)}
          </div>
        )}
      {isCreateOpen && <CreateMoneyBookDialog onClose={() => setCreateOpen(false)} />}
    </div>
  );
}
