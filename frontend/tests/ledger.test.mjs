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
    require: (name) => name in mocks ? mocks[name] : localRequire(name),
    ...globals,
  });
  return compiledModule.exports;
}
const plain = (value) => JSON.parse(JSON.stringify(value));
const builder = { query: (definition) => definition, mutation: (definition) => definition };
const apiMock = { baseApi: { injectEndpoints: ({ endpoints }) => endpoints(builder) } };

test("category API uses exact paths, optional type filter, and book scoped tags", () => {
  const { categoryApi: api } = loadModule("category/controller/categoryApi.ts", { "@/common/api/baseApi": apiMock });
  assert.deepEqual(plain(api.getCategories.query({ moneyBookUid: 4, transactionType: "INCOME" })), {
    url: "money-books/4/categories", params: { transactionType: "INCOME" },
  });
  assert.equal(api.createCategory.query({ moneyBookUid: 4, request: {} }).method, "POST");
  assert.equal(api.updateCategory.query({ moneyBookUid: 4, categoryUid: 8, request: {} }).url, "money-books/4/categories/8");
  assert.equal(api.deleteCategory.query({ moneyBookUid: 4, categoryUid: 8 }).method, "DELETE");
  assert.deepEqual(plain(api.getCategories.providesTags([], undefined, { moneyBookUid: 4 })), [{ type: "Category", id: 4 }]);
  assert.deepEqual(plain(api.createCategory.invalidatesTags({}, undefined, { moneyBookUid: 4 })), [{ type: "Category", id: 4 }, { type: "Report", id: 4 }, { type: "MoneyBookActivity", id: 4 }]);
  assert.deepEqual(plain(api.updateCategory.invalidatesTags({}, undefined, { moneyBookUid: 4 })), [
    { type: "Category", id: 4 }, { type: "Transaction", id: 4 },
    { type: "Calendar", id: 4 }, { type: "Budget", id: 4 }, { type: "Recurring", id: 4 }, { type: "Report", id: 4 }, { type: "MoneyBookActivity", id: 4 },
  ]);
  assert.deepEqual(plain(api.deleteCategory.invalidatesTags(undefined, { status: 409 }, { moneyBookUid: 4 })), []);
});

test("account API uses exact paths and updates transaction names only after successful edits", () => {
  const { accountApi: api } = loadModule("account/controller/accountApi.ts", { "@/common/api/baseApi": apiMock });
  assert.equal(api.getAccounts.query(5), "money-books/5/accounts");
  assert.equal(api.createAccount.query({ moneyBookUid: 5, request: {} }).method, "POST");
  assert.equal(api.updateAccount.query({ moneyBookUid: 5, accountUid: 6, request: {} }).url, "money-books/5/accounts/6");
  assert.equal(api.deleteAccount.query({ moneyBookUid: 5, accountUid: 6 }).method, "DELETE");
  assert.deepEqual(plain(api.createAccount.invalidatesTags({}, undefined, { moneyBookUid: 5 })), [{ type: "Account", id: 5 }, { type: "Report", id: 5 }, { type: "MoneyBookActivity", id: 5 }]);
  assert.deepEqual(plain(api.getAccounts.providesTags([], undefined, 5)), [{ type: "Account", id: 5 }]);
  assert.deepEqual(plain(api.updateAccount.invalidatesTags({}, undefined, { moneyBookUid: 5 })), [
    { type: "Account", id: 5 }, { type: "Transaction", id: 5 }, { type: "Calendar", id: 5 },
    { type: "Transfer", id: 5 }, { type: "Recurring", id: 5 }, { type: "Report", id: 5 }, { type: "MoneyBookActivity", id: 5 },
  ]);
  assert.deepEqual(plain(api.deleteAccount.invalidatesTags(undefined, { status: 409 }, { moneyBookUid: 5 })), []);
  assert.deepEqual(plain(api.deleteAccount.invalidatesTags(undefined, undefined, { moneyBookUid: 5 })), [{ type: "Account", id: 5 }, { type: "Report", id: 5 }, { type: "MoneyBookActivity", id: 5 }]);
});

