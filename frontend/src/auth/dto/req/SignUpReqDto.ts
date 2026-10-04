export interface SignUpReqDto {
  loginId: string;
  password: string;
  passwordConfirm: string;
  nickname: string;
  securityQuestionCode: string;
  securityAnswer: string;
  emailVerificationToken?: string;
}
