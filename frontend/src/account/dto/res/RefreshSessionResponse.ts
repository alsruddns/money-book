export interface RefreshSessionResponse {
  sessionUid: number;
  current: boolean;
  userAgent: string | null;
  ipAddress: string | null;
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string;
}
