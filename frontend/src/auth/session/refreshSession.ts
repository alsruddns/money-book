import type { AuthTokens } from "../store/authSlice";
import { tokenStorage } from "../storage/tokenStorage";
import { setTokens } from "../store/authSlice";
import { clearLocalSession } from "./clearLocalSession";

type Dispatch = (action: { type: string }) => unknown;
let pendingRefresh: Promise<unknown> | null = null;

export function coordinateRefresh<T>(refresh: () => Promise<T>): Promise<T> {
  if (!pendingRefresh) {
    pendingRefresh = refresh().finally(() => { pendingRefresh = null; });
  }
  return pendingRefresh as Promise<T>;
}

export function refreshSession(dispatch: Dispatch): Promise<AuthTokens | null> {
  return coordinateRefresh(async () => {
    const current = tokenStorage.getTokens();
    if (!current?.refreshToken) return Promise.resolve(null);
    const refreshToken = current.refreshToken;
    return fetch("/api/auth/refresh", {
      method: "POST", headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    }).then(async (response) => {
      if (!response.ok) return null;
      const value: unknown = await response.json();
      if (typeof value !== "object" || value === null || !("accessToken" in value) || !("refreshToken" in value)
        || typeof value.accessToken !== "string" || typeof value.refreshToken !== "string") return null;
      const latest = tokenStorage.getTokens();
      if (latest?.refreshToken !== refreshToken) return null;
      const updated = { accessToken: value.accessToken, refreshToken: value.refreshToken };
      tokenStorage.setTokens(updated);
      dispatch(setTokens(updated));
      return updated;
    }).catch(() => null).then((tokens) => {
      if (!tokens && tokenStorage.getTokens()?.refreshToken === refreshToken) clearLocalSession(dispatch);
      return tokens;
    });
  });
}
