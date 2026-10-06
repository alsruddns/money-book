import type { AccountType } from "../AccountType";

export interface CreateAccountRequest {
  name: string;
  accountType: AccountType;
  sortOrder: number;
}
