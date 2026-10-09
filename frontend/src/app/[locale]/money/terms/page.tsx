import Link from "../../../../common/components/MoneyLink";
import GoogleAdSenseScript from "@/common/components/GoogleAdSenseScript";
import { createLocalizedMetadata } from "@/common/seo/localizedMetadata";
import { isLocale, type Locale } from "@/i18n/config";
import { notFound } from "next/navigation";

const copy: Record<Locale, { title: string; subtitle: string; sections: [string, string][]; home: string }> = {
  ko: { title: "서비스 이용 안내", subtitle: "MoneyBook의 현재 제공 기능과 기본 이용 원칙을 안내합니다.", sections: [["계정과 가계부 이용", "이용자는 본인 계정으로 가계부를 만들고 수입·지출, 예산, 계좌와 설정을 관리할 수 있습니다. 계정 정보와 비밀번호를 안전하게 관리하고 등록하거나 공유받은 데이터의 정확성을 확인해 주세요."], ["공유와 권한", "가계부 소유자와 관리자는 멤버를 초대하고 권한을 관리할 수 있습니다. 데이터는 해당 가계부의 멤버와 설정된 권한에 따라 공유됩니다."], ["안전한 이용", "서비스 보안이나 다른 이용자의 사용을 방해하거나 권한 없이 데이터에 접근하려는 행위는 삼가야 합니다. 서비스는 기능 개선과 보안을 위해 화면과 운영 방식을 변경할 수 있습니다."], ["문서의 상태", "이 페이지는 개발 중인 서비스의 기본 안내입니다. 운영 주체, 제공 조건, 책임 범위와 시행일 등 최종 약관 사항은 공개 전에 별도 검토와 확정이 필요합니다."]], home: "MoneyBook 홈" },
  en: { title: "Terms of Use", subtitle: "Basic guidance for using MoneyBook and shared household books.", sections: [["Accounts and books", "Use your own account to create books and manage income, expenses, budgets, accounts, and settings. Keep your account credentials secure and check the accuracy of data you enter or receive through sharing."], ["Sharing and permissions", "Book owners and administrators may invite members and manage their permissions. Book data is shared with its members according to the permissions configured for that book."], ["Use the service safely", "Do not interfere with service security or other users, or access data without permission. The service may change its interface and operation to improve features and security."], ["Status of this document", "This is basic guidance for a service in development. The operator must separately review and finalize the operator identity, service terms, liability scope, and effective date before public launch."]], home: "MoneyBook home" },
  ja: { title: "サービス利用案内", subtitle: "MoneyBookと共有家計簿の基本的な利用方法をご案内します。", sections: [["アカウントと家計簿", "ご自身のアカウントで家計簿を作成し、収入・支出、予算、口座、設定を管理できます。認証情報を安全に保管し、ご自身で入力または共有されたデータの正確性をご確認ください。"], ["共有と権限", "家計簿の所有者と管理者はメンバーを招待し、権限を管理できます。データは家計簿のメンバーと設定された権限に応じて共有されます。"], ["安全な利用", "サービスの安全性や他の利用者の利用を妨げたり、許可なくデータへアクセスしたりしないでください。機能や安全性の改善のため、画面や運用方法を変更することがあります。"], ["この文書について", "このページは開発中サービスの基本案内です。運営者、提供条件、責任範囲、施行日などの最終的な規約内容は公開前に別途確認・確定する必要があります。"]], home: "MoneyBook ホーム" },
  zh: { title: "服务使用说明", subtitle: "MoneyBook 及共享账本的基本使用说明。", sections: [["账户与账本", "您可以使用自己的账户创建账本，并管理收支、预算、账户和设置。请妥善保管账户凭据，并确认您录入或通过共享获得的数据准确无误。"], ["共享与权限", "账本所有者和管理员可以邀请成员并管理权限。账本数据会按照该账本的成员和权限设置进行共享。"], ["安全使用", "请勿妨碍服务安全或其他用户使用，也不得未经授权访问数据。为改进功能和安全性，服务可能调整页面和运营方式。"], ["本文档状态", "本页面是开发中服务的基本说明。正式上线前，运营方还需另行审定运营主体、服务条件、责任范围和生效日期等最终条款。"]], home: "MoneyBook 首页" },
};

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
  const { locale: raw } = await params; if (!isLocale(raw)) return {}; const text = copy[raw];
  return { ...createLocalizedMetadata(raw, `/${raw}/money/terms`, text.title, text.subtitle), robots: { index: true, follow: true } };
}

export default async function TermsPage({ params }: { params: Promise<{ locale: string }> }) {
  const { locale: raw } = await params; if (!isLocale(raw)) notFound(); const text = copy[raw];
  return <><GoogleAdSenseScript /><main className="flex-1 bg-zinc-50 px-4 py-10 text-zinc-900 sm:px-6 sm:py-14">
    <article className="mx-auto max-w-3xl rounded-2xl border border-zinc-200 bg-white p-6 sm:p-10">
      <Link href={`/${raw}`} className="text-sm font-medium text-blue-700 hover:underline">{text.home}</Link>
      <h1 className="mt-5 text-3xl font-bold">{text.title}</h1>
      <p className="mt-3 text-sm text-zinc-500">{text.subtitle}</p>
      <div className="mt-8 space-y-7 leading-7 text-zinc-700">
        {text.sections.map(([heading, body]) => <section key={heading}><h2 className="text-lg font-semibold text-zinc-900">{heading}</h2><p className="mt-2">{body}</p></section>)}
      </div>
    </article>
  </main></>;
}
