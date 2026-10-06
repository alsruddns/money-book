import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import React from "react";
import { renderToStaticMarkup } from "react-dom/server";
import ts from "typescript";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
const localRequire = createRequire(import.meta.url);
function load(file, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(root, file), "utf8");
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(js, { module: mod, exports: mod.exports, require: (name) => name in mocks ? mocks[name] : name === "@/common/format/dateTime" ? load("common/format/dateTime.ts") : name === "@/common/format/money" ? { formatNumber: (value) => Number(value).toLocaleString("ko-KR"), formatMoney: (value) => `${Number(value).toLocaleString("ko-KR")}${String.fromCharCode(0xC6D0)}`, formatCurrency: (value) => `${Number(value).toLocaleString("ko-KR")}${String.fromCharCode(0xC6D0)}`, formatCount: (value, unit = String.fromCharCode(0xAC74)) => `${Number(value).toLocaleString("ko-KR")}${unit}` } : localRequire(name), ...globals });
  return mod.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const baseApi = { injectEndpoints: ({ endpoints }) => endpoints(builder) };

test("session and logout endpoints match Backend contracts and use scoped session cache tags", () => {
  const account = load("account/controller/accountApi.ts", { "@/common/api/baseApi": { baseApi } }).accountApi;
  assert.equal(account.getAccountSessions.query(), "account/sessions");
  assert.deepEqual(plain(account.getAccountSessions.providesTags), ["AccountSessions"]);
  assert.deepEqual(plain(account.revokeAccountSession.query(17)), { url: "account/sessions/17", method: "DELETE" });
  assert.deepEqual(plain(account.revokeAccountSession.invalidatesTags(undefined, undefined)), ["AccountSessions"]);
  assert.deepEqual(plain(account.revokeAccountSession.invalidatesTags(undefined, { status: 404 })), []);
  assert.deepEqual(plain(account.logoutAllAccountSessions.query()), { url: "account/sessions/logout-all", method: "POST" });
  const auth = load("auth/controller/authApi.ts", { "@/common/api/baseApi": { baseApi } }).authApi;
  assert.deepEqual(plain(auth.logout.query()), { url: "auth/logout", method: "POST" });
});

test("session DTO preserves Backend field names and omits token/session secret fields", () => {
  load("account/dto/res/RefreshSessionResponse.ts");
  const source = fs.readFileSync(path.join(root, "account/dto/res/RefreshSessionResponse.ts"), "utf8");
  for (const field of ["sessionUid", "current", "userAgent", "ipAddress", "createdAt", "lastUsedAt", "expiresAt"]) assert.match(source, new RegExp(`${field}\\??:`));
  assert.doesNotMatch(source, /refreshToken|tokenHash|sid|sessionKey/i);
});

test("session query sorts current session first and preserves API session objects", () => {
  const records = [
    { sessionUid: 10, current: false },
    { sessionUid: 11, current: true },
    { sessionUid: 12, current: false },
  ];
  const hook = load("account/hooks/useAccountSessions.ts", {
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "load error" },
    "../controller/accountApi": { useGetAccountSessionsQuery: () => ({ currentData: records, isLoading: false, isUninitialized: false, isFetching: false, isError: false }) },
  }).useAccountSessions();
  assert.deepEqual(plain(hook.sessions.map((session) => session.sessionUid)), [11, 10, 12]);
  assert.equal(hook.sessions[0].current, true);
});

test("device/date labels identify common browsers and safely fall back", () => {
  const labels = load("account/sessionLabels.ts");
  assert.equal(labels.describeSessionDevice("Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/120.0 Safari/537.36"), "Chrome · Windows");
  assert.equal(labels.describeSessionDevice("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Version/17.0 Mobile Safari/604.1"), "Safari · iPhone/iPad");
  assert.equal(labels.describeSessionDevice("unknown agent"), "알 수 없는 기기");
  assert.equal(labels.describeSessionDevice(null), "알 수 없는 기기");
  assert.equal(labels.formatSessionDateTime(null), "확인 불가");
  assert.equal(labels.formatSessionDateTime("invalid"), "확인 불가");
  assert.equal(labels.formatSessionDateTime("2026-10-02T15:15:00"), "2026.10.03 00:15");
});

test("session section shows current badge, IP fallback, dates, and separate logout actions", () => {
  const session = { sessionUid: 3, current: true, userAgent: null, ipAddress: null, createdAt: "2026-10-02T15:15:00", lastUsedAt: null, expiresAt: "2026-11-02T15:15:00" };
  const otherSession = { ...session, sessionUid: 4, current: false, userAgent: "Firefox/130.0 Windows NT 10.0" };
  const { default: Section } = load("account/components/SessionSection.tsx", {
    react: { ...React, useState: () => [true, () => {}] },
    "@/common/format/money": { formatCount: (value, unit = "?") => `${value}${unit}` },
    "../sessionLabels": load("account/sessionLabels.ts"),
    "../hooks/useAccountSessions": { useAccountSessions: () => ({ sessions: [session, otherSession], isLoading: false, isFetching: false, isError: false }) },
    "../hooks/useRevokeAccountSession": { useRevokeAccountSession: () => ({ isLoading: false }) },
    "../hooks/useLogoutAllSessions": { useLogoutAllSessions: () => ({ isLoading: false }) },
    "@/auth/hooks/useLogout": { useLogout: () => ({ isLoading: false }) },
  });
  const markup = renderToStaticMarkup(React.createElement(Section));
  assert.match(markup, /현재 세션/);
  assert.match(markup, /알 수 없는 기기/);
  assert.match(markup, /확인 불가/);
  assert.match(markup, /이 기기 로그아웃/);
  assert.match(markup, /모든 기기에서 로그아웃/);
  assert.match(markup, /기기 정보/);
});

