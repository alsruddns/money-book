import type { TransactionSearchQuery } from "./controller/transactionApi";

export type SearchSort = TransactionSearchQuery["sort"];
export interface TransactionFilters {
  startDate: string; endDate: string; transactionType: "" | "INCOME" | "EXPENSE"; categoryUid: string;
  accountUid: string; keyword: string; minAmount: string; maxAmount: string; page: number; size: number; sort: SearchSort;
}

export function localDate(year: number, month: number, day: number): string {
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

export function defaultTransactionFilters(year: number, month: number): TransactionFilters {
  return { startDate: localDate(year, month, 1), endDate: localDate(year, month, new Date(year, month, 0).getDate()),
    transactionType: "", categoryUid: "", accountUid: "", keyword: "", minAmount: "", maxAmount: "", page: 0, size: 20, sort: "DATE_DESC" };
}

function validDate(value: string): boolean {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value);
  if (!match) return false;
  const [, y, m, d] = match;
  const date = new Date(Number(y), Number(m) - 1, Number(d));
  return date.getFullYear() === Number(y) && date.getMonth() === Number(m) - 1 && date.getDate() === Number(d);
}

export function isValidSearchDateRange(startDate: string, endDate: string): boolean {
  if (!validDate(startDate) || !validDate(endDate) || startDate > endDate) return false;
  return (Date.parse(`${endDate}T00:00:00`) - Date.parse(`${startDate}T00:00:00`)) / 86_400_000 <= 731;
}

export function isValidSearchAmount(value: string): boolean {
  return value === "" || (/^\d{1,17}(\.\d{1,2})?$/.test(value) && Number.isFinite(Number(value)));
}

export function parseTransactionFilters(params: URLSearchParams, year: number, month: number): TransactionFilters {
  const fallback = defaultTransactionFilters(year, month);
  const startDate = params.get("startDate") ?? fallback.startDate;
  const endDate = params.get("endDate") ?? fallback.endDate;
  const type = params.get("transactionType") ?? "";
  const sort = params.get("sort") ?? "DATE_DESC";
  const page = Number(params.get("page") ?? 0);
  const size = Number(params.get("size") ?? 20);
  const min = params.get("minAmount") ?? "";
  const max = params.get("maxAmount") ?? "";
  const category = params.get("categoryUid") ?? "";
  const account = params.get("accountUid") ?? "";
  const validPositiveInt = (value: string) => value === "" || (/^\d+$/.test(value) && Number(value) > 0);
  if (!isValidSearchDateRange(startDate, endDate) ||
    (type !== "" && type !== "INCOME" && type !== "EXPENSE") ||
    !["DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"].includes(sort) ||
    !Number.isInteger(page) || page < 0 || ![20, 50, 100].includes(size) ||
    !validPositiveInt(category) || !validPositiveInt(account) || !isValidSearchAmount(min) || !isValidSearchAmount(max) ||
    (min !== "" && max !== "" && Number(min) > Number(max))) return fallback;
  return { startDate, endDate, transactionType: type as TransactionFilters["transactionType"], categoryUid: category,
    accountUid: account, keyword: (params.get("keyword") ?? "").slice(0, 200), minAmount: min, maxAmount: max,
    page, size, sort: sort as SearchSort };
}

export function filtersToSearchQuery(moneyBookUid: number, filters: TransactionFilters): TransactionSearchQuery {
  return { moneyBookUid, startDate: filters.startDate, endDate: filters.endDate,
    ...(filters.transactionType ? { transactionType: filters.transactionType } : {}),
    ...(filters.categoryUid ? { categoryUid: Number(filters.categoryUid) } : {}),
    ...(filters.accountUid ? { accountUid: Number(filters.accountUid) } : {}),
    ...(filters.keyword.trim() ? { keyword: filters.keyword.trim() } : {}),
    ...(filters.minAmount ? { minAmount: Number(filters.minAmount) } : {}),
    ...(filters.maxAmount ? { maxAmount: Number(filters.maxAmount) } : {}), page: filters.page, size: filters.size, sort: filters.sort };
}

export function buildTransactionSearchParams(current: URLSearchParams, filters: TransactionFilters, resetPage = false): URLSearchParams {
  const search = new URLSearchParams(current.toString());
  search.set("search", "1");
  const next = resetPage ? { ...filters, page: 0 } : filters;
  Object.entries(next).forEach(([key, value]) => {
    if (key === "page" && value === 0 || value === "" || value === 20 && key === "size" || value === "DATE_DESC" && key === "sort") search.delete(key);
    else search.set(key, String(value));
  });
  return search;
}
