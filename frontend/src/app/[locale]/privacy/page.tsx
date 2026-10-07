import Link from "next/link";
import { createLocalizedMetadata } from "@/common/seo/localizedMetadata";
import { isLocale, type Locale } from "@/i18n/config";
import { notFound } from "next/navigation";

const copy: Record<Locale, { title: string; subtitle: string; sections: [string, string][]; account: string; home: string }> = {
  ko: { title: "개인정보 처리 안내", subtitle: "서비스 이용 중 어떤 정보가 사용되는지 설명합니다.", sections: [["처리하는 정보", "계정 이용을 위해 로그인 ID, 닉네임, 인증 제공자와 가입·수정 시각을 처리합니다. 가계부 이용 시 사용자가 등록한 가계부, 멤버 권한, 카테고리, 계좌, 수입·지출, 이체 및 설정 정보를 처리합니다. 로그인 세션의 기기 정보, IP 주소, 생성·최근 사용·만료 시각도 관리합니다."], ["이용 목적", "계정 인증과 관리, 가계부 공유와 권한 적용, 거래·예산·분석 제공, 로그인 세션 관리, 서비스의 보안과 안정적인 운영을 위해 사용합니다."], ["보관과 이용자 관리", "정보는 계정과 서비스 기능 제공에 필요한 동안 이용됩니다. 계정 관리에서 닉네임과 LOCAL 비밀번호를 변경하고 로그인 기기를 확인하거나 세션을 종료할 수 있습니다. 탈퇴 시 가계부 소유권 이전이 먼저 필요할 수 있습니다."], ["안내 범위", "이 페이지는 현재 구현된 기능에 대한 기본 안내입니다. 구체적인 보관 기간, 법적 근거, 처리 위탁·제공, 문의처 등 운영 정책은 서비스 공개 전에 확정해야 합니다."]], account: "계정 관리", home: "MoneyBook 홈" },
  en: { title: "Privacy Notice", subtitle: "How information is used when you use MoneyBook.", sections: [["Information we handle", "We handle your login ID, nickname, authentication provider, and account timestamps. When you use a household book, we process the books, member permissions, categories, accounts, income and expense entries, transfers, and settings you provide. We also manage session device details, IP address, and session timestamps."], ["Why we use it", "Information supports account authentication, shared books and permissions, transaction, budget and analysis features, session management, and service security and operation."], ["Retention and your controls", "Information is used while needed to provide your account and service features. Account settings let you change your nickname or LOCAL password, review devices, and end sessions. You may need to transfer book ownership before closing your account."], ["Scope of this notice", "This is a basic notice describing currently implemented features. Retention periods, legal bases, service providers, disclosures, and contact details must be finalized by the operator before public launch."]], account: "Account settings", home: "MoneyBook home" },
  ja: { title: "プライバシーに関するお知らせ", subtitle: "MoneyBookの利用時に情報をどのように扱うかを説明します。", sections: [["取り扱う情報", "アカウントのログインID、ニックネーム、認証プロバイダー、登録・更新時刻を扱います。家計簿、メンバー権限、カテゴリ、口座、収入・支出、振替、設定など、利用者が登録した情報を処理します。ログインセッションの端末情報、IPアドレス、各種時刻も管理します。"], ["利用目的", "アカウント認証、共有家計簿と権限管理、取引・予算・分析機能、セッション管理、サービスの安全な運営に利用します。"], ["保存と利用者による管理", "情報はアカウントと機能の提供に必要な間利用します。アカウント設定からニックネームやLOCALパスワードを変更し、端末の確認やセッション終了ができます。退会前に家計簿の所有権移転が必要な場合があります。"], ["この案内について", "このページは現在実装されている機能の基本案内です。保存期間、法的根拠、委託・提供先、問い合わせ先などの運用方針は公開前に確定する必要があります。"]], account: "アカウント設定", home: "MoneyBook ホーム" },
  zh: { title: "隐私说明", subtitle: "说明使用 MoneyBook 时信息的处理方式。", sections: [["处理的信息", "我们处理登录名、昵称、认证提供方以及账户创建和修改时间。使用账本时，我们会处理您创建的账本、成员权限、分类、账户、收支、转账和设置。我们也会管理登录设备信息、IP 地址及会话时间。"], ["使用目的", "这些信息用于账户认证、共享账本及权限管理、交易预算和分析功能、会话管理以及服务安全运营。"], ["保存与账户管理", "信息仅在提供账户和服务功能所需期间使用。您可以在账户设置中修改昵称和本地密码、查看设备或结束会话。注销账户前可能需要先转移账本所有权。"], ["说明范围", "本页面是对当前功能的基本说明。正式上线前，运营方还需确定具体保存期限、法律依据、委托处理与信息提供情况以及联系渠道。"]], account: "账户设置", home: "MoneyBook 首页" },
};

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale: raw } = await params; if (!isLocale(raw)) return {}; const text = copy[raw];
  return { ...createLocalizedMetadata(raw, `/${raw}/privacy`, text.title, text.subtitle), robots: { index: true, follow: true } };
}

export default async function PrivacyPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale: raw } = await params; if (!isLocale(raw)) notFound(); const text = copy[raw];
  return <main className="flex-1 bg-zinc-50 px-4 py-10 text-zinc-900 sm:px-6 sm:py-14">
    <article className="mx-auto max-w-3xl rounded-2xl border border-zinc-200 bg-white p-6 sm:p-10">
      <Link href={`/${raw}`} className="text-sm font-medium text-blue-700 hover:underline">{text.home}</Link>
      <h1 className="mt-5 text-3xl font-bold">{text.title}</h1>
      <p className="mt-3 text-sm text-zinc-500">{text.subtitle}</p>
      <div className="mt-8 space-y-7 leading-7 text-zinc-700">
        {text.sections.map(([heading, body]) => <section key={heading}><h2 className="text-lg font-semibold text-zinc-900">{heading}</h2><p className="mt-2">{body}</p></section>)}
      </div>
      {raw === "ko" && <p className="mt-9 border-t border-zinc-200 pt-5 text-sm text-zinc-600">계정이 있다면 <Link href={`/${raw}/account`} className="font-medium text-blue-700 hover:underline">{text.account}</Link>에서 관련 기능을 확인할 수 있습니다.</p>}
    </article>
  </main>;
}
