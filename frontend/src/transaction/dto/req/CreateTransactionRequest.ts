import type { TransactionType } from "../TransactionType";

export interface CreateTransactionRequest {
  transactionType: TransactionType;
  amount: number;
  transactionDate: string;
  categoryUid: number;
  accountUid: number;
  memo: string | null;
}
