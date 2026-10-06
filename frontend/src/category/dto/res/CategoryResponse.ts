import type { TransactionType } from "@/transaction/dto/TransactionType";

export interface CategoryResponse {
  categoryUid: number;
  moneyBookUid: number;
  name: string;
  transactionType: TransactionType;
  sortOrder: number;
}
