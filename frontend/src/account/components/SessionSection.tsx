"use client";

import { describeSessionDevice, formatSessionDateTime } from "../sessionLabels";
import { useAccountSessions } from "../hooks/useAccountSessions";
import { useRevokeAccountSession } from "../hooks/useRevokeAccountSession";
import { useLogoutAllSessions } from "../hooks/useLogoutAllSessions";
import { useLogout } from "@/auth/hooks/useLogout";

export default function SessionSection() {
  const sessions = useAccountSessions();
  const revoke = useRevokeAccountSession();
  const logoutAll = useLogoutAllSessions();
  const logout = useLogout();

  return <section aria-labelledby="sessions-title" className="rounded-xl border border-zinc-200 bg-white p-5 sm:p-6">
    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h2 id="sessions-title" className="text-lg font-semibold">로그인된 기기</h2>
        <p className="mt-1 text-sm text-zinc-600">현재 로그인된 세션을 확인하고 원하지 않는 기기에서 로그아웃할 수 있습니다.</p>
      </div>
      <button type="button" onClick={() => void logoutAll.logoutAll()} disabled={logoutAll.isLoading}
        className="min-h-11 shrink-0 rounded-lg border border-red-300 px-4 text-sm font-medium text-red-800 disabled:opacity-50">
        {logoutAll.isLoading ? "모든 세션 종료 중..." : "모든 기기에서 로그아웃"}
      </button>
    </div>

    {sessions.isLoading ? <p role="status" className="mt-5">세션 목록을 불러오는 중...</p> : sessions.isError ? <div role="alert" className="mt-5 rounded-lg bg-red-50 p-4 text-sm text-red-800">
      <p>{sessions.errorMessage}</p>
      <button type="button" onClick={() => void sessions.retry()} className="mt-3 min-h-10 rounded-lg border border-red-300 bg-white px-3">다시 시도</button>
    </div> : sessions.sessions.length === 0 ? <p className="mt-5 rounded-lg bg-zinc-50 p-4 text-sm text-zinc-700">활성 로그인 세션이 없습니다. 다시 로그인하면 현재 세션이 등록됩니다.</p> : <ul className="mt-5 space-y-3">
      {sessions.sessions.map((session) => <li key={session.sessionUid} className="rounded-lg border border-zinc-200 p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h3 className="font-semibold">{describeSessionDevice(session.userAgent)}</h3>
              {session.current && <span className="rounded-full bg-blue-100 px-2.5 py-1 text-xs font-semibold text-blue-900">현재 세션</span>}
            </div>
            <dl className="mt-3 grid gap-x-5 gap-y-2 text-sm sm:grid-cols-2">
              <div><dt className="text-zinc-500">IP 주소</dt><dd className="mt-0.5 break-all">{session.ipAddress?.trim() || "확인 불가"}</dd></div>
              <div><dt className="text-zinc-500">로그인 시각</dt><dd className="mt-0.5">{formatSessionDateTime(session.createdAt)}</dd></div>
              <div><dt className="text-zinc-500">최근 사용</dt><dd className="mt-0.5">{formatSessionDateTime(session.lastUsedAt)}</dd></div>
              <div><dt className="text-zinc-500">만료 시각</dt><dd className="mt-0.5">{formatSessionDateTime(session.expiresAt)}</dd></div>
            </dl>
            {session.userAgent && <details className="mt-3 text-xs text-zinc-500">
              <summary className="min-h-8 cursor-pointer py-1">기기 정보 원문</summary>
              <p className="break-all">{session.userAgent}</p>
            </details>}
          </div>
          <button type="button" disabled={revoke.isLoading || logout.isLoading}
            onClick={() => session.current ? void logout.logout() : void revoke.revoke(session.sessionUid)}
            className="min-h-11 shrink-0 rounded-lg border border-zinc-300 px-4 text-sm font-medium disabled:opacity-50">
            {session.current ? "이 기기 로그아웃" : "로그아웃"}
          </button>
        </div>
      </li>)}
    </ul>}
    {sessions.isFetching && !sessions.isLoading && <p role="status" className="mt-3 text-xs text-zinc-500">세션 정보를 업데이트하고 있습니다.</p>}
    {revoke.errorMessage && <p role="alert" className="mt-3 text-sm text-red-700">{revoke.errorMessage}</p>}
    {logoutAll.errorMessage && <p role="alert" className="mt-3 text-sm text-red-700">{logoutAll.errorMessage}</p>}
  </section>;
}
