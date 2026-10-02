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
const sourceRoot = path.join(path.dirname(fileURLToPath(import.meta.url)), "../src");
function loadModule(relativePath, mocks = {}, globals = {}) {
  const source = fs.readFileSync(path.join(sourceRoot, relativePath), "utf8");
  const compiled = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, jsx: ts.JsxEmit.ReactJSX },
  }).outputText;
  const compiledModule = { exports: {} };
  vm.runInNewContext(compiled, {
    module: compiledModule, exports: compiledModule.exports,
    require: (name) => name in mocks ? mocks[name] : localRequire(name), ...globals,
  });
  return compiledModule.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const rule = { recurringTransactionUid: 5, moneyBookUid: 7, transactionType: "EXPENSE", amount: 50000,
  categoryUid: 3, categoryName: "식비", accountUid: 2, accountName: "현금", frequency: "MONTHLY",
  dayOfMonth: 31, dayOfWeek: null, startDate: "2026-10-01", endDate: null,
  memo: "정기 장보기", isActive: true, lastGeneratedDate: "2026-10-31" };
const formValues = { transactionType: "EXPENSE", amount: "50,000", categoryUid: "3", accountUid: "2",
  frequency: "MONTHLY", dayOfMonth: "31", dayOfWeek: "", startDate: "2026-10-01", endDate: "", memo: " 정기 장보기 " };

test("recurring API matches backend routes, methods, active payload, and generate invalidation", () => {
  const builder = { query: (definition) => definition, mutation: (definition) => definition };
  const api = loadModule("recurring/controller/recurringTransactionApi.ts", {
    "@/common/api/baseApi": { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } },
  }).recurringTransactionApi;
  const key = { moneyBookUid: 7, recurringTransactionUid: 5 };
  assert.equal(api.getRecurringTransactions.query(7), "money-books/7/recurring-transactions");
  assert.equal(api.createRecurringTransaction.query({ moneyBookUid: 7, request: {} }).method, "POST");
  assert.equal(api.updateRecurringTransaction.query({ ...key, request: {} }).url, "money-books/7/recurring-transactions/5");
  assert.deepEqual(plain(api.changeRecurringTransactionActive.query({ ...key, request: { active: false } })), {
    url: "money-books/7/recurring-transactions/5/active", method: "PATCH", body: { active: false },
  });
  assert.equal(api.deleteRecurringTransaction.query(key).method, "DELETE");
  assert.deepEqual(plain(api.generateRecurringTransactions.query({ moneyBookUid: 7, request: { baseDate: "2026-10-02" } })), {
    url: "money-books/7/recurring-transactions/generate", method: "POST", body: { baseDate: "2026-10-02" },
  });
  assert.deepEqual(plain(api.createRecurringTransaction.invalidatesTags({}, undefined, { moneyBookUid: 7 })), [{ type: "Recurring", id: 7 }]);
  assert.deepEqual(plain(api.generateRecurringTransactions.invalidatesTags({}, undefined, { moneyBookUid: 7 })), [
    { type: "Recurring", id: 7 }, { type: "Transaction", id: 7 }, { type: "Calendar", id: 7 }, { type: "Budget", id: 7 },
  ]);
  assert.deepEqual(plain(api.generateRecurringTransactions.invalidatesTags(undefined, { status: 409 }, { moneyBookUid: 7 })), []);
});

test("recurring parser sends MONTHLY and ISO WEEKLY fields with exact backend names", () => {
  const { parseRecurringRequest } = loadModule("recurring/recurringForm.ts", {
    "@/common/validation/positiveAmount": loadModule("common/validation/positiveAmount.ts"),
    "@/transaction/transactionForm": loadModule("transaction/transactionForm.ts"),
  });
  assert.deepEqual(plain(parseRecurringRequest(formValues).request), {
    transactionType: "EXPENSE", amount: 50000, categoryUid: 3, accountUid: 2,
    frequency: "MONTHLY", dayOfMonth: 31, dayOfWeek: null, startDate: "2026-10-01",
    endDate: null, memo: "정기 장보기",
  });
  assert.deepEqual(plain(parseRecurringRequest({ ...formValues, frequency: "WEEKLY", dayOfMonth: "31", dayOfWeek: "7" }).request), {
    transactionType: "EXPENSE", amount: 50000, categoryUid: 3, accountUid: 2,
    frequency: "WEEKLY", dayOfMonth: null, dayOfWeek: 7, startDate: "2026-10-01",
    endDate: null, memo: "정기 장보기",
  });
  for (const invalid of ["0", "32", "abc"]) assert.match(parseRecurringRequest({ ...formValues, dayOfMonth: invalid }).error, /1~31/);
  assert.match(parseRecurringRequest({ ...formValues, frequency: "WEEKLY", dayOfWeek: "8" }).error, /요일/);
  assert.match(parseRecurringRequest({ ...formValues, endDate: "2026-09-30" }).error, /종료 날짜/);
  assert.match(parseRecurringRequest({ ...formValues, amount: "0" }).error, /금액/);
});

