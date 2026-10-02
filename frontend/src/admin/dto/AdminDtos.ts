export type SystemRole = "USER" | "SYSTEM_ADMIN" | "SUPER_ADMIN" | (string & {});
export type UserStatus = "ACTIVE" | "INACTIVE" | "WITHDRAWN" | "BLOCKED" | (string & {});
export interface AdminMeResponse { userUid: number; nickname: string; systemRole: SystemRole }
export interface AdminOverviewResponse { totalUsers: number; activeUsers: number; suspendedUsers: number; systemAdminCount: number; superAdminCount: number; totalMoneyBooks: number; activitiesToday: number }
export interface AdminPageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; first: boolean; last: boolean }
export interface AdminUserResponse { userUid: number; loginId: string; nickname: string; status: UserStatus; systemRole: SystemRole; createdAt: string }
export interface AdminUserDetailResponse extends AdminUserResponse { updatedAt: string; ownedMoneyBookCount: number; joinedMoneyBookCount: number }
export interface AdminMoneyBookResponse { moneyBookUid: number; name: string; ownerUserUid: number; ownerNickname: string; memberCount: number; createdAt: string; lastActivityAt: string | null }
export interface AdminMoneyBookDetailResponse extends AdminMoneyBookResponse { categoryCount: number; accountCount: number; transactionCount: number; transferCount: number; monthClosingCount: number }
export interface AdminActivityResponse { activityUid: number; moneyBookUid: number; moneyBookName: string | null; actorUserUid: number; actorNickname: string; activityType: string; targetType: string; targetUid: number | null; summary: string; metadataJson: string | null; occurredAt: string }
export type AdminAuditActionType = "USER_DETAIL_VIEWED" | "USER_STATUS_CHANGED" | "USER_SYSTEM_ROLE_CHANGED" | "MONEY_BOOK_DETAIL_VIEWED" | "ADMIN_ACTIVITY_SEARCHED" | (string & {});
export type AdminAuditTargetType = "USER" | "MONEY_BOOK" | "ACTIVITY" | (string & {});
export interface AdminAuditLogResponse { adminAuditLogUid: number; actorUserUid: number; actorNickname: string; actorSystemRole: SystemRole; actionType: AdminAuditActionType; targetType: AdminAuditTargetType; targetUid: number | null; summary: string; occurredAt: string }
export interface AdminListQuery { page: number; size: number; keyword?: string; status?: string; systemRole?: string; ownerUserUid?: string }
export interface AdminActivityQuery { page: number; size: number; moneyBookUid?: string; actorUserUid?: string; activityType?: string; targetType?: string; startDate?: string; endDate?: string }
export interface AdminAuditQuery { page: number; size: number; actorUserUid?: string; actionType?: string; targetType?: string; startDate?: string; endDate?: string }
