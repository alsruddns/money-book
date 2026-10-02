import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { RecurringFrequency } from "../RecurringFrequency";

export interface CreateRecurringTransactionRequest {
  transactionType: TransactionType;
  amount: number;
  categoryUid: number;
  accountUid: number;
  frequency: RecurringFrequency;
  dayOfMonth: number | null;
  dayOfWeek: number | null;
  startDate: string;
  endDate: string | null;
  memo: string | null;
}