test("transaction API passes year/month, detail UID, methods, and scoped tags", () => {
  const { transactionApi: api } = loadModule("transaction/controller/transactionApi.ts", { "@/common/api/baseApi": apiMock });
  assert.deepEqual(plain(api.getMonthlyTransactions.query({ moneyBookUid: 7, year: 2026, month: 10 })), {
    url: "money-books/7/transactions", params: { year: 2026, month: 10 },
  });
  assert.equal(api.getTransaction.query({ moneyBookUid: 7, transactionUid: 9 }), "money-books/7/transactions/9");
  assert.equal(api.createTransaction.query({ moneyBookUid: 7, request: {} }).method, "POST");
  assert.equal(api.updateTransaction.query({ moneyBookUid: 7, transactionUid: 9, request: {} }).method, "PATCH");
  assert.equal(api.deleteTransaction.query({ moneyBookUid: 7, transactionUid: 9 }).method, "DELETE");
  assert.deepEqual(plain(api.getMonthlyTransactions.providesTags([], undefined, { moneyBookUid: 7 })), [{ type: "Transaction", id: 7 }]);
  const dependentTags = [{ type: "Transaction", id: 7 }, { type: "Calendar", id: 7 }, { type: "Budget", id: 7 }, { type: "Report", id: 7 }, { type: "MoneyBookActivity", id: 7 }];
  assert.deepEqual(plain(api.createTransaction.invalidatesTags({}, undefined, { moneyBookUid: 7 })), dependentTags);
  assert.deepEqual(plain(api.updateTransaction.invalidatesTags({}, undefined, { moneyBookUid: 7 })), dependentTags);
  assert.deepEqual(plain(api.deleteTransaction.invalidatesTags(undefined, { status: 403 }, { moneyBookUid: 7 })), []);
});

test("month URL parsing rejects invalid values and navigation crosses year boundaries", () => {
  const { parseSelectedMonth, shiftMonth } = loadModule("transaction/month.ts");
  const today = new Date(2026, 9, 2);
  assert.deepEqual(plain(parseSelectedMonth("2026", "10", today)), { year: 2026, month: 10 });
  for (const [year, month] of [["0", "10"], ["2026", "13"], ["NaN", "4"], ["2026", null]]) {
    assert.deepEqual(plain(parseSelectedMonth(year, month, today)), { year: 2026, month: 10 });
  }
  assert.deepEqual(plain(shiftMonth({ year: 2026, month: 12 }, 1)), { year: 2027, month: 1 });
  assert.deepEqual(plain(shiftMonth({ year: 2026, month: 1 }, -1)), { year: 2025, month: 12 });
  assert.deepEqual(plain(shiftMonth({ year: 1, month: 1 }, -1)), { year: 1, month: 1 });
});

test("transaction form accepts backend fields and rejects bad amount, date, and missing choices", () => {
  const { parseTransactionRequest, todayLocalDate, formatWon } = loadModule("transaction/transactionForm.ts");
  const values = { transactionType: "EXPENSE", amount: "15000.50", transactionDate: "2026-10-02", categoryUid: "2", accountUid: "3", memo: " 점심 " };
  assert.deepEqual(plain(parseTransactionRequest(values).request), {
    transactionType: "EXPENSE", amount: 15000.5, transactionDate: "2026-10-02",
    categoryUid: 2, accountUid: 3, memo: "점심",
  });
  for (const amount of ["0", "-1", "NaN", "1.234", "999999999999999999999"]) {
    assert.equal(parseTransactionRequest({ ...values, amount }).request, null);
  }
  assert.equal(parseTransactionRequest({ ...values, transactionDate: "2026-02-30" }).request, null);
  assert.equal(parseTransactionRequest({ ...values, categoryUid: "" }).request, null);
  assert.equal(parseTransactionRequest({ ...values, accountUid: "" }).request, null);
  assert.equal(todayLocalDate(new Date(2026, 9, 2)), "2026-10-02");
  assert.equal(formatWon(15000), "15,000원");
});

test("form choices keep only matching transaction categories and all accounts", () => {
  const { useTransactionFormOptions } = loadModule("transaction/hooks/useTransactionFormOptions.ts", {
    "@/category/hooks/useCategoryList": { useCategoryList: () => ({
      categories: [{ categoryUid: 1, transactionType: "INCOME" }, { categoryUid: 2, transactionType: "EXPENSE" }],
      isLoading: false, isFetching: false, isError: false,
    }) },
    "@/account/hooks/useAccountList": { useAccountList: () => ({ accounts: [{ accountUid: 3 }], isLoading: false, isError: false }) },
  });
  assert.deepEqual(plain(useTransactionFormOptions(7, "INCOME").categories.map((item) => item.categoryUid)), [1]);
  assert.deepEqual(plain(useTransactionFormOptions(7, "EXPENSE").categories.map((item) => item.categoryUid)), [2]);
  assert.deepEqual(plain(useTransactionFormOptions(7, "EXPENSE").accounts.map((item) => item.accountUid)), [3]);
});

