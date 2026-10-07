import { Suspense } from "react";
import { notFound } from "next/navigation";
import TransactionList from "@/transaction/components/TransactionList";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function TransactionsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <Suspense fallback={<p role="status">거래 화면을 불러오는 중...</p>}>
    <TransactionList moneyBookUid={uid} />
  </Suspense>;
}
