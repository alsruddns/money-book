import { Suspense } from "react";
import { notFound } from "next/navigation";
import BudgetView from "@/budget/components/BudgetView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function BudgetsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <Suspense fallback={<p role="status">예산을 불러오는 중...</p>}><BudgetView moneyBookUid={uid} /></Suspense>;
}
