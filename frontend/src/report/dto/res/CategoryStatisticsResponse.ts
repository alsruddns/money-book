import type { TransactionType } from "@/transaction/dto/TransactionType";
export interface CategoryStatisticsResponse { categoryUid: number; categoryName: string; transactionType: TransactionType; totalAmount: number; transactionCount: number; ratio: number }
