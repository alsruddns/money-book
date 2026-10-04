# MoneyBook

MoneyBook은 개인, 부부, 가족이 함께 사용하는 공유 가계부 서비스입니다. 웹과 PWA를 중심으로 수입·지출과 계좌를 관리하고, 예산·캘린더·리포트·데이터 백업을 제공합니다. 앱스토어 배포가 아닌 웹 서비스 형태로 개발 중입니다.

## 기술 스택

| 영역 | 주요 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, PostgreSQL, Flyway, Gradle |
| Frontend | Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS, Redux Toolkit, RTK Query, pnpm |

## 프로젝트 구조

```text
money-book/
├── backend/    # Spring Boot API
├── frontend/   # Next.js 웹/PWA
├── docs/       # 프로젝트 문서
├── AGENTS.md   # 개발 및 코드 작업 규칙
└── README.md   # 프로젝트 소개와 진행 현황
```

## 구현 현황

### Backend

- **인증:** 회원가입, BCrypt 비밀번호 처리, LOCAL 로그인, JWT Access/Refresh Token, 토큰 재발급, 현재 사용자 조회
- **가계부와 공유:** 가계부 관리, 초대 및 수락·거절, 멤버 관리, O/C/R/U/D 권한
- **원장:** 카테고리와 계좌/결제수단, 수입·지출 거래, 거래 상세 및 월별 조회
- **이체와 정기 거래:** 계좌 간 이체, 주간·월간 정기 거래, 실제 거래 생성 및 중복 발생 방지
- **예산과 캘린더:** 월·카테고리 예산과 집계, 월간·일별 캘린더, 한국 공휴일 DB 캐시
- **검색과 분석:** 거래 검색 및 페이지 조회, 월간·연간 리포트, 카테고리·계좌 통계, 전월 비교, Dashboard V2 집계, 월간·연간 지출 TOP20 순위
- **월 결산:** 결산 snapshot, 결산 취소, 결산된 기간의 거래 변경 제한
- **데이터 관리:** CSV/XLSX 내보내기, JSON 백업 검증·미리보기·새 가계부로 복원, 가계부 주 시작 요일 설정
- **계정 관리:** 내 계정 조회, 닉네임 변경, LOCAL 비밀번호 변경, OWNER 이전(활성 ACCEPTED 멤버만 대상, 새 OWNER 전체 권한 보장 및 Activity 기록), 회원 탈퇴(LOCAL 비밀번호 재확인, 소유 가계부가 있으면 차단, WITHDRAWN 전환, 닉네임 익명화, 인증정보와 가계부 membership 정리)
- **탈퇴 토큰 차단:** 인증 요청마다 사용자 활성 상태를 확인해 탈퇴한 사용자의 Access Token을 거부하고, Refresh 요청도 WITHDRAWN 상태에서 실패
- **Password Recovery V1:** Backend와 Frontend의 이메일 인증, 보안 질문, 복구코드, SUPER_ADMIN 임시 비밀번호 초기화 및 강제 변경 연동을 완료했습니다. 인증 이메일은 trim/lowercase 정규화하고 계정 간 unique index로 중복을 차단합니다. 공개 복구 API는 인증 토큰이 없거나 만료된 상태에서도 접근할 수 있으며 보호된 계정/관리자 API의 인증 정책은 유지합니다.
- **감사 및 운영:** 가계부 활동내역, 서비스 전역 System Admin API, Admin Operations V2 집계/상세 조회, 사용자 세션 일괄 폐기, 대상 사용자 감사 필터 및 감사로그

- **운영 모니터링:** Actuator Health/Liveness/Readiness, JVM·HTTP·HikariCP 지표, 인증된 Prometheus endpoint

### Frontend

