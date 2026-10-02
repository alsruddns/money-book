import type { TransactionType } from "@/transaction/dto/TransactionType";

export interface CalendarTransactionResponse {
  transactionUid: number;
  transactionType: TransactionType;
  amount: number;
  categoryUid: number;
  categoryName: string;
  accountUid: number;
  accountName: string;
  memo: string | null;
  recurringTransactionUid: number | null;
  scheduledDate: string | null;
}

export interface CalendarTransferResponse {
  transferUid: number;
  fromAccountUid: number;
  fromAccountName: string;
  toAccountUid: number;
  toAccountName: string;
  amount: number;
  memo: string | null;
}

export interface DailyCalendarResponse {
  date: string;
  holiday: boolean;
  holidayName: string | null;
  transactions: CalendarTransactionResponse[];
  transfers: CalendarTransferResponse[];
}
