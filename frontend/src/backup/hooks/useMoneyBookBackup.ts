"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
import { startDownload } from "@/common/download/fileDownload";
import { useExportMoneyBookBackupMutation, useRestoreMoneyBookBackupMutation, useValidateMoneyBookBackupMutation } from "../controller/moneyBookBackupApi";

export function useMoneyBookBackup(moneyBookUid: number) {
  const router = useRouter();
  const [exportBackup, backupState] = useExportMoneyBookBackupMutation();
  const [validate, validateState] = useValidateMoneyBookBackupMutation();
  const [restore, restoreState] = useRestoreMoneyBookBackupMutation();
  const [errorMessage, setErrorMessage] = useState("");
  async function downloadBackup() {
    setErrorMessage("");
    try { const file = await exportBackup(moneyBookUid).unwrap(); if (!("objectUrl" in file)) throw new Error(); startDownload(file); }
    catch { setErrorMessage("백업 파일을 생성하지 못했습니다."); }
  }
  async function validateBackup(file: File) {
    setErrorMessage("");
    try { return await validate({ file }).unwrap(); }
    catch { setErrorMessage("백업 파일을 확인하지 못했습니다."); return null; }
  }
  async function restoreBackup(file: File) {
    setErrorMessage("");
    try { const result = await restore({ file }).unwrap(); router.push(`/books/${result.moneyBookUid}`); }
    catch { setErrorMessage("복원에 실패했습니다. 기존 가계부는 변경되지 않았습니다."); }
  }
  return { downloadBackup, validateBackup, restoreBackup, isDownloading: backupState.isLoading, isValidating: validateState.isLoading, isRestoring: restoreState.isLoading, errorMessage };
}