- **인증과 공유:** 로그인·회원가입, 인증 상태 복원 및 Refresh Token rotation, 로그인 세션 조회·개별/전체 로그아웃, 가계부 목록·생성, 초대·멤버·권한 관리
- **원장:** 카테고리, 계좌/결제수단, 수입·지출 거래 및 거래 검색
- **가계부 기능:** 이체, 정기 거래, Dashboard V2, 날짜별 캘린더 거래 추가·수정·삭제, 예산, 월간·연간 리포트, 월간·연간 지출 TOP20 순위, 월 결산
- **설정과 데이터:** 주 시작 요일 설정, CSV/XLSX 내보내기, JSON 백업·검증·미리보기·복원
- **계정 관리:** 내 계정 정보 조회, 닉네임 변경, LOCAL 비밀번호 변경, 가계부 OWNER 이전, 회원 탈퇴
  - OWNER 이전 UI, 탈퇴 전 소유권 이전 안내, LOCAL 비밀번호 재인증, 비밀번호 변경 후 세션 폐기에 따른 재로그인, 탈퇴 성공 후 토큰·인증 상태·API 캐시 정리
- **레이아웃:** 데스크톱 사이드바, 모바일 내비게이션, 광고 슬롯 대응 구조
- **System Admin:** 운영 지표별 Overview, 사용자 상세/최근 활동/세션 조회, 관리자 확인 후 전체 세션 종료, 가계부 운영 지표와 읽기 전용 멤버 목록, 최근 활동, 필터형 활동/감사 로그. 감사 로그 메뉴는 SUPER_ADMIN 전용
- **공개 안내와 SEO:** 공개 Landing Page, 기본 개인정보 처리 안내와 이용 안내, canonical/Open Graph/Twitter metadata, robots.txt, 공개 URL sitemap, WebApplication JSON-LD

> Backend에 구현된 기능이라도 Frontend 화면이 없으면 Frontend 완료 항목으로 간주하지 않습니다. Admin 운영 화면은 서비스 전역 역할 기반으로 `/admin/**`에서 제공하며, 일반 MoneyBook 권한과 분리됩니다.

### Community Board / Feedback Board V1

- Backend와 Frontend 계약 정합화, 작성자 마스킹, 비밀글 권한, 댓글 depth, 공지·카테고리 권한 및 관련 테스트를 구현했습니다.
- **남은 검증:** 로컬 PostgreSQL 자격 증명으로 새 Backend를 기동하지 못해 Flyway/JPA PostgreSQL runtime 및 실제 게시판 HTTP smoke는 완료하지 못했습니다. 이 검증이 끝날 때까지 V1을 완료로 표시하지 않습니다.

### Password Recovery V1

- Backend와 Frontend 구현 및 복구 API HTTP smoke를 완료했습니다.
- **남은 별도 검증:** 실제 PostgreSQL migration/runtime, SMTP 이메일 실제 발송, 전체 E2E는 아직 검증하지 않았습니다. 이 기능을 Production ready로 간주하지 않습니다.
- 인증 이메일 migration 적용 전에는 `lower(trim(verified_email))` 그룹 기준 중복을 확인해야 합니다. 기존 중복이 있으면 migration은 값을 임의 삭제하지 않고 실패하므로 먼저 계정 소유자를 확인해 수동으로 정리해야 합니다.

## 권한 구조

가계부 내부 권한과 서비스 전역 역할은 서로 독립적입니다.

- **가계부:** `OWNER`와 멤버 권한 `O`(관리), `C`(생성), `R`(조회), `U`(수정), `D`(삭제)를 사용합니다.
- **서비스:** `USER`, `SYSTEM_ADMIN`, `SUPER_ADMIN` 역할을 사용합니다.
- **일반 가계부 API:** 서비스 관리자 역할만으로 권한을 우회할 수 없습니다. 가계부의 `OWNER` 또는 멤버 O/C/R/U/D 권한 검사를 계속 적용합니다.
- **운영 API:** 서비스 관리자 기능은 `/api/admin/**`에서 별도로 제공합니다. Admin 권한은 현재 DB의 사용자 역할과 상태를 확인합니다.
- **SUPER_ADMIN 지정:** 일반 회원가입이나 Admin API로 만들 수 없으며 운영 DB에서 명시적으로 지정합니다.

