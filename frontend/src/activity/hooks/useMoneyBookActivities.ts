"use client";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMoneyBookActivitiesQuery } from "../controller/activityApi";
import { activityTypes, targetTypes } from "../activityLabels";

export function useMoneyBookActivities(moneyBookUid: number, enabled: boolean) {
  const params = useSearchParams(); const router = useRouter(); const pathname = usePathname();
  const startDate = params.get("startDate") ?? ""; const endDate = params.get("endDate") ?? "";
  const actorRaw = params.get("actorUserUid") ?? ""; const typeRaw = params.get("activityType") ?? ""; const targetRaw = params.get("targetType") ?? "";
  const pageRaw = Number(params.get("page") ?? 0); const sizeRaw = Number(params.get("size") ?? 20);
  const validDate = (v: string) => { if (!v) return true; const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(v); if (!match) return false; const date = new Date(Date.UTC(Number(match[1]), Number(match[2])-1, Number(match[3]))); return date.toISOString().slice(0,10) === v; };
  const validRange = validDate(startDate) && validDate(endDate) && (!startDate || !endDate || startDate <= endDate);
  const actorUserUid = /^\d+$/.test(actorRaw) && Number(actorRaw) > 0 ? Number(actorRaw) : undefined;
  const activityType = activityTypes.includes(typeRaw as typeof activityTypes[number]) ? typeRaw : undefined;
  const targetType = targetTypes.includes(targetRaw as typeof targetTypes[number]) ? targetRaw : undefined;
  const page = Number.isInteger(pageRaw) && pageRaw >= 0 ? pageRaw : 0; const size = [20,50,100].includes(sizeRaw) ? sizeRaw : 20;
  const result = useGetMoneyBookActivitiesQuery({ moneyBookUid, ...(startDate ? { startDate } : {}), ...(endDate ? { endDate } : {}), ...(actorUserUid ? { actorUserUid } : {}), ...(activityType ? { activityType } : {}), ...(targetType ? { targetType } : {}), page, size }, { skip: !enabled || !validRange, refetchOnMountOrArgChange: true });
  function update(key: string, value: string) {
    const next = new URLSearchParams(params.toString());
    if (value) next.set(key, value); else next.delete(key);
    next.delete("page");
    router.push(next.size ? `${pathname}?${next}` : pathname);
  }
  function reset() { router.push(pathname); }
  function setPage(nextPage: number) { const next = new URLSearchParams(params.toString()); if (nextPage) next.set("page", String(nextPage)); else next.delete("page"); router.push(`${pathname}?${next}`); }
  return { filters: { startDate, endDate, actorUserUid: actorRaw, activityType: typeRaw, targetType: targetRaw, page, size }, validRange, update, reset, setPage, result: result.currentData, isLoading: result.isFetching && !result.currentData, isFetching: result.isFetching, isError: result.isError, errorMessage: result.isError ? getApiErrorMessage(result.error, "활동내역을 불러오지 못했습니다.") : null, refetch: result.refetch };
}
