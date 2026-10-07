export type EmailVerificationPurpose = "SIGNUP" | "ACCOUNT_EMAIL";

export interface SecurityQuestionResponse {
  code: string;
  question: string;
}

export interface EmailVerificationResponse {
  verificationUid: number;
  message: string;
}

export interface EmailGrantResponse {
  verificationToken: string;
}

export interface AccountSecurityResponse {
  securityQuestionConfigured: boolean;
  securityQuestionCode: string | null;
  recoveryCodesRemaining: number;
  emailVerified: boolean;
  maskedEmail: string | null;
  passwordChangeRequired: boolean;
}

export interface RecoveryCodesResponse {
  recoveryCodes: string[];
  message: string;
}
