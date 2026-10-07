import type { Locale } from "./config";

export const messages = {
  ko: { common: { save: "저장", cancel: "취소", delete: "삭제", edit: "수정", loading: "불러오는 중...", language: "언어 선택", home: "홈" }, auth: { login: "로그인", signup: "회원가입", logout: "로그아웃", loginId: "로그인 아이디", password: "비밀번호" }, books: { select: "내 가계부", create: "가계부 만들기" }, board: { title: "게시판", write: "글쓰기" }, account: { title: "계정 관리" }, errors: { forbidden: "권한이 없습니다.", notFound: "페이지를 찾을 수 없습니다." } },
  en: { common: { save: "Save", cancel: "Cancel", delete: "Delete", edit: "Edit", loading: "Loading...", language: "Select language", home: "Home" }, auth: { login: "Log in", signup: "Sign up", logout: "Log out", loginId: "Login ID", password: "Password" }, books: { select: "My books", create: "Create a book" }, board: { title: "Community", write: "Write a post" }, account: { title: "Account settings" }, errors: { forbidden: "You do not have permission to view this page.", notFound: "Page not found." } },
  ja: { common: { save: "保存", cancel: "キャンセル", delete: "削除", edit: "編集", loading: "読み込み中...", language: "言語を選択", home: "ホーム" }, auth: { login: "ログイン", signup: "新規登録", logout: "ログアウト", loginId: "ログインID", password: "パスワード" }, books: { select: "家計簿", create: "家計簿を作成" }, board: { title: "掲示板", write: "投稿する" }, account: { title: "アカウント設定" }, errors: { forbidden: "このページを表示する権限がありません。", notFound: "ページが見つかりません。" } },
  zh: { common: { save: "保存", cancel: "取消", delete: "删除", edit: "编辑", loading: "加载中...", language: "选择语言", home: "首页" }, auth: { login: "登录", signup: "注册", logout: "退出登录", loginId: "登录名", password: "密码" }, books: { select: "我的账本", create: "创建账本" }, board: { title: "社区", write: "发表" }, account: { title: "账户设置" }, errors: { forbidden: "您没有权限查看此页面。", notFound: "未找到页面。" } },
} as const;

export function translate(locale: Locale, key: string): string {
  const value: unknown = key.split(".").reduce<unknown>((current, part) => current && typeof current === "object" ? (current as Record<string, unknown>)[part] : undefined, messages[locale]);
  return typeof value === "string" ? value : key;
}
