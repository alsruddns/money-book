"use client";

import Link from "next/link";
import { useMoneyBookDetail } from "../hooks/useMoneyBookDetail";
import PermissionBadges from "./PermissionBadges";

export default function MoneyBookDetail({ moneyBookUid }: { moneyBookUid: number }) {
  const { moneyBook, isLoading, isError, errorMessage } = useMoneyBookDetail(moneyBookUid);

  if (isLoading) return <p role="status">가계부를 불러오는 중...</p>;
  if (isError) return <p role="alert" className="text-red-600">{errorMessage}</p>;
  if (!moneyBook) return <p role="alert">접근 가능한 가계부를 찾을 수 없습니다.</p>;

  return (
    <div className="space-y-6">
      <Link href="/books" className="text-sm font-medium text-blue-700 hover:underline">← 가계부 목록</Link>
      <section className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm sm:p-7">
        <div className="flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-semibold break-words">{moneyBook.name}</h1>
          {moneyBook.isOwner && <span className="rounded-full bg-amber-100 px-2.5 py-1 text-xs font-medium text-amber-900">소유자</span>}
        </div>
        <p className="mt-2 text-sm text-zinc-600">소유자 UID: {moneyBook.ownerUserUid}</p>
        <p className="mt-4 text-sm font-medium">내 권한</p>
        <div className="mt-2"><PermissionBadges permissions={moneyBook} /></div>
        <div className="mt-7 border-t border-zinc-200 pt-5">
          <Link href={`/books/${moneyBookUid}/members`}
            className="inline-flex min-h-11 items-center rounded-lg border border-zinc-300 px-4 text-sm font-medium hover:bg-zinc-50">
            멤버 보기
          </Link>
        </div>
      </section>
      <p className="text-sm text-zinc-500">거래와 통계 기능은 이후에 제공됩니다.</p>
    </div>
  );
}
