import MoneyBookSettingsView from "@/settings/components/MoneyBookSettingsView";

export default async function MoneyBookSettingsPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const parsedUid = Number(moneyBookUid);
  if (!Number.isSafeInteger(parsedUid) || parsedUid <= 0) return <p role="alert">올바르지 않은 가계부입니다.</p>;
  return <MoneyBookSettingsView moneyBookUid={parsedUid} />;
}
