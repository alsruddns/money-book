import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { RecurringFrequency } from "../RecurringFrequency";

export interface RecurringTransactionResponse {
  recurringTransactionUid: number;
  moneyBookUid: number;
  transactionType: TransactionType;
  amount: number;
  categoryUid: number;
  categoryName: string;
  accountUid: number;
  accountName: string;
  frequency: RecurringFrequency;
  dayOfMonth: number | null;
  dayOfWeek: number | null;
  startDate: string;
  endDate: string | null;
  memo: string | null;
  isActive: boolean;
  lastGeneratedDate: string | null;
}
