"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { usePathname } from "next/navigation";
import type { AppDispatch, RootState } from "@/store/store";
import { clearLocalSession } from "../session/clearLocalSession";
import { IDLE_ACTIVITY_STORAGE_KEY, IDLE_HEARTBEAT_MS, IDLE_WARNING_MS, isIdleExpired, readLastActivityAt, scheduleIdleTimers, writeLastActivityAt } from "../session/idleSession";
import { refreshSession } from "../session/refreshSession";
import { tokenStorage } from "../storage/tokenStorage";
import { baseApi } from "@/common/api/baseApi";
import { useMoneyRouter } from "@/common/components/useMoneyRouter";
import { getLocaleFromPathname } from "@/i18n/config";
import { translate } from "@/i18n/messages";

export default function IdleSessionManager() {
  const dispatch = useDispatch<AppDispatch>();
  const auth = useSelector((state: RootState) => state.auth);
  const pathname = usePathname() || "/ko/money";
  const router = useMoneyRouter();
  const [lastActivity, setLastActivity] = useState<number | null>(null);
  const [warning, setWarning] = useState(false);
  const lastServerRefresh = useRef(0);
  const sessionEnded = useRef(false);
  const previousPath = useRef(pathname);
  const locale = getLocaleFromPathname(pathname);

  const endSession = useCallback(async (reason: "idle" | "manual" = "idle") => {
    if (sessionEnded.current) return;
    sessionEnded.current = true;
    const tokens = tokenStorage.getTokens();
    if (tokens?.accessToken) {
      void fetch("/api/auth/logout", { method: "POST", headers: { Authorization: `Bearer ${tokens.accessToken}` } }).catch(() => undefined);
    }
    clearLocalSession(dispatch, () => dispatch(baseApi.util.resetApiState()));
    router.replace(reason === "idle" ? "/login?reason=idle" : "/login?reason=logged-out");
  }, [dispatch, router]);

  const recordActivity = useCallback((timestamp = Date.now(), persist = true) => {
    if (!tokenStorage.getTokens()) return;
    if (isIdleExpired(readLastActivityAt() ?? timestamp, timestamp)) { void endSession(); return; }
    if (persist) writeLastActivityAt(timestamp);
    setLastActivity(timestamp);
    setWarning(false);
    if (document.visibilityState === "visible" && timestamp - lastServerRefresh.current >= IDLE_HEARTBEAT_MS) {
      lastServerRefresh.current = timestamp;
      void refreshSession(dispatch).then((tokens) => { if (!tokens) void endSession(); });
    }
  }, [dispatch, endSession]);

  useEffect(() => {
    if (!auth.isInitialized || !auth.accessToken) return;
    sessionEnded.current = false;
    const stored = readLastActivityAt();
    if (stored !== null && isIdleExpired(stored, Date.now())) { void endSession(); return; }
    const initial = stored ?? Date.now();
    if (stored === null) writeLastActivityAt(initial);
    const timer = window.setTimeout(() => setLastActivity(initial), 0);
    lastServerRefresh.current = Date.now();
    return () => window.clearTimeout(timer);
  }, [auth.isInitialized, auth.accessToken, endSession]);

  useEffect(() => {
    if (previousPath.current !== pathname) {
      previousPath.current = pathname;
      recordActivity();
    }
  }, [pathname, recordActivity]);

  useEffect(() => {
    if (!auth.accessToken) return;
    let lastWrite = 0;
    const onActivity = () => {
      const now = Date.now();
      const shouldPersist = now - lastWrite >= 5000;
      if (shouldPersist) lastWrite = now;
      recordActivity(now, shouldPersist);
    };
    const onVisibility = () => {
      if (document.visibilityState === "visible") checkDeadline();
    };
    const checkDeadline = () => {
      const latest = readLastActivityAt() ?? lastActivity ?? Date.now();
      if (isIdleExpired(latest, Date.now())) void endSession();
      else { setLastActivity(latest); setWarning(Date.now() - latest >= IDLE_WARNING_MS); }
    };
    const onStorage = (event: StorageEvent) => {
      if (event.key !== IDLE_ACTIVITY_STORAGE_KEY || !event.newValue) return;
      const timestamp = Number(event.newValue);
      if (Number.isFinite(timestamp)) { setLastActivity(timestamp); setWarning(false); }
    };
    const events: Array<keyof WindowEventMap> = ["click", "keydown", "pointerdown", "touchstart"];
    events.forEach((event) => window.addEventListener(event, onActivity, { passive: true }));
    window.addEventListener("focus", checkDeadline);
    window.addEventListener("storage", onStorage);
    document.addEventListener("visibilitychange", onVisibility);
    checkDeadline();
    return () => {
      events.forEach((event) => window.removeEventListener(event, onActivity));
      window.removeEventListener("focus", checkDeadline);
      window.removeEventListener("storage", onStorage);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, [auth.accessToken, endSession, lastActivity, recordActivity]);

  useEffect(() => {
    if (!auth.accessToken || lastActivity === null) return;
    return scheduleIdleTimers(lastActivity, () => {
      const latest = readLastActivityAt() ?? lastActivity;
      if (isIdleExpired(latest, Date.now())) void endSession();
      else setWarning(Date.now() - latest >= IDLE_WARNING_MS);
    }, () => {
      const latest = readLastActivityAt() ?? lastActivity;
      if (isIdleExpired(latest, Date.now())) void endSession();
      else setLastActivity(latest);
    }, Date.now());
  }, [auth.accessToken, endSession, lastActivity]);

  const continueSession = async () => {
    const tokens = await refreshSession(dispatch);
    if (!tokens) { void endSession(); return; }
    lastServerRefresh.current = Date.now();
    recordActivity(Date.now());
  };

  if (!warning || !auth.accessToken) return null;
  return <div role="presentation" className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4">
    <section role="dialog" aria-modal="true" aria-labelledby="idle-warning-title" className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl">
      <h2 id="idle-warning-title" className="text-lg font-semibold">{translate(locale, "idleSession.warningTitle")}</h2>
      <p className="mt-3 text-sm text-zinc-700">{translate(locale, "idleSession.warning")}</p>
      <div className="mt-6 flex justify-end gap-3">
        <button type="button" className="rounded border px-4 py-2" onClick={() => void endSession("manual")}>{translate(locale, "idleSession.logout")}</button>
        <button type="button" className="rounded bg-zinc-900 px-4 py-2 text-white" onClick={() => void continueSession()}>{translate(locale, "idleSession.continue")}</button>
      </div>
    </section>
  </div>;
}
