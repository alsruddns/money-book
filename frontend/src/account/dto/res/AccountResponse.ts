import type { AccountType } from "../AccountType";

export interface AccountResponse {
  accountUid: number;
  moneyBookUid: number;
  name: string;
  accountType: AccountType;
  sortOrder: number;
}
