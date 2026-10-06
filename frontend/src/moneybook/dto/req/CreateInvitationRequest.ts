export interface CreateInvitationRequest {
  loginId: string;
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
