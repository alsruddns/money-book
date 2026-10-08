import { Suspense } from "react";
import { notFound } from "next/navigation";
import TransferView from "@/transfer/components/TransferView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function TransfersPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <Suspense fallback={<p role="status">이체를 불러오는 중...</p>}><TransferView moneyBookUid={uid} /></Suspense>;
}
