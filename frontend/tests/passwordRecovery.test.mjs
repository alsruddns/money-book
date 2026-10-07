import { resolveLocaleTestImport } from "./localeTestImports.mjs";
import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";
import ts from "typescript";

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
const require = createRequire(import.meta.url);
function load(file, mocks = {}) {
  const source = fs.readFileSync(path.join(root, file), "utf8");
  const js = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX } }).outputText;
  const mod = { exports: {} };
  vm.runInNewContext(js, { module: mod, exports: mod.exports, require: (name) => name in mocks ? mocks[name] : resolveLocaleTestImport(name, {}, require) });
  return mod.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const baseApi = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };
const text = (file) => fs.readFileSync(path.join(root, file), "utf8");

test("Backend recovery endpoint paths and HTTP methods are used as implemented", () => {
  const api = load("auth/controller/passwordRecoveryApi.ts", { "@/common/api/baseApi": baseApi }).passwordRecoveryApi;
  assert.equal(api.getSecurityQuestions.query(), "auth/security-questions");
  assert.deepEqual(plain(api.requestEmailVerification.query({ email: "a@b.test", purpose: "SIGNUP" })), { url: "auth/email-verifications/request", method: "POST", body: { email: "a@b.test", purpose: "SIGNUP" } });
  assert.deepEqual(plain(api.confirmEmailVerification.query({ verificationUid: 9, code: "123456" })), { url: "auth/email-verifications/confirm", method: "POST", body: { verificationUid: 9, code: "123456" } });
  assert.deepEqual(plain(api.requestPasswordResetEmail.query({ loginId: "x", email: "x@y.test" })), { url: "auth/password-recovery/email/request", method: "POST", body: { loginId: "x", email: "x@y.test" } });
  assert.equal(api.resetPasswordByEmail.query({ verificationToken: "g", newPassword: "n", newPasswordConfirm: "n" }).url, "auth/password-recovery/email/reset");
  assert.equal(api.resetPasswordByQuestion.query({ loginId: "x", questionCode: "A", answer: "a", newPassword: "n", newPasswordConfirm: "n" }).url, "auth/password-recovery/security-question/reset");
  assert.equal(api.resetPasswordByRecoveryCode.query({ loginId: "x", recoveryCode: "c", newPassword: "n", newPasswordConfirm: "n" }).url, "auth/password-recovery/recovery-code/reset");
});

test("Account Security API follows actual DTO names and narrowly invalidates its cache", () => {
  const api = load("auth/controller/passwordRecoveryApi.ts", { "@/common/api/baseApi": baseApi }).passwordRecoveryApi;
  assert.equal(api.getAccountSecurity.query(), "account/security");
  assert.deepEqual(plain(api.updateSecurityQuestion.query({ questionCode: "FAVORITE_COLOR", answer: "blue", currentPassword: "pw" })), { url: "account/security/security-question", method: "PATCH", body: { questionCode: "FAVORITE_COLOR", answer: "blue", currentPassword: "pw" } });
  assert.equal(api.regenerateRecoveryCodes.query({ currentPassword: "pw" }).url, "account/security/recovery-codes/regenerate");
  assert.deepEqual(plain(api.applyAccountEmail.query({ verificationToken: "grant" })), { url: "account/security/email", method: "PUT", body: { verificationToken: "grant" } });
  assert.deepEqual(plain(api.removeAccountEmail.query({ currentPassword: "pw" })), { url: "account/security/email", method: "DELETE", body: { currentPassword: "pw" } });
  assert.deepEqual(plain(api.updateSecurityQuestion.invalidatesTags(undefined, undefined)), ["AccountSecurity"]);
  assert.deepEqual(plain(api.updateSecurityQuestion.invalidatesTags(undefined, { status: 400 })), []);
});

