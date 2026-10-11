import type { TransactionType } from "../TransactionType";

export interface CreateTransactionRequest {
  transactionType: TransactionType;
  amount: number;
  transactionDate: string;
  largeCategoryUid: number;
  mediumCategoryUid: number | null;
  smallCategoryUid: number | null;
  accountUid: number;
  memo: string | null;
}
