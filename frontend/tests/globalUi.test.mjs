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

const localRequire = createRequire(import.meta.url);
const testDirectory = path.dirname(fileURLToPath(import.meta.url));

function loadModule(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(testDirectory, "../src", relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule, exports: compiledModule.exports,
    require: (name) => name in mocks ? mocks[name] : name === "@/i18n/messages" ? { translate: (_locale, key) => ({ "navigation.myBooks": "내 가계부", "navigation.invitations": "받은 초대", "navigation.board": "게시판", "navigation.account": "계정 관리", "navigation.admin": "관리자" })[key] ?? key } : name === "@/i18n/config" ? {} : localRequire(name),
    ...globals,
  });
  return compiledModule.exports;
}

test("shared number formatters add grouping without changing currency/count units", () => {
  const { formatNumber, formatMoney, formatCount, formatCurrency } = loadModule("common/format/money.ts");
  assert.equal(formatNumber(10000), "10,000");
  assert.equal(formatMoney(10000), "10,000원");
  assert.equal(formatCurrency(1530000), "1,530,000원");
  assert.equal(formatCount(12345), "12,345건");
  assert.equal(formatCount(10000, "개"), "10,000개");
  assert.equal(formatNumber(Number.NaN), "0");
});

test("global navigation exposes role-specific entries and preserves UID URLs", () => {
  const { getGlobalNavItems, isGlobalHeaderHidden } = loadModule("common/components/globalNavigation.ts");
  for (const role of ["USER", null, "UNKNOWN"]) {
    const items = getGlobalNavItems(role);
    assert.deepEqual(JSON.parse(JSON.stringify(items.map((item) => item.href))), ["/books", "/books/invitations", "/board", "/account"]);
  }
  for (const role of ["SYSTEM_ADMIN", "SUPER_ADMIN"]) {
    const items = getGlobalNavItems(role);
    assert.ok(items.some((item) => item.href === "/admin"));
    assert.deepEqual(JSON.parse(JSON.stringify(items.map((item) => item.href))), ["/books", "/books/invitations", "/board", "/admin", "/account"]);
  }
  assert.equal(isGlobalHeaderHidden("/login"), true);
  assert.equal(isGlobalHeaderHidden("/account"), false);
  assert.equal(getGlobalNavItems("USER")[0].href, "/books");
  assert.doesNotMatch(getGlobalNavItems("USER").map((item) => item.href).join(" "), /\/books\/\d+,/);
});

test("global header uses shared logout and includes a mobile navigation control", () => {
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/components/GlobalHeader.tsx"), "utf8");
  assert.match(source, /useLogout\(\)/);
  assert.match(source, /aria-controls="global-mobile-menu"/);
  assert.match(source, /md:hidden/);
  assert.match(source, /getGlobalNavItems\(role, locale\)/);
});

test("session section starts compact and reveals existing session actions when expanded", () => {
  const sessions = {
    sessions: [{ sessionUid: 8, current: true, userAgent: "Chrome/120 Windows", ipAddress: "192.0.2.1", createdAt: "2026-10-03T00:00:00Z", lastUsedAt: null, expiresAt: "2026-10-04T00:00:00Z" }],
    isLoading: false, isFetching: false, isError: false, retry: () => {},
  };
  function render(expanded) {
    const { default: SessionSection } = loadModule("account/components/SessionSection.tsx", {
      react: { useState: () => [expanded, () => {}] },
      "../sessionLabels": { describeSessionDevice: () => "Chrome · Windows", formatSessionDateTime: () => "2026. 10. 03. 09:00" },
      "../hooks/useAccountSessions": { useAccountSessions: () => sessions },
      "../hooks/useRevokeAccountSession": { useRevokeAccountSession: () => ({ isLoading: false, errorMessage: null, revoke: async () => true }) },
      "../hooks/useLogoutAllSessions": { useLogoutAllSessions: () => ({ isLoading: false, errorMessage: null, logoutAll: async () => true }) },
      "@/common/format/money": { formatCount: (value, unit = "?") => `${value}${unit}` },
      "@/auth/hooks/useLogout": { useLogout: () => ({ isLoading: false, logout: async () => {} }) },
    });
    return renderToStaticMarkup(React.createElement(SessionSection));
  }
  const compact = render(false);
  assert.match(compact, /Chrome/);
  assert.doesNotMatch(compact, /id="account-session-list"/);
  assert.doesNotMatch(compact, /192\.0\.2\.1/);
  const expandedMarkup = render(true);
  assert.match(expandedMarkup, /id="account-session-list"/);
  assert.match(expandedMarkup, /192\.0\.2\.1/);
  assert.match(expandedMarkup, /로그아웃/);
});

test("root layout keeps global header mounted and account layout links back to books", () => {
  const root = fs.readFileSync(path.join(testDirectory, "../src/app/layout.tsx"), "utf8");
  const account = fs.readFileSync(path.join(testDirectory, "../src/app/account/layout.tsx"), "utf8");
  const books = fs.readFileSync(path.join(testDirectory, "../src/app/books/layout.tsx"), "utf8");
  assert.match(root, /<GlobalHeader\s*\/>/);
  assert.match(account, /href="\/books"/);
  assert.doesNotMatch(books, /<header/);
});

test("cached auth user data survives route remount refetch without a protected-page loading flash", () => {
  const currentUser = { userUid: 19, nickname: "reader", status: "ACTIVE" };
  const { useCurrentUser } = loadModule("auth/hooks/useCurrentUser.ts", {
    "react-redux": { useSelector: (select) => select({ auth: { accessToken: "a", refreshToken: "r", isInitialized: true } }) },
    "../controller/authApi": { useGetCurrentUserQuery: () => ({ data: currentUser, isUninitialized: false, isFetching: true, isSuccess: true }) },
  });
  const state = useCurrentUser();
  assert.equal(state.currentUser, currentUser);
  assert.equal(state.isAuthenticated, true);
  assert.equal(state.isLoading, false);
});
