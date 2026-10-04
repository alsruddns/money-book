import { baseApi } from "@/common/api/baseApi";

export interface BoardCategory { categoryUid: number; name: string; description: string; active: boolean; displayOrder: number }
export interface BoardPost { postUid: number; categoryUid: number; categoryName: string; title: string; content: string; authorDisplayName: string; commentCount: number; viewCount: number; createdAt: string; updatedAt?: string; secret: boolean; notice: boolean; canEdit: boolean; canDelete: boolean }
export interface BoardComment { commentUid: number; parentCommentUid?: number | null; authorDisplayName: string; content: string; createdAt: string; secret: boolean; deleted: boolean; canEdit: boolean; canDelete: boolean }
export interface BoardPage { content: BoardPost[]; totalPages: number; number: number; totalElements: number }
export interface PostInput { categoryUid: number; title: string; content: string; secret: boolean; notice?: boolean }
export interface CategoryInput { name: string; description: string; active: boolean; displayOrder: number }
const listTag = { type: "BoardPost" as const, id: "LIST" };

export const boardApi = baseApi.injectEndpoints({ endpoints: (build) => ({
  getBoardCategories: build.query<BoardCategory[], void>({ query: () => "board/categories", providesTags: ["BoardCategory"] }),
  getAdminBoardCategories: build.query<BoardCategory[], void>({ query: () => "admin/board/categories", providesTags: ["BoardCategory"] }),
  getBoardPosts: build.query<BoardPage, { categoryUid?: string; keyword?: string; page: number }>({
    query: (params) => ({ url: "board/posts", params }), providesTags: (result) => [...(result?.content.map(({ postUid }) => ({ type: "BoardPost" as const, id: postUid })) ?? []), listTag],
  }),
  getBoardPost: build.query<BoardPost, number>({ query: (uid) => `board/posts/${uid}`, providesTags: (_r, _e, uid) => [{ type: "BoardPost", id: uid }] }),
  getBoardComments: build.query<BoardComment[], number>({ query: (uid) => `board/posts/${uid}/comments`, providesTags: (_r, _e, uid) => [{ type: "BoardPost", id: uid }] }),
  createBoardPost: build.mutation<BoardPost, PostInput>({ query: (body) => ({ url: "board/posts", method: "POST", body }), invalidatesTags: [listTag] }),
  updateBoardPost: build.mutation<BoardPost, { uid: number; body: PostInput }>({ query: ({ uid, body }) => ({ url: `board/posts/${uid}`, method: "PUT", body }), invalidatesTags: (_r, _e, { uid }) => [listTag, { type: "BoardPost", id: uid }] }),
  deleteBoardPost: build.mutation<void, number>({ query: (uid) => ({ url: `board/posts/${uid}`, method: "DELETE" }), invalidatesTags: [listTag] }),
  setBoardNotice: build.mutation<void, { uid: number; notice: boolean }>({ query: ({ uid, notice }) => ({ url: `admin/board/posts/${uid}/notice`, method: "PATCH", body: { notice } }), invalidatesTags: (_r, _e, { uid }) => [listTag, { type: "BoardPost", id: uid }] }),
  createBoardComment: build.mutation<BoardComment, { uid: number; content: string; secret: boolean; parentCommentUid?: number }>({ query: ({ uid, ...body }) => ({ url: `board/posts/${uid}/comments`, method: "POST", body }), invalidatesTags: (_r, _e, { uid }) => [{ type: "BoardPost", id: uid }, listTag] }),
  updateBoardComment: build.mutation<BoardComment, { uid: number; commentUid: number; content: string; secret: boolean }>({ query: ({ uid, commentUid, ...body }) => ({ url: `board/posts/${uid}/comments/${commentUid}`, method: "PUT", body }), invalidatesTags: (_r, _e, { uid }) => [{ type: "BoardPost", id: uid }] }),
  deleteBoardComment: build.mutation<void, { uid: number; commentUid: number }>({ query: ({ uid, commentUid }) => ({ url: `board/posts/${uid}/comments/${commentUid}`, method: "DELETE" }), invalidatesTags: (_r, _e, { uid }) => [{ type: "BoardPost", id: uid }] }),
  createBoardCategory: build.mutation<BoardCategory, CategoryInput>({ query: (body) => ({ url: "admin/board/categories", method: "POST", body }), invalidatesTags: ["BoardCategory"] }),
  updateBoardCategory: build.mutation<BoardCategory, { uid: number; body: CategoryInput }>({ query: ({ uid, body }) => ({ url: `admin/board/categories/${uid}`, method: "PUT", body }), invalidatesTags: ["BoardCategory"] }),
}) });

export const { useGetBoardCategoriesQuery, useGetAdminBoardCategoriesQuery, useGetBoardPostsQuery, useGetBoardPostQuery, useGetBoardCommentsQuery, useCreateBoardPostMutation, useUpdateBoardPostMutation, useDeleteBoardPostMutation, useSetBoardNoticeMutation, useCreateBoardCommentMutation, useUpdateBoardCommentMutation, useDeleteBoardCommentMutation, useCreateBoardCategoryMutation, useUpdateBoardCategoryMutation } = boardApi;
