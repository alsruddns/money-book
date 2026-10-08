import { Suspense } from "react";
import { notFound } from "next/navigation";
import ExpenseRankingView from "@/report/components/ExpenseRankingView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function ExpenseRankingPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <Suspense fallback={<p role="status" className="rounded-xl border bg-white p-5">지출 순위를 불러오는 중...</p>}><ExpenseRankingView moneyBookUid={uid} /></Suspense>;
}
