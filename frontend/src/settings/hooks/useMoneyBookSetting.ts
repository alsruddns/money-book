"use client";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";
import { useGetMoneyBookSettingQuery } from "../controller/moneyBookSettingApi";
export function useMoneyBookSetting(moneyBookUid: number, enabled = true) {
  const { currentData, isLoading, isFetching, isError, error } = useGetMoneyBookSettingQuery(moneyBookUid, { skip: !enabled });
  return { setting: currentData, isLoading: enabled && (isLoading || isFetching) && !currentData, isFetching, isError,
    errorMessage: isError ? getApiErrorMessage(error, "가계부 설정을 불러오지 못했습니다.") : null };
}
