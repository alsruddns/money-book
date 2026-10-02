"use client";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMoneyBookSetting } from "../hooks/useMoneyBookSetting";
import GeneralSettingSection from "./GeneralSettingSection";
import DataExportSection from "./DataExportSection";
import BackupRestoreSection from "./BackupRestoreSection";

export default function MoneyBookSettingsView({ moneyBookUid }: { moneyBookUid: number }) {
  const { canRead, canUpdate } = useMoneyBookPermission(moneyBookUid);
  const { setting, isLoading, isError, errorMessage } = useMoneyBookSetting(moneyBookUid, canRead);
  if (!canRead) return <p role="alert">가계부 설정 조회 권한이 없습니다.</p>;
  return <div className="min-w-0 space-y-5"><header><h1 className="text-2xl font-semibold">가계부 설정</h1><p className="mt-1 text-sm text-zinc-600">가계부 표시 방식과 데이터를 관리합니다.</p></header>
    {isLoading && <p role="status" className="rounded-xl bg-white p-5">설정을 불러오는 중...</p>}{isError && <p role="alert" className="rounded-xl bg-white p-5 text-red-700">{errorMessage}</p>}
    {setting && <GeneralSettingSection key={setting.weekStartDay} moneyBookUid={moneyBookUid} canUpdate={canUpdate} value={setting.weekStartDay} />}
    <DataExportSection moneyBookUid={moneyBookUid} /><BackupRestoreSection moneyBookUid={moneyBookUid} />
  </div>;
}
