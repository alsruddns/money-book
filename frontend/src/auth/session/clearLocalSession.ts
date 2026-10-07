import { tokenStorage } from "../storage/tokenStorage";
import { clearAuth } from "../store/authSlice";

type AuthDispatch = (action: { type: string }) => unknown;

export function clearLocalSession(dispatch: AuthDispatch, resetCache?: () => void): void {
  tokenStorage.clearTokens();
  dispatch(clearAuth());
  resetCache?.();
}
