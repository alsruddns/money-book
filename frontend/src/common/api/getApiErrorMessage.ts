import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "object" && error !== null && "status" in error) {
    const apiError = error as FetchBaseQueryError;
    if (
      "data" in apiError && typeof apiError.data === "object" &&
      apiError.data !== null && "message" in apiError.data &&
      typeof apiError.data.message === "string"
    ) {
      return apiError.data.message;
    }
    if (apiError.status === "FETCH_ERROR") {
      return "서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.";
    }
  }
  return fallback;
}
