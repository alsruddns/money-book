import { baseApi } from "@/common/api/baseApi";
import type { EmailGrantResponse, EmailVerificationPurpose, EmailVerificationResponse, SecurityQuestionResponse } from "../dto/RecoveryDtos";

interface EmailVerificationRequest { email: string; purpose: EmailVerificationPurpose }
interface EmailVerificationConfirmRequest { verificationUid: number; code: string }
interface NewPasswordRequest { newPassword: string; newPasswordConfirm: string }
interface EmailResetRequest extends NewPasswordRequest { verificationToken: string }
interface QuestionResetRequest extends NewPasswordRequest { loginId: string; questionCode: string; answer: string }
interface RecoveryCodeResetRequest extends NewPasswordRequest { loginId: string; recoveryCode: string }
interface EmailResetRequestBody { loginId: string; email: string }

export const passwordRecoveryApi = baseApi.injectEndpoints({
  endpoints: (builder) => ({
    getSecurityQuestions: builder.query<SecurityQuestionResponse[], void>({ query: () => "auth/security-questions" }),
    requestEmailVerification: builder.mutation<EmailVerificationResponse, EmailVerificationRequest>({
      query: (body) => ({ url: "auth/email-verifications/request", method: "POST", body }),
    }),
    confirmEmailVerification: builder.mutation<EmailGrantResponse, EmailVerificationConfirmRequest>({
      query: (body) => ({ url: "auth/email-verifications/confirm", method: "POST", body }),
    }),
    requestPasswordResetEmail: builder.mutation<EmailVerificationResponse, EmailResetRequestBody>({
      query: (body) => ({ url: "auth/password-recovery/email/request", method: "POST", body }),
    }),
    resetPasswordByEmail: builder.mutation<void, EmailResetRequest>({
      query: (body) => ({ url: "auth/password-recovery/email/reset", method: "POST", body }),
    }),
    resetPasswordByQuestion: builder.mutation<void, QuestionResetRequest>({
      query: (body) => ({ url: "auth/password-recovery/security-question/reset", method: "POST", body }),
    }),
    resetPasswordByRecoveryCode: builder.mutation<void, RecoveryCodeResetRequest>({
      query: (body) => ({ url: "auth/password-recovery/recovery-code/reset", method: "POST", body }),
    }),
    getAccountSecurity: builder.query<import("../dto/RecoveryDtos").AccountSecurityResponse, void>({
      query: () => "account/security", providesTags: ["AccountSecurity"],
    }),
    updateSecurityQuestion: builder.mutation<void, { questionCode: string; answer: string; currentPassword: string }>({
      query: (body) => ({ url: "account/security/security-question", method: "PATCH", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountSecurity"],
    }),
    regenerateRecoveryCodes: builder.mutation<import("../dto/RecoveryDtos").RecoveryCodesResponse, { currentPassword: string }>({
      query: (body) => ({ url: "account/security/recovery-codes/regenerate", method: "POST", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountSecurity"],
    }),
    applyAccountEmail: builder.mutation<void, { verificationToken: string }>({
      query: (body) => ({ url: "account/security/email", method: "PUT", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountSecurity"],
    }),
    removeAccountEmail: builder.mutation<void, { currentPassword: string }>({
      query: (body) => ({ url: "account/security/email", method: "DELETE", body }),
      invalidatesTags: (_result, error) => error ? [] : ["AccountSecurity"],
    }),
  }),
});

export const {
  useGetSecurityQuestionsQuery, useRequestEmailVerificationMutation, useConfirmEmailVerificationMutation,
  useRequestPasswordResetEmailMutation, useResetPasswordByEmailMutation, useResetPasswordByQuestionMutation,
  useResetPasswordByRecoveryCodeMutation, useGetAccountSecurityQuery, useUpdateSecurityQuestionMutation,
  useRegenerateRecoveryCodesMutation, useApplyAccountEmailMutation, useRemoveAccountEmailMutation,
} = passwordRecoveryApi;
