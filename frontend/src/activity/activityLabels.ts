export const activityTypes = ["MONEY_BOOK_CREATED","MEMBER_INVITED","MEMBER_INVITATION_ACCEPTED","MEMBER_INVITATION_REJECTED","MEMBER_PERMISSION_UPDATED","MEMBER_REMOVED","CATEGORY_CREATED","CATEGORY_UPDATED","CATEGORY_DELETED","ACCOUNT_CREATED","ACCOUNT_UPDATED","ACCOUNT_DELETED","TRANSACTION_CREATED","TRANSACTION_UPDATED","TRANSACTION_DELETED","TRANSFER_CREATED","TRANSFER_UPDATED","TRANSFER_DELETED","RECURRING_CREATED","RECURRING_UPDATED","RECURRING_ACTIVATED","RECURRING_DEACTIVATED","RECURRING_DELETED","RECURRING_GENERATED","BUDGET_UPDATED","MONTH_CLOSED","MONTH_CLOSING_CANCELLED","SETTING_UPDATED","BACKUP_EXPORTED","BACKUP_RESTORED"] as const;
export type ActivityType = typeof activityTypes[number];
export const targetTypes = ["MONEY_BOOK","MEMBER","INVITATION","CATEGORY","ACCOUNT","TRANSACTION","TRANSFER","RECURRING_TRANSACTION","BUDGET","MONTH_CLOSING","SETTING","BACKUP"] as const;
export type ActivityTargetType = typeof targetTypes[number];
export const activityTypeLabels: Record<ActivityType, string> = {
  MONEY_BOOK_CREATED:"가계부 생성", MEMBER_INVITED:"멤버 초대", MEMBER_INVITATION_ACCEPTED:"초대 수락", MEMBER_INVITATION_REJECTED:"초대 거절", MEMBER_PERMISSION_UPDATED:"멤버 권한 변경", MEMBER_REMOVED:"멤버 제거",
  CATEGORY_CREATED:"카테고리 등록", CATEGORY_UPDATED:"카테고리 수정", CATEGORY_DELETED:"카테고리 삭제", ACCOUNT_CREATED:"계좌 등록", ACCOUNT_UPDATED:"계좌 수정", ACCOUNT_DELETED:"계좌 삭제",
  TRANSACTION_CREATED:"거래 등록", TRANSACTION_UPDATED:"거래 수정", TRANSACTION_DELETED:"거래 삭제", TRANSFER_CREATED:"이체 등록", TRANSFER_UPDATED:"이체 수정", TRANSFER_DELETED:"이체 삭제",
  RECURRING_CREATED:"정기 거래 등록", RECURRING_UPDATED:"정기 거래 수정", RECURRING_ACTIVATED:"정기 거래 활성화", RECURRING_DEACTIVATED:"정기 거래 비활성화", RECURRING_DELETED:"정기 거래 삭제", RECURRING_GENERATED:"정기 거래 생성",
  BUDGET_UPDATED:"예산 변경", MONTH_CLOSED:"월 결산", MONTH_CLOSING_CANCELLED:"결산 취소", SETTING_UPDATED:"설정 변경", BACKUP_EXPORTED:"백업 내보내기", BACKUP_RESTORED:"백업 복원",
};
export const targetTypeLabels: Record<ActivityTargetType, string> = { MONEY_BOOK:"가계부", MEMBER:"멤버", INVITATION:"초대", CATEGORY:"카테고리", ACCOUNT:"계좌", TRANSACTION:"거래", TRANSFER:"이체", RECURRING_TRANSACTION:"정기 수입/지출", BUDGET:"예산", MONTH_CLOSING:"월 결산", SETTING:"설정", BACKUP:"백업" };
export function getActivityTypeLabel(type: string): string { return type in activityTypeLabels ? activityTypeLabels[type as ActivityType] : type; }
export function getTargetTypeLabel(type: string): string { return type in targetTypeLabels ? targetTypeLabels[type as ActivityTargetType] : type; }

export function formatActivityTime(iso: string): { dateKey: string; group: string; time: string } {
  const normalized = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(iso) ? iso : `${iso}+09:00`;
  const date = new Date(normalized);
  if (Number.isNaN(date.getTime())) return { dateKey: iso.slice(0,10), group: iso.slice(0,10), time: "" };
  const dateKey = new Intl.DateTimeFormat("sv-SE", { timeZone:"Asia/Seoul", year:"numeric", month:"2-digit", day:"2-digit" }).format(date);
  const today = new Intl.DateTimeFormat("sv-SE", { timeZone:"Asia/Seoul", year:"numeric", month:"2-digit", day:"2-digit" }).format(new Date());
  const yesterdayDate = new Date(`${today}T00:00:00+09:00`); yesterdayDate.setDate(yesterdayDate.getDate()-1);
  const yesterday = new Intl.DateTimeFormat("sv-SE", { timeZone:"Asia/Seoul", year:"numeric", month:"2-digit", day:"2-digit" }).format(yesterdayDate);
  const group = dateKey === today ? "오늘" : dateKey === yesterday ? "어제" : dateKey.replaceAll("-", ".");
  return { dateKey, group, time: new Intl.DateTimeFormat("ko-KR", { timeZone:"Asia/Seoul", hour:"numeric", minute:"2-digit" }).format(date) };
}

export function getActivityMetadataHint(type: string, metadataJson: string | null): string | null {
  if (!metadataJson) return null;
  try {
    const value: unknown = JSON.parse(metadataJson);
    if (!value || typeof value !== "object") return null;
    const data = value as Record<string, unknown>;
    if (type === "RECURRING_GENERATED" && typeof data.generatedCount === "number") return `생성 ${data.generatedCount}건`;
    if ((type === "MONTH_CLOSED" || type === "MONTH_CLOSING_CANCELLED" || type === "BUDGET_UPDATED") && Number.isInteger(data.year) && Number.isInteger(data.month)) return `${data.year}년 ${data.month}월`;
    if (type === "SETTING_UPDATED" && (data.weekStartDay === "SUNDAY" || data.weekStartDay === "MONDAY")) return `주 시작: ${data.weekStartDay === "SUNDAY" ? "일요일" : "월요일"}`;
    return null;
  } catch { return null; }
}
