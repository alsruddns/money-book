import { parsePositiveAmount } from "@/common/validation/positiveAmount";
import { isLocalDate } from "@/transaction/transactionForm";
import type { CreateTransferRequest } from "./dto/req/CreateTransferRequest";

export interface TransferFormValues {
  fromAccountUid: string;
  toAccountUid: string;
  amount: string;
  transferDate: string;
  memo: string;
}

export function parseTransferRequest(values: TransferFormValues):
  { request: CreateTransferRequest; error: null } | { request: null; error: string } {
  const fromAccountUid = Number(values.fromAccountUid);
  const toAccountUid = Number(values.toAccountUid);
  if (!Number.isSafeInteger(fromAccountUid) || fromAccountUid <= 0) return { request: null, error: "출금 계좌를 선택해 주세요." };
  if (!Number.isSafeInteger(toAccountUid) || toAccountUid <= 0) return { request: null, error: "입금 계좌를 선택해 주세요." };
  if (fromAccountUid === toAccountUid) return { request: null, error: "출금 계좌와 입금 계좌는 다르게 선택해 주세요." };
  const amount = parsePositiveAmount(values.amount);
  if (amount === null) return { request: null, error: "금액은 0보다 큰 숫자로 소수점 둘째 자리까지 입력해 주세요." };
  if (!isLocalDate(values.transferDate)) return { request: null, error: "올바른 이체 날짜를 입력해 주세요." };
  if (values.memo.length > 500) return { request: null, error: "메모는 500자 이내로 입력해 주세요." };
  return { request: { fromAccountUid, toAccountUid, amount, transferDate: values.transferDate,
    memo: values.memo.trim() || null }, error: null };
}
