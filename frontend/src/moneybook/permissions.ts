import type { UpdateMoneyBookMemberPermissionRequest } from "./dto/req/UpdateMoneyBookMemberPermissionRequest";

export type MemberPermissions = UpdateMoneyBookMemberPermissionRequest;

export const permissionLabels = [
  ["canCreate", "생성"],
  ["canRead", "조회"],
  ["canUpdate", "수정"],
  ["canDelete", "삭제"],
] as const;

export function normalizePermissions(permissions: MemberPermissions): MemberPermissions {
  if (!permissions.isAdmin) return permissions;
  return {
    isAdmin: true,
    canCreate: true,
    canRead: true,
    canUpdate: true,
    canDelete: true,
  };
}

export function permissionSummary(permissions: MemberPermissions): string[] {
  if (permissions.isAdmin) return ["관리자", ...permissionLabels.map(([, label]) => label)];
  const labels = permissionLabels
    .filter(([key]) => permissions[key])
    .map(([, label]) => label);
  return labels.length ? labels : ["권한 없음"];
}
