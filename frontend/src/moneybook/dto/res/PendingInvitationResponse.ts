import type { InvitationStatus } from "./InvitationResponse";

export interface PendingInvitationResponse {
  moneyBookUserUid: number;
  moneyBookUid: number;
  moneyBookName: string;
  ownerUserUid: number;
  invitationStatus: InvitationStatus;
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
