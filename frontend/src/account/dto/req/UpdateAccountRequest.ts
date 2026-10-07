import type { AccountType } from "../AccountType";

export interface UpdateAccountRequest {
  name: string;
  accountType: AccountType;
  sortOrder: number;
}
