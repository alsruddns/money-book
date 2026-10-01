"use client";

import { useMoneyBookList } from "./useMoneyBookList";

export function useMoneyBookDetail(moneyBookUid: number) {
  const { moneyBooks, isLoading, isError, errorMessage } = useMoneyBookList();
  return {
    moneyBook: moneyBooks.find((book) => book.moneyBookUid === moneyBookUid) ?? null,
    isLoading,
    isError,
    errorMessage,
  };
}
