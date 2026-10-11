import type { Locale } from "./config";

export const categoryMessages: Record<Locale, Record<string, string>> = {
  ko: {
    title: "카테고리", description: "수입과 지출 카테고리를 계층별로 관리합니다.", add: "대분류 추가",
    large: "대분류", medium: "중분류", small: "소분류", selectLarge: "대분류 선택", selectMedium: "중분류 선택", selectSmall: "소분류 선택",
    noMedium: "등록된 중분류가 없습니다", noSmall: "등록된 소분류가 없습니다", addMedium: "중분류 추가", addSmall: "소분류 추가",
    empty: "등록된 카테고리가 없습니다.", order: "정렬 순서 {order}", type: "유형", name: "이름", parent: "상위 카테고리",
    createTitle: "카테고리 추가", editTitle: "카테고리 수정", save: "저장", saving: "저장 중...", deleteConfirm: "'{name}' 카테고리를 삭제할까요?",
    deleteTreeConfirm: "'{name}' 카테고리를 삭제하면 하위 카테고리도 모두 함께 삭제됩니다. 계속할까요?",
  },
  en: {
    title: "Categories", description: "Manage income and expense categories by level.", add: "Add large category",
    large: "Large category", medium: "Medium category", small: "Small category", selectLarge: "Select a large category", selectMedium: "Select a medium category", selectSmall: "Select a small category",
    noMedium: "No medium categories yet", noSmall: "No small categories yet", addMedium: "Add medium category", addSmall: "Add small category",
    empty: "No categories have been added.", order: "Sort order {order}", type: "Type", name: "Name", parent: "Parent category",
    createTitle: "Add category", editTitle: "Edit category", save: "Save", saving: "Saving...", deleteConfirm: "Delete the '{name}' category?",
    deleteTreeConfirm: "Deleting '{name}' will also delete all categories below it. Continue?",
  },
  ja: {
    title: "カテゴリ", description: "収入と支出のカテゴリを階層ごとに管理します。", add: "大分類を追加",
    large: "大分類", medium: "中分類", small: "小分類", selectLarge: "大分類を選択", selectMedium: "中分類を選択", selectSmall: "小分類を選択",
    noMedium: "中分類はまだありません", noSmall: "小分類はまだありません", addMedium: "中分類を追加", addSmall: "小分類を追加",
    empty: "カテゴリはまだありません。", order: "並び順 {order}", type: "種類", name: "名前", parent: "親カテゴリ",
    createTitle: "カテゴリを追加", editTitle: "カテゴリを編集", save: "保存", saving: "保存中...", deleteConfirm: "「{name}」を削除しますか？",
    deleteTreeConfirm: "「{name}」を削除すると、配下のカテゴリもすべて削除されます。続行しますか？",
  },
  zh: {
    title: "分类", description: "按层级管理收入和支出分类。", add: "添加大类",
    large: "大类", medium: "中类", small: "小类", selectLarge: "选择大类", selectMedium: "选择中类", selectSmall: "选择小类",
    noMedium: "暂无中类", noSmall: "暂无小类", addMedium: "添加中类", addSmall: "添加小类",
    empty: "尚未添加分类。", order: "排序 {order}", type: "类型", name: "名称", parent: "上级分类",
    createTitle: "添加分类", editTitle: "编辑分类", save: "保存", saving: "正在保存...", deleteConfirm: "要删除“{name}”分类吗？",
    deleteTreeConfirm: "删除“{name}”后，其下的分类也会一起删除。是否继续？",
  },
};
