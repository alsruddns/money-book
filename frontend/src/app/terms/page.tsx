import Link from "next/link";
import { createPublicMetadata } from "@/common/seo/siteMetadata";

export const metadata = createPublicMetadata(
  "/terms",
  "서비스 이용 안내",
  "MoneyBook 계정과 공유 가계부를 안전하게 이용하기 위한 기본 안내입니다.",
);

export default function TermsPage() {
  return <main className="flex-1 bg-zinc-50 px-4 py-10 text-zinc-900 sm:px-6 sm:py-14">
    <article className="mx-auto max-w-3xl rounded-2xl border border-zinc-200 bg-white p-6 sm:p-10">
      <Link href="/" className="text-sm font-medium text-blue-700 hover:underline">MoneyBook 홈</Link>
      <h1 className="mt-5 text-3xl font-bold">서비스 이용 안내</h1>
      <p className="mt-3 text-sm text-zinc-500">MoneyBook의 현재 제공 기능과 기본 이용 원칙을 안내합니다.</p>
      <div className="mt-8 space-y-7 leading-7 text-zinc-700">
        <section><h2 className="text-lg font-semibold text-zinc-900">계정과 가계부 이용</h2><p className="mt-2">이용자는 본인 계정으로 가계부를 만들고 수입·지출, 예산, 계좌와 설정을 관리할 수 있습니다. 계정 정보와 비밀번호를 안전하게 관리하고, 본인이 등록하거나 공유받은 데이터의 정확성을 확인해 주세요.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">공유와 권한</h2><p className="mt-2">가계부 소유자와 관리자는 멤버를 초대하고 권한을 관리할 수 있습니다. 가계부 데이터는 해당 가계부의 멤버와 설정된 권한에 따라 공유되므로, 초대 대상과 권한을 확인해 주세요.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">안전한 이용</h2><p className="mt-2">서비스의 보안이나 다른 이용자의 사용을 방해하거나, 권한 없이 데이터에 접근하려는 행위는 삼가야 합니다. 서비스는 기능 개선과 보안상 필요한 경우 화면과 운영 방식을 변경할 수 있습니다.</p></section>
        <section><h2 className="text-lg font-semibold text-zinc-900">문서의 상태</h2><p className="mt-2">이 페이지는 개발 중인 서비스의 기본 이용 안내입니다. 운영 주체, 서비스 제공 조건, 책임 범위와 시행일 등 최종 이용약관에 필요한 사항은 공개 전에 별도 검토와 확정이 필요합니다.</p></section>
      </div>
    </article>
  </main>;
}