## 로컬 및 프로덕션 환경

- 로컬 Frontend: `http://localhost:3000`
- 로컬 Backend: `http://localhost:8080/api`
- Browser의 API 요청은 항상 `/api/**` same-origin 경로를 사용하며, Next.js rewrite가 Backend로 전달합니다.
- Frontend 환경변수 `BACKEND_API_URL`에는 Backend origin만 지정합니다. 예시와 로컬 기본값은 `frontend/.env.example`을 참고하세요. 개발 모드에서는 값이 없을 때 `http://localhost:8080`을 사용하고, Production에서는 값이 없으면 Next.js가 시작/빌드 단계에서 오류를 냅니다.
- 공개 페이지 SEO URL은 서버 빌드 환경변수 `SITE_URL`로 설정합니다. 실제 도메인이 확정되기 전에는 비워 두며, Production에서 값이 없으면 canonical/사이트맵 주소를 임의 생성하지 않습니다. Google/Naver 사이트 인증 값은 등록 시 `GOOGLE_SITE_VERIFICATION`, `NAVER_SITE_VERIFICATION`으로 설정할 수 있습니다.
- Hosting 환경에서는 `BACKEND_API_URL`을 서버 전용 Environment Variable로 설정합니다. 운영 URL은 코드나 `NEXT_PUBLIC_` 변수에 넣지 않습니다.
- Backend는 기본 `local` profile을 사용합니다. 운영에서는 `SPRING_PROFILES_ACTIVE=prod`를 설정하고, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 Backend 실행 환경에 주입합니다. Local에서도 `DB_PASSWORD`와 `JWT_SECRET`은 환경변수로 설정해야 합니다.
- SMTP 이메일 인증은 `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` 환경변수로 구성합니다. 이메일은 선택 정보이며 인증된 주소만 복구용으로 저장됩니다.
- Backend의 공통 context path는 `/api`입니다. JWT secret과 DB 비밀번호는 저장소에 기록하지 않습니다.

## Current Focus

**현재 단계: Community Board V1 PostgreSQL runtime 및 실제 API smoke 검증**

Password Recovery V1 Backend와 Frontend 연동은 완료했습니다. 게시판 PostgreSQL runtime 검증 및 나머지 별도 validation을 진행하고 있습니다.

## Development Roadmap

### Phase 1 — 프로젝트 기반

- [x] Monorepo 및 Backend 기반
- [x] Frontend 기반
- [x] PostgreSQL 및 Flyway 기반

### Phase 2 — 인증과 공유

- [x] 회원가입, 로그인, 토큰 재발급
- [x] MoneyBook 생성 및 조회
- [x] 초대와 멤버 관리
- [x] 가계부 O/C/R/U/D 권한

### Phase 3 — 원장

- [x] 카테고리 및 계좌 관리
- [x] 수입·지출 거래
- [x] 계좌 간 이체
- [x] 정기 거래 및 실제 거래 생성

### Phase 4 — 예산과 캘린더

- [x] 월·카테고리별 예산
- [x] 월간 및 날짜별 캘린더
- [x] 캘린더 날짜 클릭 후 거래 조회·추가·수정·삭제
- [x] 한국 공휴일 조회와 DB 캐시

### Phase 5 — 검색과 분석

- [x] 거래 검색 및 페이지 조회
- [x] 월간·연간 리포트
- [x] 카테고리·계좌 통계와 전월 비교
- [x] Dashboard V2 Backend 및 월간·연간 지출 TOP20 순위 Backend
- [x] 월 결산 snapshot 및 취소

### Phase 6 — 데이터 관리

- [x] CSV 및 Excel 내보내기
- [x] JSON 백업
- [x] 백업 검증 및 미리보기
- [x] 새 가계부로 복원
- [x] 가계부 설정

