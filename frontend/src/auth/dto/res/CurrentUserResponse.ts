export type UserStatus = "ACTIVE" | "INACTIVE" | "WITHDRAWN" | "BLOCKED";

export interface CurrentUserResponse {
  userUid: number;
  nickname: string;
  status: UserStatus;
  systemRole: "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN";
  passwordChangeRequired: boolean;
}
