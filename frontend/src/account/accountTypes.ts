import type { AccountType } from "./dto/AccountType";

export const accountTypeLabels: Record<AccountType, string> = {
  CASH: "현금",
  BANK: "은행",
  CARD: "카드",
  ETC: "기타",
};

export const accountTypes: AccountType[] = ["CASH", "BANK", "CARD", "ETC"];
