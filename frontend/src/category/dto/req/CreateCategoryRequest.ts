import type { TransactionType } from "@/transaction/dto/TransactionType";

export interface CreateCategoryRequest {
  name: string;
  transactionType: TransactionType;
  sortOrder: number;
}