test("weekday mapping follows Monday 1 through Sunday 7 and shows schedule", () => {
  const { recurringWeekdays, recurringSchedule } = loadModule("recurring/dto/RecurringFrequency.ts");
  assert.deepEqual(plain(recurringWeekdays.map((day) => day.value)), [1, 2, 3, 4, 5, 6, 7]);
  assert.equal(recurringSchedule("MONTHLY", 31, null), "매월 31일");
  assert.equal(recurringSchedule("WEEKLY", null, 7), "매주 일요일");
});

test("recurring form shows monthly month-end guidance and weekly weekday choices", () => {
  const types = [];
  const Form = loadModule("recurring/components/RecurringTransactionFormDialog.tsx", {
    "next/link": { default: ({ href, children }) => React.createElement("a", { href }, children) },
    "@/common/components/DialogShell": { default: ({ children }) => React.createElement("section", null, children) },
    "@/transaction/transactionForm": { todayLocalDate: () => "2026-10-02" },
    "@/transaction/hooks/useTransactionFormOptions": { useTransactionFormOptions: (_uid, type) => {
      types.push(type);
      return { categories: [{ categoryUid: 3, name: "식비" }], accounts: [{ accountUid: 2, name: "현금" }], isLoading: false, isError: false };
    } },
    "../dto/RecurringFrequency": loadModule("recurring/dto/RecurringFrequency.ts"),
    "../hooks/useCreateRecurringTransaction": { useCreateRecurringTransaction: () => ({ create: async () => true, isLoading: false }) },
    "../hooks/useUpdateRecurringTransaction": { useUpdateRecurringTransaction: () => ({ update: async () => true, isLoading: false }) },
  }).default;
  const render = (initial) => renderToStaticMarkup(React.createElement(Form, {
    moneyBookUid: 7, initial, onClose: () => {}, onSaved: () => {},
  }));
  assert.match(render(undefined), /해당 날짜가 없는 달에는 그 달의 마지막 날/);
  const weekly = render({ ...rule, transactionType: "INCOME", frequency: "WEEKLY", dayOfMonth: null, dayOfWeek: 7 });
  assert.match(weekly, /일요일/);
  assert.doesNotMatch(weekly, /매월 반복일/);
  assert.deepEqual(types, ["EXPENSE", "INCOME"]);
});

test("recurring card shows type, schedule, status, dates and permission controls", () => {
  const Card = loadModule("recurring/components/RecurringTransactionCard.tsx", {
    "@/common/format/money": { formatMoney: (value) => `${Number(value).toLocaleString("ko-KR")}원` },
    "../dto/RecurringFrequency": loadModule("recurring/dto/RecurringFrequency.ts"),
  }).default;
  function render(value, canUpdate, canDelete) {
    return renderToStaticMarkup(React.createElement(Card, { rule: value, canUpdate, canDelete, busy: false,
      onEdit: () => {}, onToggle: () => {}, onDelete: () => {} }));
  }
  const markup = render(rule, true, true);
  for (const text of ["지출", "매월 31일", "50,000원", "식비", "현금", "활성", "2026-10-31", "정기 장보기"]) assert.match(markup, new RegExp(text));
  assert.match(render({ ...rule, transactionType: "INCOME", frequency: "WEEKLY", dayOfMonth: null, dayOfWeek: 1, isActive: false }, false, false), /수입 · 매주 월요일/);
  assert.doesNotMatch(render(rule, false, false), /수정<\/button>|삭제<\/button>|비활성화<\/button>/);
  assert.match(markup, /수정<\/button>/);
  assert.match(markup, /비활성화<\/button>/);
  assert.match(markup, /삭제<\/button>/);
});

