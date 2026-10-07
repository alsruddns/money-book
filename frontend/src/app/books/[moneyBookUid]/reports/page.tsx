import { Suspense } from "react";
import { notFound } from "next/navigation";
import ReportView from "@/report/components/ReportView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";
export default async function ReportsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params; const uid = parseMoneyBookUid(moneyBookUid); if (uid === null) notFound();
  return <Suspense fallback={<p role="status">리포트를 불러오는 중...</p>}><ReportView moneyBookUid={uid} /></Suspense>;
}
