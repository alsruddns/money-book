import type { TransactionType } from "../TransactionType";
export type TransactionSearchSort = "DATE_DESC" | "DATE_ASC" | "AMOUNT_DESC" | "AMOUNT_ASC";
export interface TransactionSearchRequest {
  startDate: string; endDate: string; transactionType?: TransactionType; categoryUid?: number; accountUid?: number;
  keyword?: string; minAmount?: number; maxAmount?: number; page: number; size: number; sort: TransactionSearchSort;
}