test("recurring view exposes empty, loading, error, and C-only generate controls", () => {
  function render(canCreate, state) {
    const View = loadModule("recurring/components/RecurringTransactionView.tsx", {
      "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ canRead: true, canCreate, canUpdate: false, canDelete: false }) },
      "@/transaction/transactionForm": { todayLocalDate: () => "2026-10-02" },
      "../hooks/useRecurringTransactions": { useRecurringTransactions: () => state },
      "../hooks/useToggleRecurringTransaction": { useToggleRecurringTransaction: () => ({ isLoading: false }) },
      "../hooks/useDeleteRecurringTransaction": { useDeleteRecurringTransaction: () => ({ isLoading: false }) },
      "../hooks/useGenerateRecurringTransactions": { useGenerateRecurringTransactions: () => ({ isLoading: false, generate: async () => true }) },
      "./RecurringTransactionCard": { default: () => null }, "./RecurringTransactionFormDialog": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(View, { moneyBookUid: 7 }));
  }
  const empty = { rules: [], isLoading: false, isError: false };
  assert.match(render(false, empty), /등록된 정기 수입\/지출이 없습니다/);
  assert.doesNotMatch(render(false, empty), /기준일까지 정기 거래 반영/);
  assert.match(render(true, empty), /기준일까지 정기 거래 반영/);
  assert.match(render(true, { ...empty, isLoading: true }), /불러오는 중/);
  assert.match(render(true, { ...empty, isError: true, errorMessage: "조회 오류" }), /조회 오류/);
});

test("generate reports positive and zero counts without blocking repeated requests", async () => {
  const calls = [];
  const messages = [];
  const { useGenerateRecurringTransactions } = loadModule("recurring/hooks/useGenerateRecurringTransactions.ts", {
    react: { useState: (initial) => [initial, (value) => messages.push(value)] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "@/transaction/transactionForm": { isLocalDate: () => true },
    "../controller/recurringTransactionApi": { useGenerateRecurringTransactionsMutation: () => [
      (value) => ({ unwrap: async () => { calls.push(value); return { baseDate: value.request.baseDate, generatedCount: calls.length === 1 ? 3 : 0 }; } }),
      { isLoading: false },
    ] },
  });
  const generation = useGenerateRecurringTransactions(7);
  assert.equal(await generation.generate("2026-10-02"), true);
  assert.equal(await generation.generate("2026-10-02"), true);
  assert.equal(calls.length, 2);
  assert.deepEqual(plain(calls[0]), { moneyBookUid: 7, request: { baseDate: "2026-10-02" } });
  assert.ok(messages.includes("정기 거래 3건이 반영되었습니다."));
  assert.ok(messages.includes("새로 생성할 거래가 없습니다."));
});

test("active toggle sends active field and rule deletion explains existing transactions", async () => {
  const calls = [];
  const reactMock = { useState: (initial) => [initial, () => {}] };
  const { useToggleRecurringTransaction } = loadModule("recurring/hooks/useToggleRecurringTransaction.ts", {
    react: reactMock, "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/recurringTransactionApi": { useChangeRecurringTransactionActiveMutation: () => [
      (value) => ({ unwrap: async () => { calls.push(value); } }), { isLoading: false },
    ] },
  });
  assert.equal(await useToggleRecurringTransaction(7).toggle(5, false), true);
  assert.deepEqual(plain(calls), [{ moneyBookUid: 7, recurringTransactionUid: 5, request: { active: false } }]);
  let prompt = "";
  const { useDeleteRecurringTransaction } = loadModule("recurring/hooks/useDeleteRecurringTransaction.ts", {
    react: reactMock, "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/recurringTransactionApi": { useDeleteRecurringTransactionMutation: () => [
      () => ({ unwrap: async () => { calls.push("delete"); } }), { isLoading: false },
    ] },
  }, { window: { confirm: (message) => { prompt = message; return false; } } });
  assert.equal(await useDeleteRecurringTransaction(7).remove(5), false);
  assert.match(prompt, /이미 생성된 거래는 삭제되지 않습니다/);
  assert.equal(calls.length, 1);
});
