import { Suspense } from "react";
import { notFound } from "next/navigation";
import CalendarView from "@/calendar/components/CalendarView";
import { parseMoneyBookUid } from "@/moneybook/parseMoneyBookUid";

export default async function CalendarPage({ params }: { params: Promise<{ moneyBookUid: string }> }) {
  const { moneyBookUid } = await params;
  const uid = parseMoneyBookUid(moneyBookUid);
  if (uid === null) notFound();
  return <Suspense fallback={<p role="status">캘린더를 불러오는 중...</p>}><CalendarView moneyBookUid={uid} /></Suspense>;
}
