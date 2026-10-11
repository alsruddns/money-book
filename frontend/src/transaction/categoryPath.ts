import type { TransactionResponse } from "./dto/res/TransactionResponse";

export function formatTransactionCategoryPath(transaction: Pick<TransactionResponse, "categoryName" | "categoryPath">) {
  const names = [transaction.categoryPath?.largeCategory?.name,
    transaction.categoryPath?.mediumCategory?.name, transaction.categoryPath?.smallCategory?.name].filter(
    (name): name is string => Boolean(name),
  );
  return names.length ? names.join(" > ") : transaction.categoryName;
}
