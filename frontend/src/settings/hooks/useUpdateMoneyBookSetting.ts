"use client";
import { useState } from "react";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import type { WeekStartDay } from "../dto/res/MoneyBookSettingResponse";
import { useUpdateMoneyBookSettingMutation } from "../controller/moneyBookSettingApi";
export function useUpdateMoneyBookSetting(moneyBookUid: number, canUpdate: boolean) {
  const [updateRequest, state] = useUpdateMoneyBookSettingMutation();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  async function update(weekStartDay: WeekStartDay): Promise<boolean> {
    if (!canUpdate || state.isLoading) return false;
    setErrorMessage(null);
    try { await updateRequest({ moneyBookUid, request: { weekStartDay } }).unwrap(); return true; }
    catch (error) { setErrorMessage(getApiErrorMessage(error, "가계부 설정을 저장하지 못했습니다.")); return false; }
  }
  return { update, isLoading: state.isLoading, errorMessage };
}
