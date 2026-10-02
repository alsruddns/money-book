import { baseApi } from "@/common/api/baseApi";
import type { TransactionType } from "@/transaction/dto/TransactionType";
import type { CreateCategoryRequest } from "../dto/req/CreateCategoryRequest";
import type { UpdateCategoryRequest } from "../dto/req/UpdateCategoryRequest";
import type { CategoryResponse } from "../dto/res/CategoryResponse";

interface CategoryListArg { moneyBookUid: number; transactionType?: TransactionType }
interface CategoryKey { moneyBookUid: number; categoryUid: number }
interface CreateArg { moneyBookUid: number; request: CreateCategoryRequest }
interface UpdateArg extends CategoryKey { request: UpdateCategoryRequest }

export const categoryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getCategories: builder.query<CategoryResponse[], CategoryListArg>({
      query: ({ moneyBookUid, transactionType }) => ({
        url: `money-books/${moneyBookUid}/categories`,
        params: transactionType ? { transactionType } : undefined,
      }),
      providesTags: (_result, _error, { moneyBookUid }) => [{ type: "Category", id: moneyBookUid }],
    }),
    createCategory: builder.mutation<CategoryResponse, CreateArg>({
      query: ({ moneyBookUid, request }) => ({ url: `money-books/${moneyBookUid}/categories`, method: "POST", body: request }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Category", id: moneyBookUid }],
    }),
    updateCategory: builder.mutation<CategoryResponse, UpdateArg>({
      query: ({ moneyBookUid, categoryUid, request }) => ({
        url: `money-books/${moneyBookUid}/categories/${categoryUid}`, method: "PATCH", body: request,
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [
        { type: "Category", id: moneyBookUid }, { type: "Transaction", id: moneyBookUid },
        { type: "Calendar", id: moneyBookUid }, { type: "Budget", id: moneyBookUid },
      ],
    }),
    deleteCategory: builder.mutation<void, CategoryKey>({
      query: ({ moneyBookUid, categoryUid }) => ({
        url: `money-books/${moneyBookUid}/categories/${categoryUid}`, method: "DELETE",
      }),
      invalidatesTags: (_result, error, { moneyBookUid }) => error ? [] : [{ type: "Category", id: moneyBookUid }],
    }),
  }),
});

export const { useGetCategoriesQuery, useCreateCategoryMutation, useUpdateCategoryMutation, useDeleteCategoryMutation } = categoryApi;
