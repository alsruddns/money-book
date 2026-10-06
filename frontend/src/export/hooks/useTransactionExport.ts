"use client";
import { useState } from "react";
import { useExportTransactionsMutation } from "../controller/transactionExportApi";
import type { TransactionExportRequest } from "../dto/TransactionExportRequest";
import { startDownload } from "@/common/download/fileDownload";

export function useTransactionExport() {
  const [exportFile, state] = useExportTransactionsMutation();
  const [errorMessage, setErrorMessage] = useState("");
  async function download(request: TransactionExportRequest, format: "csv" | "xlsx") {
    setErrorMessage("");
    try {
      const result = await exportFile({ ...request, format }).unwrap();
      if (!("objectUrl" in result)) throw new Error("파일을 준비하지 못했습니다.");
      startDownload(result);
    } catch { setErrorMessage(`${format.toUpperCase()} 파일을 생성하지 못했습니다.`); }
  }
  return { download, isLoading: state.isLoading, errorMessage };
}
