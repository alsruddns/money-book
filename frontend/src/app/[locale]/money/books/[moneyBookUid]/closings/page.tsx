import { Suspense } from "react";
import { notFound } from "next/navigation";
import MonthlyClosingView from "@/closing/components/MonthlyClosingView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";
export default async function MonthlyClosingsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params; const uid = parseMoneyBookUid(moneyBookUid); if (uid === null) notFound();
  return <Suspense fallback={<p role="status">결산 정보를 불러오는 중...</p>}><MonthlyClosingView moneyBookUid={uid} /></Suspense>;
}
