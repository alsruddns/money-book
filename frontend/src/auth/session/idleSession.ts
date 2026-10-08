export const IDLE_TIMEOUT_MS = 6 * 60 * 60 * 1000;
export const IDLE_WARNING_MS = IDLE_TIMEOUT_MS - 5 * 60 * 1000;
export const IDLE_HEARTBEAT_MS = 30 * 60 * 1000;
export const IDLE_ACTIVITY_STORAGE_KEY = "money-book.auth.lastActivityAt";

export function scheduleIdleTimers(
  lastActivityAt: number,
  onWarning: () => void,
  onTimeout: () => void,
  now = Date.now(),
  schedule: (callback: () => void, delay: number) => number = (callback, delay) => window.setTimeout(callback, delay),
  cancel: (timer: number) => void = (timer) => window.clearTimeout(timer),
): () => void {
  const warningTimer = schedule(onWarning, Math.max(0, lastActivityAt + IDLE_WARNING_MS - now));
  const timeoutTimer = schedule(onTimeout, Math.max(0, lastActivityAt + IDLE_TIMEOUT_MS - now));
  return () => { cancel(warningTimer); cancel(timeoutTimer); };
}

export function isIdleExpired(lastActivityAt: number, now: number): boolean {
  return now - lastActivityAt >= IDLE_TIMEOUT_MS;
}

export function readLastActivityAt(): number | null {
  if (typeof window === "undefined") return null;
  const value = Number(window.localStorage.getItem(IDLE_ACTIVITY_STORAGE_KEY));
  return Number.isFinite(value) && value > 0 ? value : null;
}

export function writeLastActivityAt(timestamp: number): void {
  if (typeof window !== "undefined") window.localStorage.setItem(IDLE_ACTIVITY_STORAGE_KEY, String(timestamp));
}

export function clearLastActivityAt(): void {
  if (typeof window !== "undefined") window.localStorage.removeItem(IDLE_ACTIVITY_STORAGE_KEY);
}
