export interface UpdateMoneyBookMemberPermissionRequest {
  isAdmin: boolean;
  canCreate: boolean;
  canRead: boolean;
  canUpdate: boolean;
  canDelete: boolean;
}
