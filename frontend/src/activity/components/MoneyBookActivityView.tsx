"use client";
import { useMemo, useState } from "react";
import { formatCount } from "@/common/format/money";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMoneyBookMembers } from "@/moneybook/hooks/useMoneyBookMembers";
import { activityTypes, activityTypeLabels, targetTypes, targetTypeLabels, formatActivityTime, getActivityMetadataHint, getActivityTypeLabel, getTargetTypeLabel } from "../activityLabels";
import { useMoneyBookActivities } from "../hooks/useMoneyBookActivities";
import type { MoneyBookActivityResponse } from "../dto/res/MoneyBookActivityResponse";

function ActivityItem({ activity }: { activity: MoneyBookActivityResponse }) {
  const time = formatActivityTime(activity.occurredAt);
  const hint = getActivityMetadataHint(activity.activityType, activity.metadataJson);
  const marker = activity.activityType.startsWith("TRANSACTION") ? "↔" : activity.activityType.startsWith("MEMBER") ? "♙" : activity.activityType.startsWith("BACKUP") ? "▣" : activity.activityType.startsWith("MONTH_") ? "✓" : activity.activityType.startsWith("BUDGET") ? "▥" : "•";
  return <li className="relative border-l-2 border-blue-200 pb-5 pl-5 last:border-l-transparent last:pb-0">
    <span aria-hidden="true" className="absolute -left-[9px] top-1 flex size-4 items-center justify-center rounded-full border-2 border-blue-600 bg-white text-[9px]">{marker}</span>
    <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1"><p className="font-medium">{activity.actorNickname}</p><time dateTime={activity.occurredAt} className="text-xs text-zinc-500">{time.time}</time></div>
    <p className="mt-1 text-sm text-zinc-800">{activity.summary}</p>
    {hint && <p className="mt-1 text-xs text-zinc-600">{hint}</p>}
    <div className="mt-2 flex flex-wrap gap-2"><span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs text-blue-900">{getActivityTypeLabel(activity.activityType)}</span><span className="rounded-full bg-zinc-100 px-2.5 py-1 text-xs text-zinc-700">{getTargetTypeLabel(activity.targetType)}</span></div>
  </li>;
}

