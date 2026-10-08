import { notFound } from "next/navigation";
import MoneyBookNavigation from "@/moneybook/components/MoneyBookNavigation";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function MoneyBookLayout({ children, params }: {
  children: React.ReactNode;
  params: Promise<{ moneyBookUid: string }>;
}) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <MoneyBookNavigation moneyBookUid={uid}>{children}</MoneyBookNavigation>;
}
