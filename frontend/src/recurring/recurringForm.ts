import { parsePositiveAmount } from "@/common/validation/positiveAmount";
import { isLocalDate } from "@/transaction/transactionForm";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { CreateRecurringTransactionRequest } from "./dto/req/CreateRecurringTransactionRequest";
import type { RecurringFrequency } from "./dto/RecurringFrequency";

export interface RecurringFormValues {
  transactionType: TransactionType;
  amount: string;
  categoryUid: string;
  accountUid: string;
  frequency: RecurringFrequency;
  dayOfMonth: string;
  dayOfWeek: string;
  startDate: string;
  endDate: string;
  memo: string;
}

export function parseRecurringRequest(values: RecurringFormValues):
  { request: CreateRecurringTransactionRequest; error: null } | { request: null; error: string } {
  if (values.transactionType !== "INCOME" && values.transactionType !== "EXPENSE") return { request: null, error: "수입 또는 지출을 선택해 주세요." };
  const amount = parsePositiveAmount(values.amount);
  if (amount === null) return { request: null, error: "금액은 0보다 큰 숫자로 소수점 둘째 자리까지 입력해 주세요." };
  const categoryUid = Number(values.categoryUid);
  const accountUid = Number(values.accountUid);
  if (!Number.isSafeInteger(categoryUid) || categoryUid <= 0) return { request: null, error: "카테고리를 선택해 주세요." };
  if (!Number.isSafeInteger(accountUid) || accountUid <= 0) return { request: null, error: "계좌/결제수단을 선택해 주세요." };
  if (!isLocalDate(values.startDate)) return { request: null, error: "올바른 시작 날짜를 입력해 주세요." };
  if (values.endDate && (!isLocalDate(values.endDate) || values.endDate < values.startDate)) {
    return { request: null, error: "종료 날짜는 시작 날짜 이후로 입력해 주세요." };
  }
  if (values.memo.length > 500) return { request: null, error: "메모는 500자 이내로 입력해 주세요." };
  let dayOfMonth: number | null = null;
  let dayOfWeek: number | null = null;
  if (values.frequency === "MONTHLY") {
    dayOfMonth = Number(values.dayOfMonth);
    if (!/^\d+$/.test(values.dayOfMonth) || dayOfMonth < 1 || dayOfMonth > 31) {
      return { request: null, error: "매월 반복일은 1~31일 중에서 선택해 주세요." };
    }
  } else if (values.frequency === "WEEKLY") {
    dayOfWeek = Number(values.dayOfWeek);
    if (!/^\d+$/.test(values.dayOfWeek) || dayOfWeek < 1 || dayOfWeek > 7) {
      return { request: null, error: "매주 반복 요일을 선택해 주세요." };
    }
  } else return { request: null, error: "반복 주기를 선택해 주세요." };
  return { request: {
    transactionType: values.transactionType, amount, categoryUid, accountUid,
    frequency: values.frequency, dayOfMonth, dayOfWeek, startDate: values.startDate,
    endDate: values.endDate || null, memo: values.memo.trim() || null,
  }, error: null };
}
