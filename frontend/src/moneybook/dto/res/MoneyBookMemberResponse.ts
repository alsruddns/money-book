export interface MoneyBookMemberResponse {
  moneyBookUserUid: number;
  moneyBookUid: number;
  userUid: number;
  nickname: string;
  isOwner: boolean;
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
