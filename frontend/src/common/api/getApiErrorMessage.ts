import type { FetchBaseQueryError } from "@reduxjs/toolkit/query";

export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (typeof error === "object" && error !== null && "status" in error) {
    const apiError = error as FetchBaseQueryError;
    if (apiError.status === 403) return "이 기능을 사용할 권한이 없습니다.";
    if (apiError.status === 404) return "요청한 정보를 찾을 수 없습니다.";
    if (apiError.status === 429) {
      const retryAfterSeconds = "retryAfterSeconds" in apiError &&
        typeof apiError.retryAfterSeconds === "number" &&
        Number.isFinite(apiError.retryAfterSeconds) && apiError.retryAfterSeconds > 0
        ? Math.ceil(apiError.retryAfterSeconds)
        : null;
      return retryAfterSeconds
        ? `요청이 많습니다. 약 ${retryAfterSeconds}초 후 다시 시도해 주세요.`
        : "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";
    }
    if (typeof apiError.status === "number" && apiError.status >= 500) {
      return "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    }
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
