import Link from "next/link";
import { buildWebApplicationJsonLd, SITE_DESCRIPTION, SITE_NAME } from "@/common/seo/siteMetadata";
import LandingActions, { LandingHeader } from "@/landing/components/LandingActions";

const features = [
  { number: "01", title: "수입과 지출을 한곳에", description: "카테고리와 계좌를 선택해 거래를 기록하고 월별 내역에서 살펴보세요." },
  { number: "02", title: "함께 쓰는 가계부", description: "가족이나 함께 사는 사람을 초대하고 멤버별 조회·추가·수정·삭제 권한을 설정할 수 있습니다." },
  { number: "03", title: "캘린더로 보는 생활비", description: "날짜별 수입과 지출을 확인하고 캘린더에서 거래를 바로 관리합니다." },
  { number: "04", title: "예산과 월간 분석", description: "월 예산과 카테고리별 지출, 최근 흐름과 지출 순위를 확인하세요." },
  { number: "05", title: "반복 거래와 계좌 간 이체", description: "정기적으로 발생하는 거래를 관리하고 계좌 간 이체 내역을 기록합니다." },
  { number: "06", title: "내보내기와 로그인 기기 관리", description: "거래를 파일로 내보내고 로그인 세션을 확인해 사용 중인 기기를 관리합니다." },
];

