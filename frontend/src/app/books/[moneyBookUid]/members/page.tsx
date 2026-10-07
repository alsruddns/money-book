import { notFound } from "next/navigation";
import MemberList from "@/moneybook/components/MemberList";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function MembersPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <MemberList moneyBookUid={uid} />;
}
