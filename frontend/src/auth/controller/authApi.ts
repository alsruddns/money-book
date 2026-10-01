import { baseApi } from "@/common/api/baseApi";
import type { LoginRequest } from "../dto/req/LoginRequest";
import type { SignUpReqDto } from "../dto/req/SignUpReqDto";
import type { RefreshRequest } from "../dto/req/RefreshRequest";
import type { LoginResponse } from "../dto/res/LoginResponse";
import type { SignUpResDto } from "../dto/res/SignUpResDto";
import type { RefreshResponse } from "../dto/res/RefreshResponse";
import type { CurrentUserResponse } from "../dto/res/CurrentUserResponse";

export const authApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    signup: builder.mutation<SignUpResDto, SignUpReqDto>({
      query: (body) => ({ url: "auth/signup", method: "POST", body }),
    }),
    login: builder.mutation<LoginResponse, LoginRequest>({
      query: (body) => ({ url: "auth/login", method: "POST", body }),
    }),
    refreshToken: builder.mutation<RefreshResponse, RefreshRequest>({
      query: (body) => ({ url: "auth/refresh", method: "POST", body }),
    }),
    getCurrentUser: builder.query<CurrentUserResponse, void>({
      query: () => "auth/me",
    }),
  }),
});

export const {
  useSignupMutation,
  useLoginMutation,
  useRefreshTokenMutation,
  useGetCurrentUserQuery,
} = authApi;
