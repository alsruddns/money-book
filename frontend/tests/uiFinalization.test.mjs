import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";
import ts from "typescript";
import { fileURLToPath } from "node:url";

const testDirectory = path.dirname(fileURLToPath(import.meta.url));

function loadTypeScript(relativePath) {
  const source = fs.readFileSync(path.join(testDirectory, relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020 },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, { module: compiledModule, exports: compiledModule.exports });
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