### Phase 7 — 감사와 서비스 운영

- [x] MoneyBook Activity Backend
- [x] MoneyBook Activity Frontend
- [x] System Admin Backend
- [x] System Admin Frontend
- [x] Admin Operations V2 Backend 운영 집계·사용자/가계부 상세·세션 종료·감사 필터
- [x] Admin Operations V2 Frontend 운영 대시보드·상세·세션 관리·필터 UX

### Phase 8 — 계정 관리

- [x] 내 계정 조회 및 닉네임 변경 Backend
- [x] LOCAL 비밀번호 변경 Backend
- [x] Account Management Frontend
- [x] OWNER 이전 Backend
- [x] OWNER 이전 Frontend
- [x] 계정 탈퇴 Backend
- [x] 계정 탈퇴 Frontend
- [x] Password Recovery V1 Backend
- [x] Password Recovery V1 Frontend
- [ ] Password Recovery V1 실제 PostgreSQL migration, SMTP 이메일 발송 및 전체 E2E validation
- [ ] 개인정보 삭제 정책

### Phase 9 — 보안

- [x] Session / Refresh Token Management Backend
- [x] Session / Refresh Token Management Frontend
- [x] 전체 기기 로그아웃
- [x] Rate Limiting 및 Security Hardening

### Phase 10 — UX와 제품 기능

- [x] Frontend UX 일관성 개선
- [x] Pre-Release UI/UX Finalization
- [x] Dashboard V2 Frontend 분석 화면
- [x] 월간·연간 지출 TOP20 Frontend
- [ ] 앱 내 알림
- [ ] Community Board / Feedback Board V1 PostgreSQL runtime 및 실제 API smoke 검증
- [x] 공개 Landing Page Frontend
- [x] Technical SEO 기본 설정: Metadata, canonical, Open Graph/Twitter, robots.txt, sitemap.xml, JSON-LD
- [ ] Google Search Console 등록
- [ ] Naver Search Advisor 등록

### Phase 11 — 운영

- [x] Monitoring / Health / Metrics Backend
- [x] Production Infrastructure 및 Dockerization: Multi-stage image, Compose, Caddy, persistence/operations guide
- [x] Backend test stabilization: `./gradlew clean test` passes all 229 tests without skips
- [x] Production profile configuration review: required DB/JWT environment variables, actuator exposure, stateless security, and graceful shutdown
- [x] Local/production backup upload limits aligned to 20 MB file and 21 MB multipart request
- [x] Complete Flyway chain applied to a clean, isolated PostgreSQL 16 database
- [x] Production-profile Backend startup and Hibernate schema validation against the migrated PostgreSQL schema
- [x] Release HTTP smoke validation for authentication, MoneyBook ledger, calendar, dashboard/reports, transfers, budget, closing, sessions, admin, and exports
- [x] Frontend Browser E2E Smoke for signup/login, MoneyBook creation, categories/accounts, transactions, calendar, dashboard, and reports
- [x] Desktop/tablet/mobile responsive route validation at 1440px, 1024px, 768px, 390px, and 375px viewports
- [ ] Production domain 확정 및 DNS 설정
- [x] PostgreSQL nullable-filter query handling fixed for admin activity and audit-log searches
- [ ] Prometheus 서버 및 운영 모니터링 연동
- [x] GitHub Actions CI: Backend test/build, Frontend test/lint/typecheck/build, Docker image 및 Compose 설정 검증
- [ ] CD
- [ ] 프로덕션 배포

## 배포 상태

아직 프로덕션 배포 전이며, 현재는 웹/PWA 기반 기능을 개발하고 있습니다. 구체적인 배포 플랫폼과 운영 환경은 추후 결정합니다.

## 개발 규칙

코드 구조, 구현 컨벤션, 보안 및 Git 규칙은 [`AGENTS.md`](AGENTS.md)를 참고하세요.
