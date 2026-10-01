import type { AuthTokens } from "../store/authSlice";

const STORAGE_KEY = "money-book.auth.tokens";

export const tokenStorage = {
  getTokens(): AuthTokens | null {
    if (typeof window === "undefined") return null;
    try {
      const value = window.localStorage.getItem(STORAGE_KEY);
      if (!value) return null;
      const parsed: unknown = JSON.parse(value);
      if (
        typeof parsed === "object" && parsed !== null &&
        "accessToken" in parsed && "refreshToken" in parsed &&
        typeof parsed.accessToken === "string" &&
        typeof parsed.refreshToken === "string"
      ) {
        return { accessToken: parsed.accessToken, refreshToken: parsed.refreshToken };
      }
    } catch {
      return null;
    }
    return null;
  },
  setTokens(tokens: AuthTokens): void {
    if (typeof window !== "undefined") {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
    }
  },
  clearTokens(): void {
    if (typeof window !== "undefined") window.localStorage.removeItem(STORAGE_KEY);
  },
};
