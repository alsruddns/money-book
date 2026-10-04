import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { fileURLToPath } from "node:url";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));

function loadTypeScript(relativePath, globals = {}) {
  const source = fs.readFileSync(path.join(testDirectory, relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, { module: compiledModule, exports: compiledModule.exports, ...globals });
  return compiledModule.exports;
}

test("date formatters keep local dates stable and render API datetimes in Korea", () => {
  const { formatLocalDate, formatKoreaDateTime } = loadTypeScript("../src/common/format/dateTime.ts");
  assert.equal(formatLocalDate("2026-10-03"), "2026.10.03");
  assert.equal(formatLocalDate("2026-10-03", "확인 불가"), "2026.10.03");
  assert.equal(formatLocalDate(null, "확인 불가"), "확인 불가");
  assert.equal(formatKoreaDateTime("2026-10-03T15:30:00"), "2026.10.03 15:30");
  assert.equal(formatKoreaDateTime("2026-10-03T06:30:00Z"), "2026.10.03 15:30");
  assert.equal(formatKoreaDateTime("invalid"), "-");
});

test("API errors use safe status messages and explain 429 retry timing", () => {
  const { getApiErrorMessage } = loadTypeScript("../src/common/api/getApiErrorMessage.ts");
  assert.equal(getApiErrorMessage({ status: 403, data: { message: "internal details" } }, "fallback"), "이 기능을 사용할 권한이 없습니다.");
  assert.equal(getApiErrorMessage({ status: 404, data: { message: "internal details" } }, "fallback"), "요청한 정보를 찾을 수 없습니다.");
  assert.equal(getApiErrorMessage({ status: 429, retryAfterSeconds: 31.2 }, "fallback"), "요청이 많습니다. 약 32초 후 다시 시도해 주세요.");
  assert.equal(getApiErrorMessage({ status: 429 }, "fallback"), "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
  assert.equal(getApiErrorMessage({ status: 503, data: { message: "stack trace" } }, "fallback"), "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
  assert.equal(getApiErrorMessage({ status: 400, data: { message: "입력값을 확인해 주세요." } }, "fallback"), "입력값을 확인해 주세요.");
});

test("common dialogs expose a labelled modal and keep keyboard focus inside", () => {
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/components/DialogShell.tsx"), "utf8");
  assert.match(source, /aria-modal="true"/);
  assert.match(source, /aria-labelledby=\{titleId\}/);
  assert.match(source, /event\.key === "Escape"/);
  assert.match(source, /event\.shiftKey && document\.activeElement === first/);
  assert.match(source, /previouslyFocused\?\.focus\(\)/);
  assert.match(source, /aria-describedby=\{description \? /);
  assert.match(source, /event\.target === event\.currentTarget/);
});

test("body scroll lock keeps stable-gutter viewport width and restores nested locks once", () => {
  const values = new Map([["padding-right", "12px"]]);
  const priorities = new Map([["padding-right", "important"]]);
  const body = {
    style: {
      overflow: "auto",
      getPropertyValue: (name) => values.get(name) ?? "",
      getPropertyPriority: (name) => priorities.get(name) ?? "",
      setProperty: (name, value, priority = "") => { values.set(name, value); priorities.set(name, priority); },
      removeProperty: (name) => { values.delete(name); priorities.delete(name); },
    },
  };
  const document = { body, documentElement: { clientWidth: 1200 } };
  const window = { innerWidth: 1200, CSS: { supports: (query) => query === "scrollbar-gutter: stable" }, getComputedStyle: () => ({ paddingRight: "12px" }) };
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/components/bodyScrollLock.ts"), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, { module: compiledModule, exports: compiledModule.exports, document, window });
  const { acquireBodyScrollLock } = compiledModule.exports;
  const beforeWidth = document.documentElement.clientWidth;
  const releaseFirst = acquireBodyScrollLock();
  const releaseSecond = acquireBodyScrollLock();
  assert.equal(body.style.overflow, "hidden");
  assert.equal(values.get("padding-right"), "12px");
  assert.equal(document.documentElement.clientWidth, beforeWidth);
  releaseFirst();
  assert.equal(body.style.overflow, "hidden");
  releaseSecond();
  assert.equal(body.style.overflow, "auto");
  assert.equal(values.get("padding-right"), "12px");
  assert.equal(priorities.get("padding-right"), "important");
});

test("body scroll lock compensates older browsers and restores the original inline padding", () => {
  const values = new Map([["padding-right", "8px"]]);
  const body = {
    style: {
      overflow: "",
      getPropertyValue: (name) => values.get(name) ?? "",
      getPropertyPriority: () => "",
      setProperty: (name, value) => values.set(name, value),
      removeProperty: (name) => values.delete(name),
    },
  };
  const document = { body, documentElement: { clientWidth: 1185 } };
  const window = { innerWidth: 1200, CSS: { supports: () => false }, getComputedStyle: () => ({ paddingRight: "8px" }) };
  const source = fs.readFileSync(path.join(testDirectory, "../src/common/components/bodyScrollLock.ts"), "utf8");
  const compiled = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 } }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, { module: compiledModule, exports: compiledModule.exports, document, window });
  const release = compiledModule.exports.acquireBodyScrollLock();
  assert.equal(values.get("padding-right"), "23px");
  assert.equal(body.style.overflow, "hidden");
  release();
  assert.equal(values.get("padding-right"), "8px");
  assert.equal(body.style.overflow, "");
});

test("release UX keeps navigation focus, drawer focus, and backup size feedback accessible", () => {
  const header = fs.readFileSync(path.join(testDirectory, "../src/common/components/GlobalHeader.tsx"), "utf8");
  const navigation = fs.readFileSync(path.join(testDirectory, "../src/moneybook/components/MoneyBookNavigation.tsx"), "utf8");
  const backup = fs.readFileSync(path.join(testDirectory, "../src/settings/components/BackupRestoreSection.tsx"), "utf8");
  const signup = fs.readFileSync(path.join(testDirectory, "../src/auth/components/SignupForm.tsx"), "utf8");
  const clipboard = fs.readFileSync(path.join(testDirectory, "../src/common/utils/copyText.ts"), "utf8");
  assert.match(header, /isGlobalNavItemActive\(pathname, item\.href\)/);
  assert.match(navigation, /event\.key !== "Tab"/);
  assert.match(navigation, /trigger\?\.focus\(\)/);
  assert.match(navigation, /aria-modal="true"/);
  assert.match(backup, /fileSizeError &&/);
  assert.match(signup, /copyText\(recoveryCodes\.join/);
  assert.match(signup, /가입하려면 입력한 이메일의 인증을 완료해주세요/);
  assert.match(clipboard, /catch \{/);
});

test("transaction and report screens use shared date/percentage formatting", () => {
  const transactionRow = fs.readFileSync(path.join(testDirectory, "../src/transaction/components/TransactionRow.tsx"), "utf8");
  const transactionDetail = fs.readFileSync(path.join(testDirectory, "../src/transaction/components/TransactionDetailDialog.tsx"), "utf8");
  const ranking = fs.readFileSync(path.join(testDirectory, "../src/report/components/ExpenseRankingView.tsx"), "utf8");
  const report = fs.readFileSync(path.join(testDirectory, "../src/report/components/ReportView.tsx"), "utf8");
  assert.match(transactionRow, /formatLocalDate\(transaction\.transactionDate\)/);
  assert.match(transactionDetail, /formatLocalDate\(transaction\.transactionDate\)/);
  assert.match(ranking, /formatLocalDate\(item\.transactionDate\)/);
  assert.match(report, /formatFractionPercent\(item\.ratio, 1\)/);
});
