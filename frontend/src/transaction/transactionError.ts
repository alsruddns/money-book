import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";

export function getTransactionErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "object" && error !== null && "status" in error) {
    if (error.status === 409) return "결산된 월에는 거래를 등록하거나 변경할 수 없습니다.";
    if (error.status === 403) return "이 가계부에서 거래를 처리할 권한이 없습니다.";
  }
  return getApiErrorMessage(error, fallback);
}
