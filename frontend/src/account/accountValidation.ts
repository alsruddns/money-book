import type { AccountPasswordUpdateReqDto } from "./dto/req/AccountPasswordUpdateReqDto";

export function validateNickname(value: string): string | null {
  const nickname = value.trim();
  if (!nickname) return "닉네임을 입력해 주세요.";
  if (nickname.length > 50) return "닉네임은 50자 이내로 입력해 주세요.";
  return null;
}

export function validatePasswordUpdate(value: AccountPasswordUpdateReqDto): string | null {
  if (!value.currentPassword.trim()) return "현재 비밀번호를 입력해 주세요.";
  if (!value.newPassword.trim()) return "새 비밀번호를 입력해 주세요.";
  if (!value.newPasswordConfirm.trim()) return "새 비밀번호 확인을 입력해 주세요.";
  if (value.currentPassword.length > 72 || value.newPassword.length > 72 || value.newPasswordConfirm.length > 72) {
    return "비밀번호는 72자 이내로 입력해 주세요.";
  }
  if (new TextEncoder().encode(value.newPassword).length > 72) {
    return "새 비밀번호는 UTF-8 기준 72바이트 이내로 입력해 주세요.";
  }
  if (value.newPassword !== value.newPasswordConfirm) return "새 비밀번호 확인 값이 일치하지 않습니다.";
  if (value.currentPassword === value.newPassword) return "새 비밀번호는 현재 비밀번호와 달라야 합니다.";
  return null;
}
