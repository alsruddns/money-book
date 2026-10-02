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
const account = {
  userUid: 41, nickname: "Jamie", status: "ACTIVE", systemRole: "USER", providers: ["LOCAL"],
  loginId: "jamie@example.test", regTime: "2026-10-01T09:00:00", modTime: "2026-10-02T09:00:00",
};

function loadModule(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(testDirectory, "../src", relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule, exports: compiledModule.exports,
    require: (name) => name in mocks ? mocks[name] : localRequire(name),
    ...globals,
  });
  return compiledModule.exports;
}

const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const baseApi = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };

test("Account endpoints match backend routes and invalidate AccountMe/AuthMe narrowly", () => {
  const { accountApi: api } = loadModule("account/controller/accountApi.ts", { "@/common/api/baseApi": baseApi });
  assert.equal(api.getAccountMe.query(), "account/me");
  assert.deepEqual(plain(api.updateAccountProfile.query({ nickname: "Jamie" })), {
    url: "account/profile", method: "PATCH", body: { nickname: "Jamie" },
  });
  const passwordRequest = { currentPassword: "old", newPassword: "new", newPasswordConfirm: "new" };
  assert.deepEqual(plain(api.updateAccountPassword.query(passwordRequest)), {
    url: "account/password", method: "PATCH", body: passwordRequest,
  });
  assert.deepEqual(plain(api.withdrawAccount.query({ currentPassword: "verify" })), {
    url: "account", method: "DELETE", body: { currentPassword: "verify" },
  });
  assert.deepEqual(plain(api.getAccountMe.providesTags), ["AccountMe"]);
  assert.deepEqual(plain(api.updateAccountProfile.invalidatesTags({}, undefined)), ["AccountMe", "AuthMe"]);
  assert.deepEqual(plain(api.updateAccountProfile.invalidatesTags(undefined, { status: 400 })), []);
  assert.deepEqual(plain(api.updateAccountPassword.invalidatesTags({}, undefined)), ["AccountMe"]);
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/api/baseApi.ts"), "utf8");
  assert.match(source, /baseUrl:\s*"\/api"/);
  assert.match(source, /"AccountMe"/);
  assert.match(source, /"AuthMe"/);
});

test("auth/me has a cache tag for nickname refresh after profile updates", () => {
  const { authApi } = loadModule("auth/controller/authApi.ts", { "@/common/api/baseApi": baseApi });
  assert.equal(authApi.getCurrentUser.query(), "auth/me");
  assert.deepEqual(plain(authApi.getCurrentUser.providesTags), ["AuthMe"]);
});

test("account response DTO mirrors backend names, optional LOCAL login ID, providers and timestamps", () => {
  const dto = loadModule("account/dto/res/AccountMeResDto.ts");
  const response = plain(account);
  assert.deepEqual(Object.keys(response), ["userUid", "nickname", "status", "systemRole", "providers", "loginId", "regTime", "modTime"]);
  assert.ok(dto);
});

test("nickname validation trims, rejects blank values and enforces the backend 50 character limit", async () => {
  const { validateNickname } = loadModule("account/accountValidation.ts");
  assert.equal(validateNickname("   "), "닉네임을 입력해 주세요.");
  assert.equal(validateNickname("n".repeat(51)), "닉네임은 50자 이내로 입력해 주세요.");
  assert.equal(validateNickname(" n "), null);
  const hook = loadModule("account/hooks/useUpdateAccountProfile.ts", {
    react: { useState: (initial) => [initial, () => {}] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "서버 오류" },
    "../accountValidation": { validateNickname },
    "../controller/accountApi": { useUpdateAccountProfileMutation: () => [(body) => ({ unwrap: async () => calls.push(body) }), { isLoading: false }] },
  });
  const calls = [];
  const update = hook.useUpdateAccountProfile();
  assert.equal(await update.updateNickname(" same "), true);
  assert.deepEqual(plain(calls), [{ nickname: "same" }]);
});

