import type { AccountAuthProvider, AccountSystemRole, AccountUserStatus } from "./dto/res/AccountMeResDto";
import { formatKoreaDateTime } from "@/common/format/dateTime";

const providerLabels: Record<string, string> = {
  LOCAL: "로컬 계정",
  GOOGLE: "Google",
  KAKAO: "Kakao",
  NAVER: "Naver",
};

const statusLabels: Record<string, string> = {
  ACTIVE: "활성",
  INACTIVE: "비활성",
  WITHDRAWN: "탈퇴",
  BLOCKED: "차단됨",
};

const roleLabels: Record<string, string> = {
  USER: "일반 사용자",
  SYSTEM_ADMIN: "시스템 관리자",
  SUPER_ADMIN: "최고 관리자",
};

export function accountProviderLabel(provider: AccountAuthProvider): string {
  return providerLabels[provider] ?? provider;
}

export function accountStatusLabel(status: AccountUserStatus): string {
  return statusLabels[status] ?? status;
}

export function accountRoleLabel(role: AccountSystemRole): string {
  return roleLabels[role] ?? role;
}

export function formatAccountDateTime(value: string): string {
  return formatKoreaDateTime(value, value);
}
