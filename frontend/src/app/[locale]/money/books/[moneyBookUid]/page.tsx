import { notFound } from "next/navigation";
import MoneyBookDetail from "@/moneybook/components/MoneyBookDetail";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function MoneyBookPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <MoneyBookDetail moneyBookUid={uid} />;
}
