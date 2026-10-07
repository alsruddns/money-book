import { baseApi } from "@/common/api/baseApi";
import { prepareDownload, type PreparedDownload } from "@/common/download/fileDownload";
import type { TransactionExportRequest } from "../dto/TransactionExportRequest";

interface ExportArg extends TransactionExportRequest { format: "csv" | "xlsx" }
function makeQuery(arg: ExportArg) {
  const { moneyBookUid, format, ...filters } = arg;
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => { if (value !== undefined && value !== "") params.set(key, String(value)); });
  const fallback = `transactions-${filters.startDate}-${filters.endDate}.${format}`;
  return { url: `money-books/${moneyBookUid}/exports/transactions.${format}?${params}`, responseHandler: (response: Response) => prepareDownload(response, fallback) };
}

export const transactionExportApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    exportTransactions: builder.mutation<PreparedDownload, ExportArg>({ query: makeQuery }),
  }),
});
export const { useExportTransactionsMutation } = transactionExportApi;
