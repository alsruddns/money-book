"use client";
import { useState } from "react";
import { formatNumber } from "@/common/format/money";
import { useMoneyBookPermission } from "@/moneybook/hooks/useMoneyBookPermission";
import { useMoneyBookBackup } from "@/backup/hooks/useMoneyBookBackup";
import type { BackupValidationResponse } from "@/backup/dto/BackupValidationResponse";

export default function BackupRestoreSection({ moneyBookUid }: { moneyBookUid: number }) {
  const { isOwner, isAdmin } = useMoneyBookPermission(moneyBookUid);
  const { downloadBackup, validateBackup, restoreBackup, isDownloading, isValidating, isRestoring, errorMessage } = useMoneyBookBackup(moneyBookUid);
  const [file, setFile] = useState<File | null>(null); const [preview, setPreview] = useState<BackupValidationResponse | null>(null); const [confirmRestore, setConfirmRestore] = useState(false);
  async function checkFile(next: File | null) {
    setFile(next); setPreview(null); setConfirmRestore(false);
    if (!next) return;
    if (next.size > 20 * 1024 * 1024) { setFile(null); return; }
    setPreview(await validateBackup(next));
  }
  return <section className="space-y-5 rounded-xl border border-zinc-200 bg-white p-5">
    <div><h2 className="text-lg font-semibold">백업 및 복원</h2><p className="text-sm text-zinc-600">백업은 소유자와 관리자만 받을 수 있습니다. 복원은 기존 가계부를 변경하지 않고 새 가계부를 만듭니다.</p></div>
    {(isOwner || isAdmin) && <div className="rounded-lg bg-zinc-50 p-4"><h3 className="font-medium">가계부 백업</h3><p className="mt-1 text-sm text-zinc-600">가계부 데이터를 JSON 파일로 저장합니다.</p><button type="button" disabled={isDownloading} onClick={downloadBackup} className="mt-3 min-h-11 rounded-lg border border-zinc-300 bg-white px-4 font-medium disabled:opacity-50">{isDownloading ? "백업 준비 중..." : "백업 파일 다운로드"}</button></div>}
    <div className="space-y-3 rounded-lg bg-zinc-50 p-4"><h3 className="font-medium">백업 복원</h3><p className="text-sm text-zinc-600">복원하면 새 가계부가 생성되며 현재 가계부는 그대로 유지됩니다.</p>
      <label className="block space-y-2 text-sm">JSON 백업 파일<input type="file" accept="application/json,.json" disabled={isValidating || isRestoring} onChange={(e) => void checkFile(e.target.files?.[0] ?? null)} className="block min-h-11 w-full rounded-lg border bg-white p-2" /></label>
      {file && file.size > 20 * 1024 * 1024 && <p role="alert" className="text-sm text-red-700">파일은 20MB 이하여야 합니다.</p>}
      {isValidating && <p role="status" className="text-sm">백업 파일을 확인하는 중...</p>}
      {preview && <div className="space-y-2 rounded-lg border bg-white p-3"><h4 className="font-medium">{preview.moneyBookName ?? "백업 미리보기"}</h4>{preview.valid ? <p className="text-sm">카테고리 {formatNumber(preview.categoryCount)}개 · 계좌 {formatNumber(preview.accountCount)}개 · 거래 {formatNumber(preview.transactionCount)}건 · 이체 {formatNumber(preview.transferCount)}건 · 정기 규칙 {formatNumber(preview.recurringTransactionCount)}개 · 예산 {formatNumber(preview.budgetCount)}개 · 결산 {formatNumber(preview.monthClosingCount)}개</p> : <p role="alert" className="text-sm text-red-700">유효하지 않은 백업 파일입니다.</p>}{(preview.firstTransactionDate || preview.lastTransactionDate) && <p className="text-sm text-zinc-600">거래 기간: {preview.firstTransactionDate ?? "-"} ~ {preview.lastTransactionDate ?? "-"}</p>}{preview.warnings.map((warning) => <p key={warning} className="text-sm text-amber-800">안내: {warning}</p>)}{preview.errors.map((error) => <p key={error} className="text-sm text-red-700">{error}</p>)}</div>}
      {preview?.valid && file && !confirmRestore && <button type="button" onClick={() => setConfirmRestore(true)} className="min-h-11 rounded-lg bg-blue-700 px-4 font-medium text-white">새 가계부로 복원</button>}
      {confirmRestore && file && <div role="alertdialog" aria-label="백업 복원 확인" className="space-y-3 rounded-lg border border-amber-300 bg-amber-50 p-4"><p className="font-medium">백업을 새 가계부로 복원하시겠습니까?</p><p className="text-sm">기존 가계부는 변경되지 않으며, 복원 완료 후 새 가계부로 이동합니다.</p><div className="flex gap-2"><button type="button" disabled={isRestoring} onClick={() => void restoreBackup(file)} className="min-h-11 rounded-lg bg-blue-700 px-4 text-white disabled:opacity-50">{isRestoring ? "복원 중..." : "복원"}</button><button type="button" disabled={isRestoring} onClick={() => setConfirmRestore(false)} className="min-h-11 rounded-lg border px-4">취소</button></div></div>}
    </div>
    {errorMessage && <p role="alert" className="text-sm text-red-700">{errorMessage}</p>}
  </section>;
}
