"use client";

import { useState } from "react";
import { usePathname } from "next/navigation";
import DialogShell from "@/common/components/DialogShell";
import { getLocaleFromPathname } from "@/i18n/config";
import Link from "./MoneyLink";

const copy = {
  ko: { privacy: "개인정보 안내", terms: "이용안내", contact: "문의 사항", title: "문의 사항", intro: "woori.today 서비스 이용 중 궁금한 점이나 요청사항이 있으신가요?", lead: "다음과 같은 문의를 받고 있습니다.", types: ["일반 문의 및 요청사항", "협업 문의", "광고 문의 및 제안", "버그 제보"], note: "문의 내용을 확인한 후 가능한 범위에서 답변드리겠습니다.", close: "닫기" },
  en: { privacy: "Privacy", terms: "Terms", contact: "Contact", title: "Contact", intro: "Do you have a question or request about using woori.today?", lead: "We welcome inquiries about:", types: ["General questions and requests", "Partnerships", "Advertising inquiries and proposals", "Bug reports"], note: "We will review your message and respond where possible.", close: "Close" },
  ja: { privacy: "プライバシー", terms: "利用案内", contact: "お問い合わせ", title: "お問い合わせ", intro: "woori.todayのご利用について、ご質問やご要望はありますか？", lead: "次のようなお問い合わせを受け付けています。", types: ["一般的な質問・ご要望", "協業に関するお問い合わせ", "広告に関するお問い合わせ・ご提案", "不具合の報告"], note: "内容を確認し、可能な範囲で回答いたします。", close: "閉じる" },
  zh: { privacy: "隐私说明", terms: "使用指南", contact: "联系我们", title: "联系我们", intro: "使用 woori.today 时有疑问或建议吗？", lead: "欢迎咨询以下事项：", types: ["一般问题与请求", "合作咨询", "广告咨询与提案", "错误反馈"], note: "我们会查看您的留言，并在可能的范围内回复。", close: "关闭" },
} as const;

export default function GlobalFooter() {
  const pathname = usePathname() || "/ko";
  const locale = getLocaleFromPathname(pathname);
  const text = copy[locale];
  const [open, setOpen] = useState(false);
  return <>
    <footer className="mt-auto border-t border-zinc-200 bg-white px-4 py-6 text-sm sm:px-6">
      <nav aria-label={text.title} className="mx-auto flex max-w-screen-2xl flex-wrap items-center justify-center gap-x-4 gap-y-2 text-zinc-600">
        <Link href="/privacy" className="min-h-11 inline-flex items-center hover:text-blue-700">{text.privacy}</Link><span aria-hidden="true">|</span>
        <Link href="/terms" className="min-h-11 inline-flex items-center hover:text-blue-700">{text.terms}</Link><span aria-hidden="true">|</span>
        <button type="button" onClick={() => setOpen(true)} className="min-h-11 px-2 hover:text-blue-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-600">{text.contact}</button>
      </nav>
    </footer>
    {open && <DialogShell title={text.title} description={text.intro} onClose={() => setOpen(false)}>
      <p className="text-sm font-medium text-zinc-800">{text.lead}</p>
      <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-zinc-700">{text.types.map((item) => <li key={item}>{item}</li>)}</ul>
      <p className="mt-4 text-sm text-zinc-700">Email: <a className="font-medium text-blue-700 underline underline-offset-2" href="mailto:krdd125@gmail.com">krdd125@gmail.com</a></p>
      <p className="mt-4 text-sm leading-6 text-zinc-600">{text.note}</p>
    </DialogShell>}
  </>;
}