test("category, account, and transaction lists expose empty states and permission controls", () => {
  function categoryMarkup(permission, categories = []) {
    const Component = loadModule("category/components/CategoryList.tsx", {
      react: React,
      "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => permission },
      "../hooks/useCategoryList": { useCategoryList: () => ({ categories, isLoading: false, isError: false }) },
      "../hooks/useDeleteCategory": { useDeleteCategory: () => ({ isLoading: false, errorMessage: null }) },
      "./CategoryFormDialog": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(Component, { moneyBookUid: 7 }));
  }
  const category = { categoryUid: 1, name: "식비", transactionType: "EXPENSE", sortOrder: 0 };
  assert.match(categoryMarkup({}, []), /등록된 카테고리가 없습니다/);
  assert.doesNotMatch(categoryMarkup({}, [category]), /카테고리 추가|>수정<|>삭제</);
  assert.match(categoryMarkup({ canCreate: true, canUpdate: true, canDelete: true }, [category]), /카테고리 추가/);
  assert.match(categoryMarkup({ canUpdate: true }, [category]), />수정</);
  assert.match(categoryMarkup({ canDelete: true }, [category]), />삭제</);

  function accountMarkup(permission, accounts = []) {
    const Component = loadModule("account/components/AccountList.tsx", {
      react: React,
      "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => permission },
      "../accountTypes": { accountTypeLabels: { CASH: "현금" } },
      "../hooks/useAccountList": { useAccountList: () => ({ accounts, isLoading: false, isError: false }) },
      "../hooks/useDeleteAccount": { useDeleteAccount: () => ({ isLoading: false, errorMessage: null }) },
      "./AccountFormDialog": { default: () => null },
    }).default;
    return renderToStaticMarkup(React.createElement(Component, { moneyBookUid: 7 }));
  }
  const account = { accountUid: 1, name: "현금", accountType: "CASH", sortOrder: 0 };
  assert.match(accountMarkup({}, []), /등록된 계좌\/결제수단이 없습니다/);
  assert.doesNotMatch(accountMarkup({}, [account]), /계좌 추가|>수정<|>삭제</);
  assert.match(accountMarkup({ canCreate: true, canUpdate: true, canDelete: true }, [account]), /계좌 추가/);

  const TransactionList = loadModule("transaction/components/TransactionList.tsx", {
    react: React,
    "next/navigation": { useSearchParams: () => new URLSearchParams() },
    "@/moneybook/hooks/useMoneyBookPermission": { useMoneyBookPermission: () => ({ canCreate: false, canUpdate: false, canDelete: false }) },
    "../hooks/useTransactionSearch": { useTransactionSearch: () => ({ filters: { startDate: "2026-10-01", endDate: "2026-10-31", transactionType: "", categoryUid: "", accountUid: "", keyword: "", minAmount: "", maxAmount: "", page: 0, size: 20, sort: "DATE_DESC" }, update() {}, showMonthly() {}, result: undefined, isLoading: false, isError: false }) },
    "../hooks/useTransactionSearchOptions": { useTransactionSearchOptions: () => ({ categories: [], accounts: [] }) },
    "../search": { defaultTransactionFilters: () => ({ startDate: "2026-10-01", endDate: "2026-10-31", transactionType: "", categoryUid: "", accountUid: "", keyword: "", minAmount: "", maxAmount: "", page: 0, size: 20, sort: "DATE_DESC" }), isValidSearchDateRange: () => true, isValidSearchAmount: () => true },
    "../hooks/useMonthNavigation": { useMonthNavigation: () => ({ year: 2026, month: 10, moveMonth: () => {} }) },
    "../hooks/useMonthlyTransactions": { useMonthlyTransactions: () => ({ transactions: [], isLoading: false, isError: false }) },
    "./MonthSelector": { default: () => null },
    "./TransactionRow": { default: () => null },
    "./TransactionFormDialog": { default: () => null },
    "./TransactionDetailDialog": { default: () => null },
  }).default;
  const markup = renderToStaticMarkup(React.createElement(TransactionList, { moneyBookUid: 7 }));
  assert.match(markup, /이 달에 등록된 거래가 없습니다/);
  assert.doesNotMatch(markup, /거래 추가/);
});

test("in-use delete errors remain visible and do not report success", async () => {
  const state = [];
  const reactMock = { useState: (initial) => [initial, (value) => state.push(value)] };
  const getApiErrorMessage = () => "사용 중인 항목은 삭제할 수 없습니다.";
  const failure = () => ({ unwrap: async () => { throw { status: 409 }; } });
  for (const [file, exportName, mutationName, actionName] of [
    ["category/hooks/useDeleteCategory.ts", "useDeleteCategory", "useDeleteCategoryMutation", "deleteCategory"],
    ["account/hooks/useDeleteAccount.ts", "useDeleteAccount", "useDeleteAccountMutation", "deleteAccount"],
  ]) {
    const hook = loadModule(file, {
      react: reactMock,
      "@/common/api/getApiErrorMessage": { getApiErrorMessage },
      [file.startsWith("category") ? "../controller/categoryApi" : "../controller/accountApi"]: {
        [mutationName]: () => [failure, { isLoading: false }],
      },
    }, { window: { confirm: () => true } })[exportName](7);
    assert.equal(await hook[actionName](1, "항목"), false);
    assert.equal(state.at(-1), "사용 중인 항목은 삭제할 수 없습니다.");
  }
});

test("category and account hooks send trimmed create/update requests", async () => {
  const calls = [];
  const reactMock = { useState: (initial) => [initial, () => {}] };
  const trigger = (argument) => ({ unwrap: async () => { calls.push(argument); } });
  const validation = { validateNamedItem: () => null };
  const error = { getApiErrorMessage: () => "오류" };
  for (const [domain, name, key] of [
    ["category", "Category", "categoryUid"], ["account", "Account", "accountUid"],
  ]) {
    for (const action of ["Create", "Update"]) {
      const hook = loadModule(`${domain}/hooks/use${action}${name}.ts`, {
        react: reactMock,
        "@/common/api/getApiErrorMessage": error,
        "@/common/validation/validateNamedItem": validation,
        [`../controller/${domain}Api`]: { [`use${action}${name}Mutation`]: () => [trigger, { isLoading: false }] },
      })[`use${action}${name}`](7, 3);
      const request = domain === "category"
        ? { name: "  식비  ", sortOrder: 0, ...(action === "Create" ? { transactionType: "EXPENSE" } : {}) }
        : { name: "  현금  ", sortOrder: 0, accountType: "CASH" };
      assert.equal(await hook[`${action.toLowerCase()}${name}`](request), true);
      assert.deepEqual(plain(calls.shift()), {
        moneyBookUid: 7, ...(action === "Update" ? { [key]: 3 } : {}),
        request: { ...request, name: request.name.trim() },
      });
    }
  }
});

test("transaction create/update hooks use validated DTO and delete requires confirmation", async () => {
  const calls = [];
  const state = [];
  const reactMock = { useState: (initial) => [initial, (value) => state.push(value)] };
  const trigger = (argument) => ({ unwrap: async () => { calls.push(argument); } });
  const error = { getApiErrorMessage: () => "거래 오류" };
  const form = loadModule("transaction/transactionForm.ts");
  const values = { transactionType: "INCOME", amount: "1000", transactionDate: "2026-10-02", categoryUid: "2", accountUid: "3", memo: "급여" };
  for (const [action, uid] of [["Create", null], ["Update", 8]]) {
    const hook = loadModule(`transaction/hooks/use${action}Transaction.ts`, {
      react: reactMock,
      "@/common/api/getApiErrorMessage": error,
      "../transactionForm": form,
      "../controller/transactionApi": { [`use${action}TransactionMutation`]: () => [trigger, { isLoading: false }] },
    })[`use${action}Transaction`](7, uid);
    assert.equal(await hook[`${action.toLowerCase()}Transaction`](values), true);
    assert.deepEqual(plain(calls.shift()), plain({
      moneyBookUid: 7, ...(uid ? { transactionUid: uid } : {}), request: form.parseTransactionRequest(values).request,
    }));
    assert.equal(await hook[`${action.toLowerCase()}Transaction`]({ ...values, amount: "0" }), false);
    assert.equal(calls.length, 0);
    assert.match(state.at(-1), /금액/);
  }
  const deleteMock = {
    react: reactMock,
    "@/common/api/getApiErrorMessage": error,
    "../controller/transactionApi": { useDeleteTransactionMutation: () => [trigger, { isLoading: false }] },
  };
  const denied = loadModule("transaction/hooks/useDeleteTransaction.ts", deleteMock,
    { window: { confirm: () => false } }).useDeleteTransaction(7);
  assert.equal(await denied.deleteTransaction(8), false);
  assert.equal(calls.length, 0);
  const confirmed = loadModule("transaction/hooks/useDeleteTransaction.ts", deleteMock,
    { window: { confirm: () => true } }).useDeleteTransaction(7);
  assert.equal(await confirmed.deleteTransaction(8), true);
  assert.deepEqual(plain(calls.shift()), { moneyBookUid: 7, transactionUid: 8 });
});

test("book permissions come from the existing query result and owner keeps full rights", () => {
  const { useMoneyBookPermission } = loadModule("moneybook/hooks/useMoneyBookPermission.ts", {
    "./useMoneyBookDetail": { useMoneyBookDetail: () => ({
      moneyBook: { isOwner: true, canCreate: false, canRead: false, canUpdate: false, canDelete: false },
      isLoading: false,
    }) },
  });
  const owner = useMoneyBookPermission(7);
  assert.equal(owner.isOwner, true);
  assert.equal(owner.canCreate && owner.canRead && owner.canUpdate && owner.canDelete, true);
});
