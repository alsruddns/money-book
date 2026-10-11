import type { TransactionType } from "@/transaction/dto/TransactionType";

export interface CategoryResponse {
  categoryUid: number;
  moneyBookUid: number;
  name: string;
  transactionType: TransactionType;
  sortOrder: number;
  level: CategoryLevel;
  parentCategoryUid: number | null;
  active: boolean;
}

export type CategoryLevel = "LARGE" | "MEDIUM" | "SMALL";

export interface CategoryTreeResponse {
  categoryUid: number;
  name: string;
  level: CategoryLevel;
  children: CategoryTreeResponse[];
}
