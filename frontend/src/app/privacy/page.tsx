import Link from "next/link";
import { createPublicMetadata } from "@/common/seo/siteMetadata";

export const metadata = createPublicMetadata(
  "/privacy",
  "개인정보 처리 안내",
  "MoneyBook에서 계정, 가계부, 로그인 세션 정보를 사용하는 목적과 이용자의 관리 방법을 안내합니다.",
);

export default function PrivacyPage() {
  return <main className="flex-1 bg-zinc-50 px-4 py-10 text-zinc-900 sm:px-6 sm:py-14">
    <article className="mx-auto max-w-3xl rounded-2xl border border-zinc-200 bg-white p-6 sm:p-10">
      <Link href="/" className="text-sm font-medium text-blue-700 hover:underline">MoneyBook 홈</Link>
      <h1 className="mt-5 text-3xl font-bold">개인정보 처리 안내</h1>
      <p className="mt-3 text-sm text-zinc-500">서비스 이용 중 어떤 정보가 사용되는지 설명합니다.</p>
      <div className="mt-8 space-y-7 leading-7 text-zinc-700">
        <section><h2 className="text-lg font-semibold text-zinc-900">처리하는 정보</h2><p className="mt-2">계정 이용을 위해 로그인 ID, 닉네임, 인증 제공자와 가입·수정 시각을 처리합니다. 가계부 기능을 사용할 때에는 사용자가 등록한 가계부, 멤버 권한, 카테고리, 계좌, 수입·지출 및 이체 정보와 가계부 설정을 처리합니다. 로그인 상태 유지를 위해 로그인 세션의 기기 정보, IP 주소, 생성·최근 사용·만료 시각도 관리합니다.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">이용 목적</h2><p className="mt-2">계정 인증과 관리, 가계부 공유와 권한 적용, 거래·예산·분석 제공, 로그인 세션 관리, 서비스의 보안과 안정적인 운영을 위해 사용합니다.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">보관과 이용자 관리</h2><p className="mt-2">정보는 계정과 서비스 기능 제공에 필요한 동안 이용됩니다. 계정 관리에서 닉네임과 LOCAL 비밀번호를 변경하고 로그인 기기를 확인하거나 세션을 종료할 수 있습니다. 회원 탈퇴는 서비스에 표시된 조건과 절차에 따라 요청할 수 있으며, 가계부 소유권 이전이 먼저 필요할 수 있습니다.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">안내 범위</h2><p className="mt-2">이 페이지는 현재 구현된 기능을 이해하기 위한 기본 안내입니다. 정보의 구체적인 보관 기간, 법적 근거, 처리 위탁·제공, 문의 창구 등 실제 운영 정책은 서비스 공개 전에 운영 주체가 확정해 보완해야 합니다.</p></section>
      </div>
      <p className="mt-9 border-t border-zinc-200 pt-5 text-sm text-zinc-600">계정이 있다면 <Link href="/account" className="font-medium text-blue-700 hover:underline">계정 관리</Link>에서 관련 기능을 확인할 수 있습니다.</p>
    </article>
  </main>;
}