test("password validation covers required fields, matching, character and UTF-8 byte limits", () => {
  const { validatePasswordUpdate } = loadModule("account/accountValidation.ts", {}, { TextEncoder });
  const valid = { currentPassword: "old-pass", newPassword: "new-pass", newPasswordConfirm: "new-pass" };
  assert.match(validatePasswordUpdate({ ...valid, currentPassword: " " }), /현재 비밀번호/);
  assert.match(validatePasswordUpdate({ ...valid, newPassword: " " }), /새 비밀번호를 입력/);
  assert.match(validatePasswordUpdate({ ...valid, newPasswordConfirm: " " }), /확인/);
  assert.match(validatePasswordUpdate({ ...valid, newPasswordConfirm: "other" }), /일치/);
  assert.match(validatePasswordUpdate({ ...valid, newPassword: "a".repeat(73), newPasswordConfirm: "a".repeat(73) }), /72자/);
  assert.equal(validatePasswordUpdate({ ...valid, newPassword: "가".repeat(24), newPasswordConfirm: "가".repeat(24) }), null);
  assert.match(validatePasswordUpdate({ ...valid, newPassword: "가".repeat(25), newPasswordConfirm: "가".repeat(25) }), /72바이트/);
  assert.equal(validatePasswordUpdate({ ...valid, newPassword: "😀".repeat(18), newPasswordConfirm: "😀".repeat(18) }), null);
  assert.match(validatePasswordUpdate({ ...valid, currentPassword: "same", newPassword: "same", newPasswordConfirm: "same" }), /달라야/);
});

test("password hook sends the backend DTO, returns backend errors and never logs credentials", async () => {
  const calls = [];
  let hookError = null;
  const { useUpdateAccountPassword } = loadModule("account/hooks/useUpdateAccountPassword.ts", {
    react: { useState: (initial) => [initial, (value) => { hookError = value; }] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "현재 비밀번호가 올바르지 않습니다." },
    "../accountValidation": { validatePasswordUpdate: () => null },
    "../controller/accountApi": { useUpdateAccountPasswordMutation: () => [(body) => ({
      unwrap: async () => { calls.push(body); if (body.currentPassword === "wrong") throw new Error("bad"); },
    }), { isLoading: false }] },
  });
  const hook = useUpdateAccountPassword();
  const request = { currentPassword: "old", newPassword: "new", newPasswordConfirm: "new" };
  assert.equal(await hook.updatePassword(request), true);
  assert.deepEqual(plain(calls), [request]);
  assert.equal(await hook.updatePassword({ ...request, currentPassword: "wrong" }), false);
  assert.equal(hookError, "현재 비밀번호가 올바르지 않습니다.");
  const source = fs.readFileSync(path.join(testDirectory, "../src/account/hooks/useUpdateAccountPassword.ts"), "utf8");
  assert.doesNotMatch(source, /console\.(log|error)|localStorage|sessionStorage|tokenStorage|authSlice/);
});

function renderAccountView(accountResponse) {
  const { default: View } = loadModule("account/components/AccountManagementView.tsx", {
    react: React,
    "../accountManagementLabels": loadModule("account/accountManagementLabels.ts"),
    "../hooks/useAccountMe": { useAccountMe: () => ({ account: accountResponse, isLoading: false, isError: false }) },
    "../hooks/useUpdateAccountProfile": { useUpdateAccountProfile: () => ({ isLoading: false }) },
    "../hooks/useUpdateAccountPassword": { useUpdateAccountPassword: () => ({ isLoading: false }) },
    "../hooks/useWithdrawAccount": { useWithdrawAccount: () => ({ isLoading: false, errorMessage: null, withdraw: async () => true }) },
  });
  return renderToStaticMarkup(React.createElement(View));
}

test("LOCAL users see password form; OAuth-only users get guidance and no password inputs", () => {
  const localMarkup = renderAccountView(account);
  assert.match(localMarkup, /현재 비밀번호/);
  assert.match(localMarkup, /type="password"/);
  const oauthMarkup = renderAccountView({ ...account, providers: ["GOOGLE"], loginId: null });
  assert.match(oauthMarkup, /소셜 로그인 계정은 이 화면에서 비밀번호를 변경할 수 없습니다/);
  assert.doesNotMatch(oauthMarkup, /type="password"|현재 비밀번호/);
  assert.match(oauthMarkup, /Google/);
});

test("withdrawal UI is available to LOCAL USER and SYSTEM_ADMIN but blocked for SUPER_ADMIN and OAuth-only accounts", () => {
  const localMarkup = renderAccountView(account);
  assert.match(localMarkup, /회원 탈퇴/);
  assert.match(localMarkup, /소유권을 이전/);
  const adminMarkup = renderAccountView({ ...account, systemRole: "SYSTEM_ADMIN" });
  assert.match(adminMarkup, /회원 탈퇴 진행/);
  const superAdminMarkup = renderAccountView({ ...account, systemRole: "SUPER_ADMIN" });
  assert.match(superAdminMarkup, /최고 관리자는 이 화면에서 탈퇴할 수 없습니다/);
  assert.doesNotMatch(superAdminMarkup, /회원 탈퇴 진행/);
  const oauthMarkup = renderAccountView({ ...account, providers: ["GOOGLE"], loginId: null });
  assert.match(oauthMarkup, /현재 소셜 로그인 계정은 이 화면에서 탈퇴할 수 없습니다/);
  assert.doesNotMatch(oauthMarkup, /회원 탈퇴 진행/);
});

