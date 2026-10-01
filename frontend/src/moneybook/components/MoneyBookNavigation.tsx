"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { useMoneyBookPermission } from "../hooks/useMoneyBookPermission";

export default function MoneyBookNavigation({ moneyBookUid, children }: { moneyBookUid: number; children: ReactNode }) {
  const { moneyBook, isLoading, isError, errorMessage } = useMoneyBookPermission(moneyBookUid);
  if (isLoading) return <p role="status">가계부를 불러오는 중...</p>;
  if (isError) return <p role="alert" className="text-red-600">{errorMessage}</p>;
  if (!moneyBook) return <p role="alert">접근 가능한 가계부를 찾을 수 없습니다.</p>;

  const links = [
    ["거래", `/books/${moneyBookUid}/transactions`],
    ["카테고리", `/books/${moneyBookUid}/categories`],
    ["계좌/결제수단", `/books/${moneyBookUid}/accounts`],
    ["멤버", `/books/${moneyBookUid}/members`],
  ] as const;

  return (
    <div className="space-y-6">
      <div className="space-y-3 rounded-xl border border-zinc-200 bg-white px-4 py-4 sm:px-5">
        <Link href={`/books/${moneyBookUid}`} className="font-semibold break-words hover:text-blue-700">{moneyBook.name}</Link>
        <nav aria-label="가계부 기능" className="flex flex-wrap gap-2">
          {links.map(([label, href]) => (
            <Link key={href} href={href}
              className="inline-flex min-h-11 items-center rounded-lg border border-zinc-200 px-3 text-sm font-medium hover:border-blue-300 hover:text-blue-700">
              {label}
            </Link>
          ))}
        </nav>
      </div>
      {children}
    </div>
  );
}
