export interface MoneyBookListResponse {
  moneyBookUid: number;
  name: string;
  ownerUserUid: number;
  isOwner: boolean;
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
