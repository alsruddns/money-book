export interface LoginResponse {
  userUid: number;
  nickname: string;
  accessToken: string;
  refreshToken: string;
  passwordChangeRequired: boolean;
}
