import type { MoneyBookActivityResponse } from "./MoneyBookActivityResponse";
export interface MoneyBookActivityPageResponse {
  content: MoneyBookActivityResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