test("signup DTO requires Backend security question and answer and only accepts a verification grant", () => {
  const dto = load("auth/dto/req/SignUpReqDto.ts");
  assert.ok(dto);
  const source = text("auth/dto/req/SignUpReqDto.ts");
  assert.match(source, /securityQuestionCode:\s*string/);
  assert.match(source, /securityAnswer:\s*string/);
  assert.match(source, /emailVerificationToken\?:\s*string/);
  assert.doesNotMatch(source, /email\??:\s*string/);
  assert.match(text("auth/dto/res/SignUpResDto.ts"), /recoveryCodes:\s*string\[\]/);
});

test("signup question options come from Backend and email can be skipped or verified first", () => {
  const source = text("auth/components/SignupForm.tsx");
  assert.match(source, /useGetSecurityQuestionsQuery/);
  assert.match(source, /purpose:\s*"SIGNUP"/);
  assert.match(source, /email\.trim\(\)\s*&&\s*!verificationToken/);
  assert.match(source, /emailVerificationToken:\s*verificationToken/);
  assert.match(source, /signup\.noEmailTip/);
  assert.match(source, /signup\.emailVerified/);
  assert.match(source, /autoComplete="off"/);
});

test("signup rejects answer boundary whitespace without trimming valid input", () => {
  const validation = load("auth/securityAnswerValidation.ts");
  assert.equal(validation.hasSecurityAnswerBoundaryWhitespace(" 라면"), true);
  assert.equal(validation.hasSecurityAnswerBoundaryWhitespace("라면 "), true);
  assert.equal(validation.hasSecurityAnswerBoundaryWhitespace(" 라면 "), true);
  assert.equal(validation.hasSecurityAnswerBoundaryWhitespace("라면"), false);
  const source = text("auth/components/SignupForm.tsx");
  assert.match(source, /hasSecurityAnswerBoundaryWhitespace\(form\.securityAnswer\)/);
  assert.match(source, /securityAnswerError/);
  assert.match(source, /signup\.invalidSecurityAnswer/);
  assert.doesNotMatch(source, /form\.securityAnswer\.trim\(\)/);
  assert.match(source, /signup\(\s*\{ \.\.\.form/);
});

test("signup keeps email send errors separate from submit-time unverified guidance", () => {
  const source = text("auth/components/SignupForm.tsx");
  assert.match(source, /setVerificationError\(getApiErrorMessage\(error/);
  assert.match(source, /showUnverifiedEmailError && !verificationToken/);
  assert.match(source, /setShowUnverifiedEmailError\(true\)/);
  assert.match(source, /setShowUnverifiedEmailError\(false\)/);
  assert.match(source, /setVerificationToken\(""\); setVerificationUid\(null\)/);
  assert.match(source, /setCode\(""\)/);
  assert.match(source, /emailVerificationToken: verificationToken/);
  assert.match(source, /!email \? <p/);
});

test("signup duplicate verified email gets a field error and focuses the email input", () => {
  const form = text("auth/components/SignupForm.tsx");
  const hook = text("auth/hooks/useSignup.ts");
  assert.match(hook, /getApiErrorCode\(error\) === "EMAIL_ALREADY_IN_USE"/);
  assert.match(form, /document\.getElementById\("signup-email"\)\?\.focus\(\)/);
  assert.match(form, /aria-invalid=\{duplicateEmailError\}/);
  assert.match(form, /signup\.duplicateEmail/);
  assert.match(form, /errorMessage && !duplicateEmailError/);
});

test("shared API error parsing exposes the backend duplicate email code", () => {
  const errors = load("common/api/getApiErrorMessage.ts");
  assert.equal(errors.getApiErrorCode({ status: 409, data: { code: "EMAIL_ALREADY_IN_USE" } }), "EMAIL_ALREADY_IN_USE");
  assert.equal(errors.getApiErrorCode({ status: 409, data: { code: "OTHER_ERROR" } }), "OTHER_ERROR");
  assert.equal(errors.getApiErrorCode({ status: 409, data: "invalid" }), null);
});

test("signup optional and verified email submission paths remain supported", () => {
  const form = text("auth/components/SignupForm.tsx");
  assert.match(form, /signup\.noEmailTip/);
  assert.match(form, /emailVerificationToken: verificationToken/);
  assert.match(text("auth/dto/req/SignUpReqDto.ts"), /emailVerificationToken\?:\s*string/);
});

test("signup only renders recovery codes from response and clears the one-time result", () => {
  const source = text("auth/components/SignupForm.tsx");
  assert.match(source, /setRecoveryCodes\(result\.recoveryCodes/);
  assert.match(source, /signup\.oneTime/);
  assert.match(source, /signup\.copyAll/);
  assert.match(source, /setRecoveryCodes\(null\).*setForm\(blank\).*setVerificationToken\(""\)/);
  assert.doesNotMatch(source, /localStorage|sessionStorage|console\.log/);
});

test("login links to forgot-password and honors Backend forced-change flag", () => {
  assert.match(text("auth/components/LoginForm.tsx"), /href="\/forgot-password"/);
  assert.match(text("auth/hooks/useLogin.ts"), /response\.passwordChangeRequired\s*\?\s*"\/change-required-password"/);
});

test("forgot page exposes only the three user recovery methods and generic email copy", () => {
  const source = text("auth/components/ForgotPasswordForm.tsx");
  for (const label of ["이메일", "보안 질문", "복구 코드"]) assert.ok(source.includes(label));
  assert.doesNotMatch(source, /SUPER_ADMIN|운영자 초기화/);
  assert.match(source, /입력한 정보와 일치하는 인증 이메일이 있는 경우 인증번호를 전송했습니다/);
  assert.match(source, /genericRecoveryError/);
  assert.match(source, /getApiErrorMessage/);
  assert.match(source, /getApiErrorMessage/);
  assert.match(text("common/api/getApiErrorMessage.ts"), /status === 429/);
});

test("forgot password consumes the Backend verification grant and has password confirmation", () => {
  const source = text("auth/components/ForgotPasswordForm.tsx");
  assert.match(source, /verificationToken/);
  assert.match(source, /verificationUid, code/);
  assert.match(source, /newPasswordConfirm/);
  assert.match(source, /보안 질문의 답변은 가입 시 입력한 내용과 정확하게 일치해야 합니다/);
  assert.match(source, /복구코드는 발급 시 한 번만 확인/);
});

test("forgot password route exists and is mobile constrained", () => {
  assert.ok(fs.existsSync(path.join(root, "app/[locale]/money/forgot-password/page.tsx")));
  assert.match(text("app/[locale]/money/forgot-password/page.tsx"), /max-w-lg/);
  assert.match(text("auth/components/ForgotPasswordForm.tsx"), /break-all|font-mono/);
});

test("account security displays missing question, Backend masked email, and recovery count", () => {
  const source = text("account/components/AccountSecuritySection.tsx");
  assert.match(source, /securityQuestionConfigured/);
  assert.match(source, /설정 안 됨/);
  assert.match(source, /security\.data\.maskedEmail/);
  assert.match(source, /recoveryCodesRemaining/);
  assert.doesNotMatch(source, /securityAnswerHash|verifiedEmail\b/);
});

test("question update requires new answer and current password without reading the old answer", () => {
  const source = text("account/components/AccountSecuritySection.tsx");
  assert.match(source, /updateQuestion\(\{ questionCode, answer, currentPassword \}\)/);
  assert.match(source, /새 답변/);
  assert.doesNotMatch(source, /현재 답변/);
});

test("regenerating recovery codes confirms replacement and clears one-time display state", () => {
  const source = text("account/components/AccountSecuritySection.tsx");
  assert.match(source, /새 복구코드를 발급하면 기존 복구코드는 모두 사용할 수 없게 됩니다/);
  assert.match(source, /regenerate\(\{ currentPassword: regeneratePassword \}\)/);
  assert.match(source, /regenerateState\.reset\(\)/);
  assert.match(source, /setRecoveryCodes\(null\)/);
});

test("email add and change require a verification grant; deletion uses supported endpoint", () => {
  const source = text("account/components/AccountSecuritySection.tsx");
  assert.match(source, /purpose:\s*"ACCOUNT_EMAIL"/);
  assert.match(source, /confirmCode\(\{ verificationUid, code: emailCode \}\)/);
  assert.match(source, /applyEmail\(\{ verificationToken \}\)/);
  assert.match(source, /removeEmail\(\{ currentPassword: password \}\)/);
});

test("account email duplicate shows an email field error and preserves its grant for retry", () => {
  const source = text("account/components/AccountSecuritySection.tsx");
  assert.match(source, /getApiErrorCode\(reason\) === "EMAIL_ALREADY_IN_USE"/);
  assert.match(source, /document\.getElementById\("account-recovery-email"\)\?\.focus\(\)/);
  assert.match(source, /aria-invalid=\{emailDuplicateError\}/);
  assert.match(source, /이미 다른 계정에서 사용 중인 이메일입니다/);
  assert.match(source, /setSuccess\("복구 이메일을 등록했습니다\."\)/);
  assert.match(source, /setSuccess\("복구 이메일을 삭제했습니다\."\)/);
  const saveEmail = source.match(/async function saveEmail\(\)[\s\S]*?async function deleteEmail/)?.[0] ?? "";
  const duplicateCatch = saveEmail.match(/catch \(reason\) \{([\s\S]*?)\n\s*\}/)?.[1] ?? "";
  assert.match(duplicateCatch, /setEmailDuplicateError\(true\)/);
  assert.doesNotMatch(duplicateCatch, /setVerificationToken\(""\)/);
});

test("Admin reset endpoint matches Backend and temporary secret remains transient", () => {
  const api = load("admin/controller/adminApi.ts", { "@/common/api/baseApi": { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } } }).adminApi;
  assert.deepEqual(plain(api.resetAdminUserPassword.query(12)), { url: "admin/users/12/password-reset", method: "POST" });
  const source = text("admin/components/AdminViews.tsx");
  assert.match(source, /session\.isSuperAdmin && data\.systemRole !== "SUPER_ADMIN"/);
  assert.match(source, /temporaryPassword/);
  assert.match(source, /resetState\.reset\(\)/);
  assert.match(source, /사용자 비밀번호를 초기화하시겠습니까/);
  assert.doesNotMatch(source, /localStorage|console\.log/);
});

test("forced change uses the actual account password endpoint and clears auth state", () => {
  assert.ok(fs.existsSync(path.join(root, "app/[locale]/money/change-required-password/page.tsx")));
  assert.match(text("auth/components/RequiredPasswordChangeForm.tsx"), /currentPassword/);
  assert.match(text("account/hooks/useUpdateAccountPassword.ts"), /clearLocalSession/);
  assert.match(text("auth/hooks/useAuthGuard.ts"), /passwordChangeRequired/);
  assert.match(text("admin/hooks/useAdminSession.ts"), /change-required-password/);
});

test("backend response contract exposes the forced-change flag in login and current user DTOs", () => {
  assert.match(text("auth/dto/res/LoginResponse.ts"), /passwordChangeRequired:\s*boolean/);
  assert.match(text("auth/dto/res/CurrentUserResponse.ts"), /passwordChangeRequired:\s*boolean/);
});

test("recovery, verification, answers, and temporary credentials are not placed in browser storage or URLs", () => {
  for (const file of ["auth/components/SignupForm.tsx", "auth/components/ForgotPasswordForm.tsx", "account/components/AccountSecuritySection.tsx", "admin/components/AdminViews.tsx"]) {
    const source = text(file);
    assert.doesNotMatch(source, /localStorage|sessionStorage|console\.log/);
    assert.doesNotMatch(source, /searchParams\.set\(.*(?:answer|recoveryCode|verificationToken|temporaryPassword)/);
  }
});

test("the backend-enforced password-change exception includes the supported account password route", () => {
  const source = fs.readFileSync(path.join(root, "../../backend/src/main/java/com/moneybook/backend/accountmanagement/PasswordChangeRequiredInterceptor.java"), "utf8");
  assert.match(source, /"PATCH"\.equals\(method\).*"\/account\/password"\.equals\(path\)/s);
  assert.match(source, /PASSWORD_CHANGE_REQUIRED/);
});
