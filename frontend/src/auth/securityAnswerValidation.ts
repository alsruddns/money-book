export const securityAnswerWhitespaceMessage = "보안 질문 답변의 앞뒤에는 공백을 입력할 수 없습니다.";

export function hasSecurityAnswerBoundaryWhitespace(value: string): boolean {
  return value !== value.trim();
}
