export type InvitationStatus = "PENDING" | "ACCEPTED" | "REJECTED";

export interface InvitationResponse {
  moneyBookUserUid: number;
  moneyBookUid: number;
  userUid: number;
  invitationStatus: InvitationStatus;
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
