export interface TransferResponse {
  transferUid: number;
  moneyBookUid: number;
  fromAccountUid: number;
  fromAccountName: string;
  toAccountUid: number;
  toAccountName: string;
  amount: number;
  transferDate: string;
  memo: string | null;
}
