"use client";

import { useMoneyBookDetail } from "./useMoneyBookDetail";

export function useMoneyBookPermission(moneyBookUid: number) {
  const detail = useMoneyBookDetail(moneyBookUid);
  const book = detail.moneyBook;
  return {
    ...detail,
    canCreate: Boolean(book?.isOwner || book?.canCreate),
    canRead: Boolean(book?.isOwner || book?.canRead),
    canUpdate: Boolean(book?.isOwner || book?.canUpdate),
    canDelete: Boolean(book?.isOwner || book?.canDelete),
    isOwner: Boolean(book?.isOwner),
    isAdmin: Boolean(book?.isAdmin),
  };
}