export default function MoneyBookActivityView({ moneyBookUid }: { moneyBookUid: number }) {
  const permission = useMoneyBookPermission(moneyBookUid);
  const view = useMoneyBookActivities(moneyBookUid, permission.canRead);
  const members = useMoneyBookMembers(moneyBookUid, permission.canRead);
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const groups = useMemo(() => {
    const values = new Map<string, MoneyBookActivityResponse[]>();
    for (const item of view.result?.content ?? []) { const group = formatActivityTime(item.occurredAt).group; const items = values.get(group) ?? []; items.push(item); values.set(group, items); }
    return [...values.entries()];
  }, [view.result?.content]);
  if (permission.isLoading) return <p role="status">권한을 확인하는 중...</p>;
  if (!permission.canRead) return <p role="alert" className="rounded-xl border bg-white p-5">활동내역 조회 권한이 없습니다.</p>;
  return <main className="min-w-0 space-y-5">
    <header><h1 className="text-2xl font-semibold">활동내역</h1><p className="mt-1 text-sm text-zinc-600">가계부에서 발생한 변경 기록입니다.</p></header>
    <div className="rounded-xl border border-zinc-200 bg-white p-4">
      <button type="button" aria-expanded={mobileFilterOpen} aria-controls="activity-filters" onClick={() => setMobileFilterOpen(!mobileFilterOpen)} className="min-h-11 rounded-lg border px-4 font-medium md:hidden">{mobileFilterOpen ? "필터 닫기" : "필터"}</button>
      <div id="activity-filters" className={`${mobileFilterOpen ? "" : "hidden"} mt-3 grid gap-3 md:mt-0 md:grid md:grid-cols-2 xl:grid-cols-5`}>
        <label className="space-y-1 text-sm">시작일<input type="date" value={view.filters.startDate} onChange={(e) => view.update("startDate", e.target.value)} className="min-h-11 w-full rounded-lg border px-3" /></label>
        <label className="space-y-1 text-sm">종료일<input type="date" value={view.filters.endDate} onChange={(e) => view.update("endDate", e.target.value)} className="min-h-11 w-full rounded-lg border px-3" /></label>
        <label className="space-y-1 text-sm">사용자<select value={view.filters.actorUserUid} onChange={(e) => view.update("actorUserUid", e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체 사용자</option>{members.members.map((member) => <option key={member.userUid} value={member.userUid}>{member.nickname}</option>)}</select></label>
        <label className="space-y-1 text-sm">활동 유형<select value={view.filters.activityType} onChange={(e) => view.update("activityType", e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체 활동</option>{activityTypes.map((type) => <option key={type} value={type}>{activityTypeLabels[type]}</option>)}</select></label>
        <label className="space-y-1 text-sm">대상<select value={view.filters.targetType} onChange={(e) => view.update("targetType", e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="">전체 대상</option>{targetTypes.map((type) => <option key={type} value={type}>{targetTypeLabels[type]}</option>)}</select></label>
        <label className="space-y-1 text-sm md:col-span-2 xl:col-span-1">페이지 크기<select value={view.filters.size} onChange={(e) => view.update("size", e.target.value)} className="min-h-11 w-full rounded-lg border px-3"><option value="20">20개</option><option value="50">50개</option><option value="100">100개</option></select></label>
        <div className="flex items-end gap-2 md:col-span-2 xl:col-span-4"><button type="button" onClick={view.reset} className="min-h-11 rounded-lg border px-4">초기화</button>{!view.validRange && <p role="alert" className="self-center text-sm text-red-700">시작일은 종료일보다 늦을 수 없습니다.</p>}</div>
      </div>
    </div>
    {view.isError ? <div role="alert" className="rounded-xl border bg-white p-5 text-red-700"><p>{view.errorMessage}</p><button type="button" onClick={() => void view.refetch()} className="mt-3 min-h-11 rounded-lg border px-4 text-zinc-800">다시 시도</button></div> : view.isLoading ? <p role="status" className="rounded-xl border bg-white p-5">활동내역을 불러오는 중...</p> : view.result && <>
      {view.isFetching && <p role="status" className="text-sm text-zinc-500">목록을 업데이트하는 중...</p>}
      {view.result.content.length === 0 ? <p className="rounded-xl border bg-white p-8 text-center text-zinc-600">{view.filters.startDate || view.filters.endDate || view.filters.actorUserUid || view.filters.activityType || view.filters.targetType ? "조건에 맞는 활동내역이 없습니다." : "아직 기록된 활동이 없습니다."}</p> : <section aria-label="활동 타임라인" className="space-y-5">{groups.map(([date, activities]) => <section key={date} className="rounded-xl border border-zinc-200 bg-white p-5"><h2 className="mb-5 font-semibold">{date}</h2><ol>{activities.map((activity) => <ActivityItem key={activity.activityUid} activity={activity} />)}</ol></section>)}</section>}
      <nav aria-label="활동내역 페이지" className="flex items-center justify-center gap-4 rounded-xl border bg-white p-3"><button type="button" aria-label="이전 페이지" disabled={view.result.first} onClick={() => view.setPage(Math.max(0, view.result!.page - 1))} className="min-h-11 rounded-lg border px-4 disabled:opacity-40">이전</button><span aria-live="polite" className="text-sm">{view.result.totalPages === 0 ? 0 : view.result.page + 1} / {view.result.totalPages} 페이지 · 전체 {formatCount(view.result.totalElements)}건</span><button type="button" aria-label="다음 페이지" disabled={view.result.last || view.result.totalPages === 0} onClick={() => view.setPage(view.result!.page + 1)} className="min-h-11 rounded-lg border px-4 disabled:opacity-40">다음</button></nav>
    </>}
    {members.isError && <p className="text-xs text-zinc-500">사용자 필터를 불러오지 못했습니다.</p>}
  </main>;
}
