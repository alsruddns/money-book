import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { tokenStorage } from "@/auth/storage/tokenStorage";
import { setTokens } from "@/auth/store/authSlice";
import { clearLocalSession } from "@/auth/session/clearLocalSession";
import { getLocaleFromPathname, withMoneyLocale } from "@/i18n/config";

function isPublicAuthRequest(url: string): boolean {
  const path = url.replace(/^\//, "");
  return path === "auth/login" || path === "auth/signup" || path === "auth/refresh";
}

const rawBaseQuery = fetchBaseQuery({
  baseUrl: "/api",
  prepareHeaders: (headers, { arg }) => {
    const url = typeof arg === "string" ? arg : arg.url;
    if (!isPublicAuthRequest(url)) {
      const accessToken = tokenStorage.getTokens()?.accessToken;
      if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
    }
    return headers;
  },
});

let refreshPromise: Promise<boolean> | null = null;

function clearStoredAuth(dispatch: Parameters<BaseQueryFn>[1]["dispatch"]) {
  clearLocalSession(dispatch, () => dispatch(baseApi.util.resetApiState()));
}

export const baseQueryWithReauth: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> =
  async (args, api, extraOptions) => {
    const url = typeof args === "string" ? args : args.url;
    const sentAccessToken = tokenStorage.getTokens()?.accessToken;
    const result = await rawBaseQuery(args, api, extraOptions);
    if (result.error?.status !== 401 || isPublicAuthRequest(url)) {
      redirectAccessErrorRead(result.error, args);
      return addRetryAfter(result);
    }

    const tokens = tokenStorage.getTokens();
    if (!tokens?.refreshToken) {
      clearStoredAuth(api.dispatch);
      return result;
    }

    if (tokens.accessToken !== sentAccessToken) {
      const retryResult = await rawBaseQuery(args, api, extraOptions);
      if (retryResult.error?.status === 401) clearStoredAuth(api.dispatch);
      redirectAccessErrorRead(retryResult.error, args);
      return addRetryAfter(retryResult);
    }

    if (!refreshPromise) {
      const refreshToken = tokens.refreshToken;
      refreshPromise = (async () => {
        const refreshResult = await rawBaseQuery(
          { url: "auth/refresh", method: "POST", body: { refreshToken } },
          api,
          extraOptions,
        );
        const data = refreshResult.data;
        if (
          !refreshResult.error && typeof data === "object" && data !== null &&
          "accessToken" in data && typeof data.accessToken === "string" && data.accessToken &&
          "refreshToken" in data && typeof data.refreshToken === "string" && data.refreshToken
        ) {
          const current = tokenStorage.getTokens();
          if (current?.refreshToken !== refreshToken) return false;
          const updated = { accessToken: data.accessToken, refreshToken: data.refreshToken };
          tokenStorage.setTokens(updated);
          if (tokenStorage.getTokens()?.refreshToken === updated.refreshToken) {
            api.dispatch(setTokens(updated));
            return true;
          }
        }
        if (tokenStorage.getTokens()?.refreshToken === refreshToken) {
          clearStoredAuth(api.dispatch);
        }
        return false;
      })().finally(() => {
        refreshPromise = null;
      });
    }

    const refreshed = await refreshPromise;
    if (!refreshed) return result;

    const retryResult = await rawBaseQuery(args, api, extraOptions);
    if (retryResult.error?.status === 401) clearStoredAuth(api.dispatch);
    redirectAccessErrorRead(retryResult.error, args);
    return addRetryAfter(retryResult);
  };

function redirectAccessErrorRead(error: FetchBaseQueryError | undefined, args: string | FetchArgs) {
  const method = typeof args === "string" ? "GET" : args.method ?? "GET";
  if ((error?.status !== 403 && error?.status !== 404)
      || method.toUpperCase() !== "GET" || typeof window === "undefined") return;
  const url = typeof args === "string" ? args : args.url;
  // Money book resources use 404 to represent valid empty states, including an unclosed month.
  if (url.replace(/^\/+/, "").startsWith("money-books/")) return;
  const locale = getLocaleFromPathname(window.location.pathname);
  if (error.status === 404) {
    const destination = withMoneyLocale(locale, "/not-found");
    if (window.location.pathname !== destination) window.location.assign(new URL(destination, window.location.origin).toString());
    return;
  }
  const code = typeof error === "object" && "data" in error && typeof error.data === "object"
    && error.data !== null && "code" in error.data && typeof error.data.code === "string"
    ? error.data.code : null;
  const destination = withMoneyLocale(locale, code === "PASSWORD_CHANGE_REQUIRED" ? "/change-required-password" : "/forbidden");
  if (window.location.pathname !== destination) {
    window.location.assign(new URL(destination, window.location.origin).toString());
  }
}

function addRetryAfter<T extends { error?: FetchBaseQueryError; meta?: { response?: Response } }>(result: T): T {
  if (result.error?.status !== 429) return result;
  const header = result.meta?.response?.headers.get("Retry-After");
  const retryAfterSeconds = header === null || header === undefined ? Number.NaN : Number(header);
  if (Number.isFinite(retryAfterSeconds) && retryAfterSeconds >= 0) {
    Object.assign(result.error, { retryAfterSeconds });
  }
  return result;
}

export const baseApi = createApi({
  reducerPath: "baseApi",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["MoneyBook", "MoneyBookInvitation", "MoneyBookMember", "MoneyBookSetting", "MoneyBookActivity", "Category", "Account", "AccountMe", "AccountSessions", "AccountSecurity", "AuthMe", "Transaction", "Calendar", "Budget", "Transfer", "Recurring", "Report", "Dashboard", "Closing", "AdminMe", "AdminOverview", "AdminUser", "AdminUserList", "AdminMoneyBook", "AdminMoneyBookList", "AdminActivity", "AdminAuditLog", "BoardPost", "BoardCategory"],
  endpoints: () => ({}),
});
