export interface CreateTransferRequest {
  fromAccountUid: number;
  toAccountUid: number;
  amount: number;
  transferDate: string;
  memo: string | null;
}
