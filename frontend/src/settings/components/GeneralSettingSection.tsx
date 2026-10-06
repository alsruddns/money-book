"use client";
import { useState } from "react";
import type { WeekStartDay } from "../dto/res/MoneyBookSettingResponse";
import { useUpdateMoneyBookSetting } from "../hooks/useUpdateMoneyBookSetting";

export default function GeneralSettingSection({ moneyBookUid, canUpdate, value }: { moneyBookUid: number; canUpdate: boolean; value: WeekStartDay }) {
  const [selected, setSelected] = useState(value);
  const [saved, setSaved] = useState(false);
  const updateState = useUpdateMoneyBookSetting(moneyBookUid, canUpdate);
  async function save() { setSaved(await updateState.update(selected)); }
  return <section className="space-y-4 rounded-xl border border-zinc-200 bg-white p-5">
    <div><h2 className="text-lg font-semibold">일반 설정</h2><p className="text-sm text-zinc-600">캘린더의 한 주 시작 요일을 설정합니다.</p></div>
    <fieldset disabled={!canUpdate || updateState.isLoading} className="flex flex-wrap gap-4">
      <legend className="mb-2 text-sm font-medium">한 주 시작 요일</legend>
      {([ ["SUNDAY", "일요일"], ["MONDAY", "월요일"] ] as const).map(([value, label]) => <label key={value} className="flex min-h-11 items-center gap-2 rounded-lg border px-3">
        <input type="radio" name="weekStartDay" value={value} checked={selected === value} onChange={() => { setSelected(value); setSaved(false); }} />{label}
      </label>)}
    </fieldset>
    {canUpdate ? <button type="button" disabled={updateState.isLoading || selected === value} onClick={save} className="min-h-11 rounded-lg bg-blue-700 px-4 font-medium text-white disabled:opacity-50">{updateState.isLoading ? "저장 중..." : "설정 저장"}</button> : <p className="text-sm text-zinc-600">조회 전용 설정입니다.</p>}
    {saved && <p role="status" className="text-sm text-green-700">설정을 저장했습니다.</p>}{updateState.errorMessage && <p role="alert" className="text-sm text-red-700">{updateState.errorMessage}</p>}
  </section>;
}