test("withdrawal keeps auth on failure and clears tokens, auth and API cache only after success", async () => {
  const tokenEvents = [];
  const dispatched = [];
  const routes = [];
  let responseError = { data: { code: "OWNED_MONEY_BOOK_EXISTS", message: "owned" } };
  let sent;
  const { useWithdrawAccount } = loadModule("account/hooks/useWithdrawAccount.ts", {
    react: { useState: (initial) => [initial, (next) => { tokenEvents.push(["error", next]); }] },
    "next/navigation": { useRouter: () => ({ replace: (path) => routes.push(path) }) },
    "react-redux": { useDispatch: () => (action) => dispatched.push(action) },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "fallback" },
    "@/common/api/baseApi": { baseApi: { util: { resetApiState: () => ({ type: "RESET_API" }) } } },
    "@/auth/storage/tokenStorage": { tokenStorage: { clearTokens: () => tokenEvents.push(["clear"]) } },
    "@/auth/store/authSlice": { clearAuth: () => ({ type: "CLEAR_AUTH" }) },
    "@/store/store": {},
    "../controller/accountApi": { useWithdrawAccountMutation: () => [(body) => ({ unwrap: async () => { sent = body; if (responseError) throw responseError; } }), { isLoading: false }] },
  }, { TextEncoder });
  const hook = useWithdrawAccount();
  assert.equal(await hook.withdraw("pass"), false);
  assert.deepEqual(plain(sent), { currentPassword: "pass" });
  assert.match(tokenEvents.at(-1)[1], /소유 중인 가계부/);
  assert.deepEqual(dispatched, []);
  assert.deepEqual(routes, []);
  responseError = null;
  assert.equal(await hook.withdraw("pass"), true);
  assert.deepEqual(tokenEvents.filter(([event]) => event === "clear"), [["clear"]]);
  assert.deepEqual(plain(dispatched), [{ type: "CLEAR_AUTH" }, { type: "RESET_API" }]);
  assert.deepEqual(routes, ["/login"]);
  const source = fs.readFileSync(path.join(testDirectory, "../src/account/hooks/useWithdrawAccount.ts"), "utf8");
  assert.doesNotMatch(source, /console\.(log|error)|sessionStorage|localStorage/);
});

test("account screen shows read-only role/status/provider labels and account load retry", () => {
  const labels = loadModule("account/accountManagementLabels.ts");
  assert.equal(labels.accountStatusLabel("ACTIVE"), "활성");
  assert.equal(labels.accountStatusLabel("OTHER"), "OTHER");
  assert.equal(labels.accountRoleLabel("SYSTEM_ADMIN"), "시스템 관리자");
  assert.equal(labels.accountRoleLabel("SUPER_ADMIN"), "최고 관리자");
  assert.equal(labels.accountProviderLabel("KAKAO"), "Kakao");
  assert.equal(labels.accountProviderLabel("UNKNOWN"), "UNKNOWN");
  const { default: View } = loadModule("account/components/AccountManagementView.tsx", {
    react: React,
    "../accountManagementLabels": loadModule("account/accountManagementLabels.ts"),
    "../hooks/useAccountMe": { useAccountMe: () => ({ account: null, isLoading: false, isError: true, errorMessage: "잠시 후 다시 시도해 주세요.", retry: () => {} }) },
    "../hooks/useUpdateAccountProfile": { useUpdateAccountProfile: () => ({ isLoading: false }) },
    "../hooks/useUpdateAccountPassword": { useUpdateAccountPassword: () => ({ isLoading: false }) },
    "../hooks/useWithdrawAccount": { useWithdrawAccount: () => ({ isLoading: false, errorMessage: null, withdraw: async () => true }) },
  });
  const markup = renderToStaticMarkup(React.createElement(View));
  assert.match(markup, /잠시 후 다시 시도해 주세요/);
  assert.match(markup, /다시 시도/);
});

test("account route uses the existing role-neutral AuthGuard and links from books and admin navigation", () => {
  const { default: Layout } = loadModule("app/account/layout.tsx", {
    "@/auth/components/AuthGuard": { default: ({ children }) => React.createElement("div", { "data-guard": "auth" }, children) },
  });
  assert.match(renderToStaticMarkup(React.createElement(Layout, null, React.createElement("p", null, "계정"))), /data-guard="auth"/);
  const booksLayout = fs.readFileSync(path.join(testDirectory, "../src/app/books/layout.tsx"), "utf8");
  const adminShell = fs.readFileSync(path.join(testDirectory, "../src/admin/components/AdminShell.tsx"), "utf8");
  assert.match(booksLayout, /href="\/account"[^>]*>계정 관리/);
  assert.match(adminShell, /href="\/account"[^>]*>계정 관리/);
  const accountLayout = fs.readFileSync(path.join(testDirectory, "../src/app/account/layout.tsx"), "utf8");
  assert.match(accountLayout, /AuthGuard/);
  assert.doesNotMatch(accountLayout, /systemRole|isAdmin|isSuperAdmin/);
});

