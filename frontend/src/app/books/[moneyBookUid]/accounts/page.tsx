import { notFound } from "next/navigation";
import AccountList from "@/account/components/AccountList";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function AccountsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <AccountList moneyBookUid={uid} />;
}
