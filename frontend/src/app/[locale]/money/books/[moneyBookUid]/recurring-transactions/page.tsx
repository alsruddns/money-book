import { notFound } from "next/navigation";
import RecurringTransactionView from "@/recurring/components/RecurringTransactionView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function RecurringTransactionsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <RecurringTransactionView moneyBookUid={uid} />;
}
