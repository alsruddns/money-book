import MoneyBookActivityView from "@/activity/components/MoneyBookActivityView";
export default async function MoneyBookActivitiesPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params; const uid = Number(moneyBookUid);
  if (!Number.isSafeInteger(uid) || uid <= 0) return <p role="alert">올바르지 않은 가계부입니다.</p>;
  return <MoneyBookActivityView moneyBookUid={uid} />;
}
