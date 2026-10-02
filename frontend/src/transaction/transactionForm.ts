import type { TransactionType } from "./dto/TransactionType";
import type { CreateTransactionRequest } from "./dto/req/CreateTransactionRequest";

export interface TransactionFormValues {
  transactionType: TransactionType;
  amount: string;
  transactionDate: string;
  categoryUid: string;
  accountUid: string;
  memo: string;
}

export function todayLocalDate(now = new Date()): string {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function isLocalDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  if (year < 1 || month < 1 || month > 12 || day < 1) return false;
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0);
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return day <= days[month - 1];
}

export function parseTransactionRequest(values: TransactionFormValues):
  { request: CreateTransactionRequest; error: null } | { request: null; error: string } {
  if (values.transactionType !== "INCOME" && values.transactionType !== "EXPENSE") {
    return { request: null, error: "수입 또는 지출을 선택해 주세요." };
  }
  const amountText = values.amount.trim();
  const amount = Number(amountText);
  if (!/^\d+(\.\d{1,2})?$/.test(amountText) || !Number.isFinite(amount) || amount <= 0 ||
      !Number.isSafeInteger(Math.trunc(amount))) {
    return { request: null, error: "금액은 0보다 큰 숫자이며 소수점 둘째 자리까지 입력할 수 있습니다." };
  }
  if (!isLocalDate(values.transactionDate)) return { request: null, error: "올바른 거래 날짜를 입력해 주세요." };
  const categoryUid = Number(values.categoryUid);
  const accountUid = Number(values.accountUid);
  if (!Number.isSafeInteger(categoryUid) || categoryUid <= 0) return { request: null, error: "카테고리를 선택해 주세요." };
  if (!Number.isSafeInteger(accountUid) || accountUid <= 0) return { request: null, error: "계좌 또는 결제수단을 선택해 주세요." };
  if (values.memo.length > 500) return { request: null, error: "메모는 500자 이내로 입력해 주세요." };
  return {
    request: {
      transactionType: values.transactionType,
      amount,
      transactionDate: values.transactionDate,
      categoryUid,
      accountUid,
      memo: values.memo.trim() || null,
    },
    error: null,
  };
}

export function formatWon(amount: number): string {
  return `${new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(amount)}원`;
}
