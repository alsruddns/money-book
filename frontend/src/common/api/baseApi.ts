import { createApi, fetchBaseQuery } from "@reduxjs/toolkit/query/react";
import { tokenStorage } from "@/auth/storage/tokenStorage";

export const baseApi = createApi({
  reducerPath: "baseApi",
  baseQuery: fetchBaseQuery({
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
    prepareHeaders: (headers) => {
      const accessToken = tokenStorage.getTokens()?.accessToken;
      if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
      return headers;
    },
  }),
  endpoints: () => ({}),
});
