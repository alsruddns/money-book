import { baseApi } from "@/common/api/baseApi";
import type { CreateMoneyBookRequest } from "../dto/req/CreateMoneyBookRequest";
import type { CreateInvitationRequest } from "../dto/req/CreateInvitationRequest";
import type { UpdateMoneyBookMemberPermissionRequest } from "../dto/req/UpdateMoneyBookMemberPermissionRequest";
import type { CreateMoneyBookResponse } from "../dto/res/CreateMoneyBookResponse";
import type { MoneyBookListResponse } from "../dto/res/MoneyBookListResponse";
import type { PendingInvitationResponse } from "../dto/res/PendingInvitationResponse";
import type { InvitationResponse } from "../dto/res/InvitationResponse";
import type { MoneyBookMemberResponse } from "../dto/res/MoneyBookMemberResponse";

interface InvitationKey { moneyBookUid: number; moneyBookUserUid: number }
type MemberKey = InvitationKey;
interface InviteMemberArg { moneyBookUid: number; request: CreateInvitationRequest }
interface UpdatePermissionArg extends MemberKey { request: UpdateMoneyBookMemberPermissionRequest }

export const moneyBookApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getMoneyBooks: builder.query<MoneyBookListResponse[], void>({
      query: () => "money-books",
      providesTags: ["MoneyBook"],
    }),
    createMoneyBook: builder.mutation<CreateMoneyBookResponse, CreateMoneyBookRequest>({
      query: (body) => ({ url: "money-books", method: "POST", body }),
      invalidatesTags: (_result, error) => error ? [] : ["MoneyBook", "MoneyBookActivity"],
    }),
    getPendingInvitations: builder.query<PendingInvitationResponse[], void>({
      query: () => "money-books/invitations",
      providesTags: ["MoneyBookInvitation"],
    }),
    inviteMoneyBookUser: builder.mutation<InvitationResponse, InviteMemberArg>({
      query: ({ moneyBookUid, request }) => ({
        url: `money-books/${moneyBookUid}/invitations`, method: "POST", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "MoneyBookActivity", id: moneyBookUid }],
    }),
    acceptInvitation: builder.mutation<InvitationResponse, InvitationKey>({
      query: ({ moneyBookUid, moneyBookUserUid }) => ({
        url: `money-books/${moneyBookUid}/invitations/${moneyBookUserUid}/accept`, method: "PATCH",
      }),
      invalidatesTags: (_result, error) => error ? [] : ["MoneyBookInvitation", "MoneyBook", "MoneyBookActivity"],
    }),
    rejectInvitation: builder.mutation<InvitationResponse, InvitationKey>({
      query: ({ moneyBookUid, moneyBookUserUid }) => ({
        url: `money-books/${moneyBookUid}/invitations/${moneyBookUserUid}/reject`, method: "PATCH",
      }),
      invalidatesTags: (_result, error) => error ? [] : ["MoneyBookInvitation", "MoneyBookActivity"],
    }),
    getMoneyBookMembers: builder.query<MoneyBookMemberResponse[], number>({
      query: (moneyBookUid) => `money-books/${moneyBookUid}/members`,
      providesTags: (_result, _error, moneyBookUid) => [{ type: "MoneyBookMember", id: moneyBookUid }],
    }),
    updateMoneyBookMemberPermission: builder.mutation<void, UpdatePermissionArg>({
      query: ({ moneyBookUid, moneyBookUserUid, request }) => ({
        url: `money-books/${moneyBookUid}/members/${moneyBookUserUid}/permissions`,
        method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "MoneyBookMember", id: moneyBookUid }, "MoneyBook", { type: "MoneyBookActivity", id: moneyBookUid },
      ],
    }),
    removeMoneyBookMember: builder.mutation<void, MemberKey>({
      query: ({ moneyBookUid, moneyBookUserUid }) => ({
        url: `money-books/${moneyBookUid}/members/${moneyBookUserUid}`, method: "DELETE",
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "MoneyBookMember", id: moneyBookUid }, "MoneyBook", { type: "MoneyBookActivity", id: moneyBookUid },
      ],
    }),
  }),
});

export const {
  useGetMoneyBooksQuery,
  useCreateMoneyBookMutation,
  useGetPendingInvitationsQuery,
  useInviteMoneyBookUserMutation,
  useAcceptInvitationMutation,
  useRejectInvitationMutation,
  useGetMoneyBookMembersQuery,
  useUpdateMoneyBookMemberPermissionMutation,
  useRemoveMoneyBookMemberMutation,
} = moneyBookApi;
