import { tokenStorage } from "../storage/tokenStorage";
import { clearAuth } from "../store/authSlice";
import { clearLastActivityAt } from "./idleSession";

type AuthDispatch = (action: { type: string }) => unknown;

export function clearLocalSession(dispatch: AuthDispatch, resetCache?: () => void): void {
  tokenStorage.clearTokens();
  clearLastActivityAt();
  dispatch(clearAuth());
  resetCache?.();
}
