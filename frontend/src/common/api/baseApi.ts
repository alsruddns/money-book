import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from "@reduxjs/toolkit/query";
import { tokenStorage } from "@/auth/storage/tokenStorage";
import { clearAuth, setTokens } from "@/auth/store/authSlice";

function isPublicAuthRequest(url: string): boolean {
  const path = url.replace(/^\//, "");
  return path === "auth/login" || path === "auth/signup" || path === "auth/refresh";
}

const rawBaseQuery = fetchBaseQuery({
  baseUrl: process.env.NEXT_PUBLIC_API_BASE_URL,
  fetchFn: (input, init) => {
    const request = new Request(input, init);
    if (typeof window === "undefined") return fetch(request);

    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;
    if (!apiBaseUrl) return fetch(request);

    const backendUrl = new URL(apiBaseUrl);
    const requestUrl = new URL(request.url);
    if (
      requestUrl.origin === backendUrl.origin &&
      requestUrl.pathname.startsWith(`${backendUrl.pathname.replace(/\/$/, "")}/`)
    ) {
      const localUrl = new URL(requestUrl.pathname + requestUrl.search, window.location.origin);
      return fetch(new Request(localUrl, request));
    }
    return fetch(request);
  },
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
  tokenStorage.clearTokens();
  dispatch(clearAuth());
}

export const baseQueryWithReauth: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> =
  async (args, api, extraOptions) => {
    const url = typeof args === "string" ? args : args.url;
    const sentAccessToken = tokenStorage.getTokens()?.accessToken;
    const result = await rawBaseQuery(args, api, extraOptions);
    if (result.error?.status !== 401 || isPublicAuthRequest(url)) return result;

    const tokens = tokenStorage.getTokens();
    if (!tokens?.refreshToken) {
      clearStoredAuth(api.dispatch);
      return result;
    }

    if (tokens.accessToken !== sentAccessToken) {
      const retryResult = await rawBaseQuery(args, api, extraOptions);
      if (retryResult.error?.status === 401) clearStoredAuth(api.dispatch);
      return retryResult;
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
          "accessToken" in data && typeof data.accessToken === "string" && data.accessToken
        ) {
          const current = tokenStorage.getTokens();
          if (current?.refreshToken !== refreshToken) return false;
          const updated = tokenStorage.updateAccessToken(data.accessToken);
          if (updated) {
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
    return retryResult;
  };

export const baseApi = createApi({
  reducerPath: "baseApi",
  baseQuery: baseQueryWithReauth,
  tagTypes: ["MoneyBook", "MoneyBookInvitation", "MoneyBookMember", "Category", "Account", "Transaction", "Calendar", "Budget"],
  endpoints: () => ({}),
});
