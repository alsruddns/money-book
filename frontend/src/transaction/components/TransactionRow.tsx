"use client";

import type { TransactionResponse } from "../dto/res/TransactionResponse";
import { formatLocalDate } from "@/common/format/dateTime";
import { formatLocalizedAmount } from "@/common/format/money";
import { useTranslation } from "@/i18n/useTranslation";

export default function TransactionRow({ transaction, onSelect }: {
  transaction: TransactionResponse; onSelect: () => void;
}) {
  const { locale, t } = useTranslation();
  const isIncome = transaction.transactionType === "INCOME";
  return (
    <li>
      <button type="button" onClick={onSelect}
        className="flex w-full flex-col gap-2 rounded-xl border border-zinc-200 bg-white p-4 text-left hover:border-blue-300 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <p className="text-sm text-zinc-600">{formatLocalDate(transaction.transactionDate, "-", locale)} · {t(isIncome ? "transactions.income" : "transactions.expense")}</p>
          <p className="mt-1 font-medium break-words">{transaction.categoryName}</p>
          <p className="mt-1 text-sm text-zinc-600 break-words">{transaction.accountName}{transaction.memo ? ` · ${transaction.memo}` : ""}</p>
        </div>
        <span className={`shrink-0 text-lg font-semibold ${isIncome ? "text-blue-700" : "text-zinc-900"}`}>
          {isIncome ? "+" : "−"}{formatLocalizedAmount(Number(transaction.amount), locale)}
        </span>
      </button>
    </li>
  );
}
