export interface TransactionExportRequest {
  moneyBookUid: number;
  startDate: string;
  endDate: string;
  transactionType?: "INCOME" | "EXPENSE";
  categoryUid?: number;
  accountUid?: number;
  keyword?: string;
}