test("password success clears local form state and credentials are not persisted by account code", () => {
  const component = fs.readFileSync(path.join(testDirectory, "../src/account/components/AccountManagementView.tsx"), "utf8");
  assert.match(component, /setPasswordValues\(\{ \.\.\.emptyPassword \}\)/);
  assert.doesNotMatch(component, /localStorage|sessionStorage|tokenStorage|authSlice|console\.(log|error)/);
  const sourceFiles = [
    "account/hooks/useUpdateAccountPassword.ts",
    "account/dto/req/AccountPasswordUpdateReqDto.ts",
    "account/components/AccountManagementView.tsx",
  ].map((name) => fs.readFileSync(path.join(testDirectory, "../src", name), "utf8")).join("\n");
  assert.doesNotMatch(sourceFiles, /passwordHash|refreshToken|jwtPayload|providerUserId|oauthSecret/i);
});

test("profile form starts with current nickname, skips unchanged values, and clears password fields after success", async () => {
  const states = [];
  let stateIndex = 0;
  const calls = { profile: [], password: [] };
  const reactMock = {
    useState(initial) {
      const index = stateIndex++;
      if (!(index in states)) states[index] = initial;
      return [states[index], (next) => { states[index] = typeof next === "function" ? next(states[index]) : next; }];
    },
  };
  const { default: AccountView } = loadModule("account/components/AccountManagementView.tsx", {
    react: reactMock,
    "../accountManagementLabels": loadModule("account/accountManagementLabels.ts"),
    "../hooks/useAccountMe": { useAccountMe: () => ({ account, isLoading: false, isError: false }) },
    "../hooks/useUpdateAccountProfile": { useUpdateAccountProfile: () => ({
      isLoading: false,
      updateNickname: async (nickname) => { calls.profile.push(nickname); return true; },
    }) },
    "../hooks/useUpdateAccountPassword": { useUpdateAccountPassword: () => ({
      isLoading: false,
      updatePassword: async (request) => { calls.password.push(request); return true; },
    }) },
    "../hooks/useWithdrawAccount": { useWithdrawAccount: () => ({ isLoading: false, errorMessage: null, withdraw: async () => true }) },
  });
  function visit(value, elements) {
    if (!value || typeof value !== "object") return;
    if (Array.isArray(value)) { value.forEach((item) => visit(item, elements)); return; }
    if (React.isValidElement(value)) {
      elements.push(value);
      visit(value.props.children, elements);
    }
  }
  function renderElements() {
    stateIndex = 0;
    const outerElement = AccountView();
    const detailElement = outerElement.type(outerElement.props);
    const elements = [];
    visit(detailElement, elements);
    return elements;
  }
  let elements = renderElements();
  let forms = elements.filter((element) => element.type === "form");
  assert.equal(states[0], "Jamie");
  assert.equal(forms.length, 2);
  await forms[0].props.onSubmit({ preventDefault() {} });
  assert.deepEqual(calls.profile, []);

  const nicknameInput = elements.find((element) => element.type === "input" && element.props.id === "account-nickname");
  nicknameInput.props.onChange({ target: { value: "  Casey  " } });
  elements = renderElements();
  forms = elements.filter((element) => element.type === "form");
  await forms[0].props.onSubmit({ preventDefault() {} });
  assert.deepEqual(calls.profile, ["  Casey  "]);
  assert.equal(states[0], "Casey");

  const passwordValues = ["currentPassword", "newPassword", "newPasswordConfirm"];
  const passwordInputs = passwordValues.map((name) => elements.find((element) => element.type === "input" && element.props.name === name));
  for (const [index, input] of passwordInputs.entries()) input.props.onChange({ target: { value: ["old-pass", "new-pass", "new-pass"][index] } });
  elements = renderElements();
  forms = elements.filter((element) => element.type === "form");
  await forms[1].props.onSubmit({ preventDefault() {} });
  assert.deepEqual(plain(calls.password), [{ currentPassword: "old-pass", newPassword: "new-pass", newPasswordConfirm: "new-pass" }]);
  assert.deepEqual(plain(states[3]), { currentPassword: "", newPassword: "", newPasswordConfirm: "" });
});
