import type { TransactionResponse } from "./TransactionResponse";
export interface TransactionSearchResponse {
  content: TransactionResponse[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean;
}