export default function HomePage() {
  const jsonLd = JSON.stringify(buildWebApplicationJsonLd()).replace(/</g, "\\u003c");

  return <>
    <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: jsonLd }} />
    <LandingHeader />
    <main className="flex-1 text-zinc-900">
      <section className="overflow-hidden bg-gradient-to-br from-blue-50 via-white to-emerald-50">
        <div className="mx-auto grid max-w-7xl items-center gap-12 px-4 py-16 sm:px-6 sm:py-24 lg:grid-cols-[1.1fr_0.9fr] lg:px-8 lg:py-28">
          <div>
            <p className="inline-flex rounded-full border border-blue-200 bg-white/80 px-3 py-1 text-sm font-medium text-blue-800">가족과 함께 쓰는 공유 가계부</p>
            <h1 className="mt-6 max-w-2xl text-4xl font-bold tracking-tight sm:text-5xl lg:text-6xl">우리 집 돈의 흐름을<br className="hidden sm:block" /> 함께 기록하고 살펴보세요</h1>
            <p className="mt-6 max-w-xl text-base leading-7 text-zinc-600 sm:text-lg">{SITE_DESCRIPTION}</p>
            <div className="mt-8"><LandingActions /></div>
            <p className="mt-4 text-sm text-zinc-500">개인 가계부와 공유 가계부를 한 계정에서 관리할 수 있습니다.</p>
          </div>
          <div aria-label="MoneyBook에서 관리할 수 있는 항목" className="rounded-3xl border border-blue-100 bg-white p-5 shadow-xl shadow-blue-900/5 sm:p-8">
            <div className="flex items-center justify-between border-b border-zinc-100 pb-5"><div><p className="text-sm text-zinc-500">이번 달 가계부</p><p className="mt-1 text-xl font-semibold">생활비 흐름</p></div><span className="rounded-full bg-emerald-50 px-3 py-1 text-sm font-medium text-emerald-800">함께 기록 중</span></div>
            <div className="grid grid-cols-2 gap-3 py-5 sm:gap-4"><div className="rounded-2xl bg-blue-50 p-4"><p className="text-sm text-zinc-600">수입</p><p className="mt-2 text-lg font-semibold text-blue-800">거래별 기록</p></div><div className="rounded-2xl bg-rose-50 p-4"><p className="text-sm text-zinc-600">지출</p><p className="mt-2 text-lg font-semibold text-rose-800">카테고리 관리</p></div></div>
            <div className="space-y-3 rounded-2xl bg-zinc-50 p-4 sm:p-5"><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">캘린더</span><span className="text-sm text-zinc-600">날짜별 거래 확인</span></div><div className="h-px bg-zinc-200" /><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">예산과 분석</span><span className="text-sm text-zinc-600">월별 흐름 살펴보기</span></div><div className="h-px bg-zinc-200" /><div className="flex items-center justify-between gap-3"><span className="text-sm font-medium">멤버 권한</span><span className="text-sm text-zinc-600">필요한 범위로 공유</span></div></div>
            <p className="mt-4 text-xs leading-5 text-zinc-500">화면 예시는 기능 안내용이며 실제 사용자 거래 데이터가 아닙니다.</p>
          </div>
        </div>
      </section>

      <section id="features" className="scroll-mt-20 px-4 py-16 sm:px-6 sm:py-20 lg:px-8"><div className="mx-auto max-w-7xl">
        <div className="max-w-2xl"><p className="text-sm font-semibold text-blue-700">필요한 가계부 기능을 한곳에서</p><h2 className="mt-3 text-3xl font-bold tracking-tight sm:text-4xl">기록은 간편하게, 함께 보는 기준은 분명하게</h2><p className="mt-4 leading-7 text-zinc-600">일상적인 수입과 지출부터 공유 권한과 월별 분석까지, 생활비를 정리하는 데 필요한 기능을 제공합니다.</p></div>
        <div className="mt-10 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{features.map((feature) => <article key={feature.number} className="rounded-2xl border border-zinc-200 bg-white p-5 sm:p-6"><span className="text-sm font-semibold text-blue-700">{feature.number}</span><h3 className="mt-4 text-lg font-semibold">{feature.title}</h3><p className="mt-2 leading-6 text-zinc-600">{feature.description}</p></article>)}</div>
      </div></section>

      <section className="bg-zinc-50 px-4 py-16 sm:px-6 sm:py-20 lg:px-8"><div className="mx-auto grid max-w-7xl gap-10 lg:grid-cols-2 lg:items-center">
        <div><p className="text-sm font-semibold text-blue-700">함께 관리하는 가계부</p><h2 className="mt-3 text-3xl font-bold tracking-tight">누구와 무엇을 공유할지 직접 정하세요</h2><p className="mt-4 leading-7 text-zinc-600">가족이나 파트너를 가계부에 초대하고, 멤버별로 조회·추가·수정·삭제 권한을 설정할 수 있습니다. 소유자와 관리자는 멤버와 가계부 설정을 관리합니다.</p></div>
        <div className="grid gap-3 sm:grid-cols-2"><div className="rounded-2xl border bg-white p-5"><p className="font-semibold">필요한 만큼 공유</p><p className="mt-2 text-sm leading-6 text-zinc-600">초대를 수락한 멤버와 같은 가계부를 살펴봅니다.</p></div><div className="rounded-2xl border bg-white p-5"><p className="font-semibold">멤버별 권한 설정</p><p className="mt-2 text-sm leading-6 text-zinc-600">조회와 거래 관리 범위를 역할에 맞게 설정합니다.</p></div></div>
      </div></section>

      <section className="px-4 py-16 sm:px-6 sm:py-20 lg:px-8"><div className="mx-auto max-w-7xl rounded-3xl bg-blue-950 px-6 py-10 text-white sm:px-10 sm:py-14 lg:flex lg:items-center lg:justify-between lg:gap-10">
        <div className="max-w-2xl"><p className="text-sm font-semibold text-blue-200">기록에서 흐름까지</p><h2 className="mt-3 text-3xl font-bold tracking-tight">이번 달 지출을 이해하기 쉽게</h2><p className="mt-4 leading-7 text-blue-100">월 수입과 지출, 최근 추이, 카테고리별 사용액과 예산 현황을 대시보드와 리포트에서 확인할 수 있습니다.</p></div><div className="mt-7 shrink-0 lg:mt-0"><LandingActions light /></div>
      </div></section>
    </main>
    <footer className="border-t border-zinc-200 bg-white px-4 py-8 sm:px-6 lg:px-8"><div className="mx-auto flex max-w-7xl flex-col gap-5 sm:flex-row sm:items-center sm:justify-between"><div><p className="font-semibold">{SITE_NAME}</p><p className="mt-1 text-sm text-zinc-500">함께 기록하는 생활비 관리</p></div><nav aria-label="서비스 정보" className="flex flex-wrap gap-x-5 gap-y-2 text-sm"><Link className="text-zinc-600 hover:text-blue-700" href="/privacy">개인정보 처리 안내</Link><Link className="text-zinc-600 hover:text-blue-700" href="/terms">이용 안내</Link><Link className="text-zinc-600 hover:text-blue-700" href="/login">로그인</Link></nav></div></footer>
  </>;
}