test("revoking another session confirms and sends its sessionUid without clearing current auth", async () => {
  const sent = [];
  const confirms = [];
  const { useRevokeAccountSession } = load("account/hooks/useRevokeAccountSession.ts", {
    react: { useState: (initial) => [initial, () => {}] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "revoke failed" },
    "../controller/accountApi": { useRevokeAccountSessionMutation: () => [(uid) => ({ unwrap: async () => sent.push(uid) }), { isLoading: false }] },
  }, { window: { confirm: (message) => { confirms.push(message); return true; } } });
  assert.equal(await useRevokeAccountSession().revoke(37), true);
  assert.deepEqual(sent, [37]);
  assert.match(confirms[0], /이 세션에서 로그아웃/);
});

function loadLogoutHarness({ fail = false, confirms = true } = {}) {
  const calls = [];
  const dispatches = [];
  const routes = [];
  const prompts = [];
  const deps = {
    "next/navigation": { useRouter: () => ({ replace: (route) => routes.push(route) }) },
    "react-redux": { useDispatch: () => (action) => dispatches.push(action) },
    "@/common/api/baseApi": { baseApi: { util: { resetApiState: () => ({ type: "RESET_API" }) } } },
    "@/auth/session/clearLocalSession": { clearLocalSession: (dispatch, reset) => { calls.push("clear-tokens"); dispatch({ type: "CLEAR_AUTH" }); reset(); } },
    "@/store/store": {},
  };
  const globals = { window: { confirm: (message) => { prompts.push(message); return confirms; } } };
  const mutation = () => [() => ({ unwrap: async () => { calls.push("request"); if (fail) throw new Error("network"); } }), { isLoading: false }];
  return { calls, dispatches, routes, prompts, deps, globals, mutation };
}

test("current-device logout posts server revoke, then clears local auth/cache even if the request fails", async () => {
  for (const fail of [false, true]) {
    const h = loadLogoutHarness({ fail });
    const { useLogout } = load("auth/hooks/useLogout.ts", { ...h.deps, "../controller/authApi": { useLogoutMutation: h.mutation } }, h.globals);
    // eslint-disable-next-line react-hooks/rules-of-hooks -- invoke the hook once for each isolated test harness.
    await useLogout().logout();
    assert.deepEqual(h.calls, ["request", "clear-tokens"]);
    assert.deepEqual(plain(h.dispatches), [{ type: "CLEAR_AUTH" }, { type: "RESET_API" }]);
    assert.deepEqual(h.routes, [fail ? "/login?reason=logout-incomplete" : "/login?reason=logged-out"]);
    assert.match(h.prompts[0], /이 기기에서 로그아웃/);
  }
});

test("logout-all requires confirmation and only clears auth after successful Backend response", async () => {
  for (const fail of [false, true]) {
    const h = loadLogoutHarness({ fail });
    let errorMessage = null;
    const { useLogoutAllSessions } = load("account/hooks/useLogoutAllSessions.ts", { ...h.deps,
      react: { useState: (initial) => [initial, (value) => { errorMessage = value; }] },
      "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "logout all failed" },
      "../controller/accountApi": { useLogoutAllAccountSessionsMutation: h.mutation },
    }, h.globals);
    // eslint-disable-next-line react-hooks/rules-of-hooks -- each loop iteration gets a fresh isolated hook harness.
    const hook = useLogoutAllSessions();
    assert.equal(await hook.logoutAll(), !fail);
    assert.match(h.prompts[0], /현재 기기를 포함한 모든 로그인 세션/);
    assert.deepEqual(h.calls, fail ? ["request"] : ["request", "clear-tokens"]);
    assert.deepEqual(h.routes, fail ? [] : ["/login?reason=sessions-ended"]);
    if (fail) assert.equal(errorMessage, "logout all failed");
  }
  const h = loadLogoutHarness({ confirms: false });
  const { useLogoutAllSessions } = load("account/hooks/useLogoutAllSessions.ts", { ...h.deps,
    react: { useState: (initial) => [initial, () => {}] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "logout all failed" },
    "../controller/accountApi": { useLogoutAllAccountSessionsMutation: h.mutation },
  }, h.globals);
  assert.equal(await useLogoutAllSessions().logoutAll(), false);
  assert.deepEqual(h.calls, []);
});

test("password change and refresh use rotation cleanup without decoding or exposing token payloads", () => {
  const passwordHook = fs.readFileSync(path.join(root, "account/hooks/useUpdateAccountPassword.ts"), "utf8");
  const baseQuery = fs.readFileSync(path.join(root, "common/api/baseApi.ts"), "utf8");
  assert.match(passwordHook, /clearLocalSession/);
  assert.match(passwordHook, /reason=password-changed/);
  assert.match(baseQuery, /"refreshToken" in data/);
  assert.match(baseQuery, /tokenStorage\.setTokens\(updated\)/);
  assert.match(baseQuery, /let refreshPromise: Promise<boolean> \| null = null/);
  assert.doesNotMatch(baseQuery, /atob\(|jwtDecode|decodeJwt/);
  assert.doesNotMatch(passwordHook, /localStorage|sessionStorage|console\.(log|error)/);
});
