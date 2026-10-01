import type { TransactionType } from "../TransactionType";

export interface UpdateTransactionRequest {
  transactionType: TransactionType;
  amount: number;
  transactionDate: string;
  categoryUid: number;
  accountUid: number;
  memo: string | null;
}
