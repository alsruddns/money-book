import type { TransactionType } from "../TransactionType";

export interface TransactionResponse {
  transactionUid: number;
  moneyBookUid: number;
  transactionType: TransactionType;
  amount: number;
  transactionDate: string;
  categoryUid: number;
  categoryName: string;
  categoryPath?: {
    largeCategory: { categoryUid: number; name: string } | null;
    mediumCategory: { categoryUid: number; name: string } | null;
    smallCategory: { categoryUid: number; name: string } | null;
  };
  accountUid: number;
  accountName: string;
  memo: string | null;
}
