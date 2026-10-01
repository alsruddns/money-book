import type { TransactionType } from "../TransactionType";

export interface TransactionResponse {
  transactionUid: number;
  moneyBookUid: number;
  transactionType: TransactionType;
  amount: number;
  transactionDate: string;
  categoryUid: number;
  categoryName: string;
  accountUid: number;
  accountName: string;
  memo: string | null;
}
