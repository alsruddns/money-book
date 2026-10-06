"use client";
import { useGetAdminActivitiesQuery, useGetAdminAuditLogsQuery, useGetAdminMoneyBookQuery, useGetAdminMoneyBookMembersQuery, useGetAdminMoneyBooksQuery, useGetAdminOverviewQuery, useGetAdminUserActivitiesQuery, useGetAdminUserQuery, useGetAdminUsersQuery, useChangeAdminSystemRoleMutation, useChangeAdminUserStatusMutation, useRevokeAdminUserSessionsMutation } from "../controller/adminApi";
import type { AdminActivityQuery, AdminAuditQuery, AdminListQuery } from "../dto/AdminDtos";
import { getApiErrorMessage } from "@/common/api/getApiErrorMessage";

function getAdminReadError(error: unknown, fallback: string, notFound: string) {
  if (typeof error === "object" && error !== null && "status" in error) {
    const status = (error as { status?: unknown }).status;
    if (status === 403) return "이 기능을 사용할 권한이 없습니다.";
    if (status === 404) return notFound;
    if (typeof status === "number" && status >= 500) return "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
  }
  return getApiErrorMessage(error, fallback);
}

export function useAdminOverview() { const query = useGetAdminOverviewQuery(); return { data: query.currentData, loading: query.isLoading, error: query.isError ? getApiErrorMessage(query.error, "운영 현황을 불러오지 못했습니다.") : null, retry: query.refetch }; }
export function useAdminUsers(query: AdminListQuery) { const result = useGetAdminUsersQuery(query); return { data: result.currentData, loading: result.isLoading, fetching: result.isFetching, error: result.isError ? getApiErrorMessage(result.error, "사용자 목록을 불러오지 못했습니다.") : null, retry: result.refetch }; }
export function useAdminUser(uid: number) { const query = useGetAdminUserQuery(uid, { skip: !Number.isSafeInteger(uid) || uid <= 0 }); return { data: query.currentData, loading: query.isLoading, error: query.isError ? getAdminReadError(query.error, "사용자 정보를 불러오지 못했습니다.", "사용자를 찾을 수 없습니다.") : null, retry: query.refetch }; }
export function useAdminUserActivities(userUid: number, page = 0, size = 10) { const query = useGetAdminUserActivitiesQuery({ userUid, page, size }, { skip: !Number.isSafeInteger(userUid) || userUid <= 0 }); return { data: query.currentData, loading: query.isLoading, error: query.isError ? getApiErrorMessage(query.error, "사용자 활동을 불러오지 못했습니다.") : null, retry: query.refetch }; }
export function useAdminMoneyBookMembers(moneyBookUid: number, page = 0, size = 20) { const query = useGetAdminMoneyBookMembersQuery({ moneyBookUid, page, size }, { skip: !Number.isSafeInteger(moneyBookUid) || moneyBookUid <= 0 }); return { data: query.currentData, loading: query.isLoading, error: query.isError ? getApiErrorMessage(query.error, "가계부 멤버를 불러오지 못했습니다.") : null, retry: query.refetch }; }
export function useAdminMoneyBooks(query: AdminListQuery) { const result = useGetAdminMoneyBooksQuery(query); return { data: result.currentData, loading: result.isLoading, fetching: result.isFetching, error: result.isError ? getApiErrorMessage(result.error, "가계부 목록을 불러오지 못했습니다.") : null, retry: result.refetch }; }
export function useAdminMoneyBook(uid: number) { const query = useGetAdminMoneyBookQuery(uid, { skip: !Number.isSafeInteger(uid) || uid <= 0 }); return { data: query.currentData, loading: query.isLoading, error: query.isError ? getAdminReadError(query.error, "가계부 정보를 불러오지 못했습니다.", "가계부를 찾을 수 없습니다.") : null, retry: query.refetch }; }
export function useAdminActivities(query: AdminActivityQuery, skip = false) { const result = useGetAdminActivitiesQuery(query, { skip }); return { data: result.currentData, loading: result.isLoading, fetching: result.isFetching, error: result.isError ? getApiErrorMessage(result.error, "전체 활동내역을 불러오지 못했습니다.") : null, retry: result.refetch }; }
export function useAdminAuditLogs(query: AdminAuditQuery) { const result = useGetAdminAuditLogsQuery(query); return { data: result.currentData, loading: result.isLoading, fetching: result.isFetching, error: result.isError ? getApiErrorMessage(result.error, "감사로그를 불러오지 못했습니다.") : null, retry: result.refetch }; }
export function useAdminUserMutations() { const [status, statusState] = useChangeAdminUserStatusMutation(); const [role, roleState] = useChangeAdminSystemRoleMutation(); const error = statusState.error || roleState.error; return { changeStatus: status, changeRole: role, isSaving: statusState.isLoading || roleState.isLoading, errorMessage: error ? getApiErrorMessage(error, "요청을 처리하지 못했습니다. 권한을 확인하고 다시 시도해 주세요.") : null }; }
export function useRevokeAdminUserSessions() { const [revoke, state] = useRevokeAdminUserSessionsMutation(); return { revokeSessions: revoke, isRevoking: state.isLoading, revokedSessionCount: state.data, errorMessage: state.error ? getApiErrorMessage(state.error, "세션을 종료하지 못했습니다. 권한을 확인하고 다시 시도해 주세요.") : null }; }
