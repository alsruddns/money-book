import { baseApi } from "@/common/api/baseApi";
import { prepareDownload, type PreparedDownload } from "@/common/download/fileDownload";
import type { BackupValidationResponse } from "../dto/BackupValidationResponse";
import type { BackupRestoreResponse } from "../dto/BackupRestoreResponse";

interface BackupFileArg { file: File }
export const moneyBookBackupApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    exportMoneyBookBackup: builder.mutation<PreparedDownload | { message: string }, number>({
      query: (moneyBookUid) => ({ url: `money-books/${moneyBookUid}/backups/export`, responseHandler: (response: Response) => prepareDownload(response, `money-book-${moneyBookUid}-backup.json`) }),
      invalidatesTags: (_result, error, moneyBookUid) => error ? [] : [{ type: "MoneyBookActivity", id: moneyBookUid }],
    }),
    validateMoneyBookBackup: builder.mutation<BackupValidationResponse, BackupFileArg>({
      query: ({ file }) => { const body = new FormData(); body.set("file", file); return { url: "money-books/backups/validate", method: "POST", body }; },
    }),
    restoreMoneyBookBackup: builder.mutation<BackupRestoreResponse, BackupFileArg>({
      query: ({ file }) => { const body = new FormData(); body.set("file", file); return { url: "money-books/backups/restore", method: "POST", body }; },
      invalidatesTags: (_result, error) => error ? [] : ["MoneyBook", "MoneyBookActivity"],
    }),
  }),
});
export const { useExportMoneyBookBackupMutation, useValidateMoneyBookBackupMutation, useRestoreMoneyBookBackupMutation } = moneyBookBackupApi;
