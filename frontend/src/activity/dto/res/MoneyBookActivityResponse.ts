export interface MoneyBookActivityResponse {
  activityUid: number;
  actorUserUid: number;
  actorNickname: string;
  activityType: string;
  targetType: string;
  targetUid: number | null;
  summary: string;
  metadataJson: string | null;
  occurredAt: string;
}
