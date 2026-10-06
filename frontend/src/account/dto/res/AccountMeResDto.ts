export type AccountAuthProvider = "LOCAL" | "GOOGLE" | "KAKAO" | "NAVER" | (string & {});
export type AccountUserStatus = "ACTIVE" | "INACTIVE" | "WITHDRAWN" | "BLOCKED" | (string & {});
export type AccountSystemRole = "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN" | (string & {});

export interface AccountMeResDto {
  userUid: number;
  nickname: string;
  status: AccountUserStatus;
  systemRole: AccountSystemRole;
  providers: AccountAuthProvider[];
  loginId: string | null;
  regTime: string;
  modTime: string;
}
