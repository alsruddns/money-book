"use client";
import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useGetMonthlyReportQuery } from "@/report/controller/reportApi";
import { useCancelMonthClosingMutation, useCloseMonthMutation, useGetMonthClosingQuery } from "../controller/closingApi";

export function useMonthlyClosing(moneyBookUid: number, year: number, month: number) {
  const permission = useMoneyBookPermission(moneyBookUid);
  const key = { moneyBookUid, year, month };
  const closing = useGetMonthClosingQuery(key, { skip: !permission.canRead });
  const monthly = useGetMonthlyReportQuery(key, { skip: !permission.canRead || Boolean(closing.currentData) });
  const [closeRequest, closeState] = useCloseMonthMutation();
  const [cancelRequest, cancelState] = useCancelMonthClosingMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const closed = closing.currentData ?? null;
  const notClosed = closing.isError && isNotFound(closing.error);
  async function close() {
    if (!permission.canUpdate || closeState.isLoading) return false;
    setErrorMessage(null);
    try { await closeRequest(key).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "월 결산을 완료하지 못했습니다.")); return false; }
  }
  async function cancel() {
    if (!permission.canUpdate || cancelState.isLoading) return false;
    setErrorMessage(null);
    try { await cancelRequest(key).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "결산을 취소하지 못했습니다.")); return false; }
  }
  const hasError = closing.isError && !notClosed || monthly.isError;
  return { permission, closing: closed, monthly: monthly.currentData, notClosed,
    isLoading: closing.isLoading || closing.isFetching && !closed || notClosed && (monthly.isLoading || monthly.isFetching && !monthly.currentData),
    isError: hasError, errorMessage: hasError ? errorMessage ?? getApiErrorMessage(closing.error ?? monthly.error, "결산 정보를 불러오지 못했습니다.") : errorMessage,
    close, cancel, isSaving: closeState.isLoading || cancelState.isLoading };
}

function isNotFound(error: unknown): boolean {
  return typeof error === "object" && error !== null && "status" in error && error.status === 404;
}
