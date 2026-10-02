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
const transfer = { transferUid: 9, moneyBookUid: 7, fromAccountUid: 1, fromAccountName: "신한은행",
  toAccountUid: 2, toAccountName: "생활비 통장", amount: 50000, transferDate: "2026-10-02", memo: "생활비 이동" };

test("transfer API uses backend routes and invalidates Transfer and Calendar only on success", () => {
  const builder = { query: (definition) => definition, mutation: (definition) => definition };
  const api = loadModule("transfer/controller/transferApi.ts", {
    "@/common/api/baseApi": { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } },
  }).transferApi;
  assert.deepEqual(plain(api.getMonthlyTransfers.query({ moneyBookUid: 7, year: 2026, month: 10 })), {
    url: "money-books/7/transfers", params: { year: 2026, month: 10 },
  });
  assert.equal(api.getTransfer.query({ moneyBookUid: 7, transferUid: 9 }), "money-books/7/transfers/9");
  assert.equal(api.createTransfer.query({ moneyBookUid: 7, request: {} }).method, "POST");
  assert.equal(api.updateTransfer.query({ moneyBookUid: 7, transferUid: 9, request: {} }).method, "PATCH");
  assert.equal(api.deleteTransfer.query({ moneyBookUid: 7, transferUid: 9 }).method, "DELETE");
  const tags = [{ type: "Transfer", id: 7 }, { type: "Calendar", id: 7 }];
  for (const name of ["createTransfer", "updateTransfer", "deleteTransfer"]) {
    assert.deepEqual(plain(api[name].invalidatesTags({}, undefined, { moneyBookUid: 7 })), tags);
    assert.deepEqual(plain(api[name].invalidatesTags(undefined, { status: 403 }, { moneyBookUid: 7 })), []);
  }
});

test("transfer form produces exact payload and rejects same account, zero, negative, and bad date", () => {
  const { parseTransferRequest } = loadModule("transfer/transferForm.ts", {
    "@/common/validation/positiveAmount": loadModule("common/validation/positiveAmount.ts"),
    "@/transaction/transactionForm": loadModule("transaction/transactionForm.ts"),
  });
  const values = { fromAccountUid: "1", toAccountUid: "2", amount: "50,000", transferDate: "2026-10-02", memo: " 생활비 이동 " };
  assert.deepEqual(plain(parseTransferRequest(values).request), {
    fromAccountUid: 1, toAccountUid: 2, amount: 50000, transferDate: "2026-10-02", memo: "생활비 이동",
  });
  assert.match(parseTransferRequest({ ...values, toAccountUid: "1" }).error, /다르게/);
  assert.match(parseTransferRequest({ ...values, amount: "0" }).error, /0보다/);
  assert.match(parseTransferRequest({ ...values, amount: "-1" }).error, /0보다/);
  assert.match(parseTransferRequest({ ...values, transferDate: "2026-02-30" }).error, /날짜/);
  assert.match(parseTransferRequest({ ...values, memo: "x".repeat(501) }).error, /500자/);
});

test("transfer list shows accounts, amount, empty state, and C permission", () => {
  function render(transfers, canCreate) {
    const View = loadModule("transfer/components/TransferView.tsx", {
      "@/common/format/money": { formatMoney: (value) => `${Number(value).toLocaleString("ko-KR")}원` },
      "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ canRead: true, canCreate, canUpdate: false, canDelete: false }) },
      "@/transaction/hooks/useMonthNavigation": { useMonthNavigation: () => ({ year: 2026, month: 10, moveMonth: () => {}, goToToday: () => {} }) },
      "@/transaction/components/MonthSelector": { default: () => null },
      "../hooks/useMonthlyTransfers": { useMonthlyTransfers: () => ({ transfers, isLoading: false, isError: false }) },
      "./TransferFormDialog": { default: () => null }, "./TransferDetailDialog": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(View, { moneyBookUid: 7 }));
  }
  assert.match(render([], false), /이 달에 등록된 이체가 없습니다/);
  assert.doesNotMatch(render([], false), /이체 추가/);
  assert.match(render([], true), /이체 추가/);
  const markup = render([transfer], true);
  assert.match(markup, /신한은행/);
  assert.match(markup, /생활비 통장/);
  assert.match(markup, /50,000원/);
  assert.match(markup, /생활비 이동/);
});

test("transfer form explains the two-account requirement", () => {
  const Form = loadModule("transfer/components/TransferFormDialog.tsx", {
    "next/link": { default: ({ href, children }) => React.createElement("a", { href }, children) },
    "@/common/components/DialogShell": { default: ({ children }) => React.createElement("section", null, children) },
    "@/account/hooks/useAccountList": { useAccountList: () => ({ accounts: [{ accountUid: 1, name: "현금" }], isLoading: false, isError: false }) },
    "@/transaction/transactionForm": { todayLocalDate: () => "2026-10-02" },
    "../hooks/useCreateTransfer": { useCreateTransfer: () => ({ create: async () => true, isLoading: false }) },
    "../hooks/useUpdateTransfer": { useUpdateTransfer: () => ({ update: async () => true, isLoading: false }) },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(Form, { moneyBookUid: 7, onClose: () => {}, onSaved: () => {} }));
  assert.match(markup, /2개 이상 필요합니다/);
  assert.match(markup, /href="\/books\/7\/accounts"/);
  assert.match(markup, /disabled=""/);
});

test("transfer detail hides U and D controls and delete requires confirmation", async () => {
  function render(canUpdate, canDelete) {
    const Detail = loadModule("transfer/components/TransferDetailDialog.tsx", {
      "@/common/components/DialogShell": { default: ({ children }) => React.createElement("section", null, children) },
      "@/common/format/money": { formatMoney: (value) => `${value}원` },
      "../hooks/useTransferDetail": { useTransferDetail: () => ({ transfer }) },
      "../hooks/useDeleteTransfer": { useDeleteTransfer: () => ({ remove: async () => true, isLoading: false }) },
      "./TransferFormDialog": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(Detail, { moneyBookUid: 7, transferUid: 9, canUpdate, canDelete, onClose: () => {} }));
  }
  assert.doesNotMatch(render(false, false), /수정<\/button>|삭제<\/button>/);
  assert.match(render(true, false), /수정<\/button>/);
  assert.match(render(false, true), /삭제<\/button>/);
  let calls = 0;
  const { useDeleteTransfer } = loadModule("transfer/hooks/useDeleteTransfer.ts", {
    react: { useState: (initial) => [initial, () => {}] },
    "@/common/api/getApiErrorMessage": { getApiErrorMessage: () => "오류" },
    "../controller/transferApi": { useDeleteTransferMutation: () => [() => ({ unwrap: async () => { calls++; } }), { isLoading: false }] },
  }, { window: { confirm: () => false } });
  assert.equal(await useDeleteTransfer(7).remove(9), false);
  assert.equal(calls, 0);
});
