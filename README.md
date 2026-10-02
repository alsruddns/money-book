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
- **검색과 분석:** 거래 검색 및 페이지 조회, 월간·연간 리포트, 카테고리·계좌 통계, 전월 비교
- **월 결산:** 결산 snapshot, 결산 취소, 결산된 기간의 거래 변경 제한
- **데이터 관리:** CSV/XLSX 내보내기, JSON 백업 검증·미리보기·새 가계부로 복원, 가계부 주 시작 요일 설정
- **감사 및 운영:** 가계부 활동내역, 서비스 전역 System Admin API, 관리자 운영 조회 및 감사로그

### Frontend

- **인증과 공유:** 로그인·회원가입, 인증 상태 복원 및 토큰 재발급, 가계부 목록·생성, 초대·멤버·권한 관리
- **원장:** 카테고리, 계좌/결제수단, 수입·지출 거래 및 거래 검색
- **가계부 기능:** 이체, 정기 거래, 대시보드, 캘린더, 예산, 월간·연간 리포트, 월 결산
- **설정과 데이터:** 주 시작 요일 설정, CSV/XLSX 내보내기, JSON 백업·검증·미리보기·복원
- **레이아웃:** 데스크톱 사이드바, 모바일 내비게이션, 광고 슬롯 대응 구조
- **System Admin:** 관리자 대시보드, 사용자 상태/역할 관리, 가계부 운영 조회, 전체 활동내역, SUPER_ADMIN 감사로그

> Backend에 구현된 기능이라도 Frontend 화면이 없으면 Frontend 완료 항목으로 간주하지 않습니다. Admin 운영 화면은 서비스 전역 역할 기반으로 `/admin/**`에서 제공하며, 일반 MoneyBook 권한과 분리됩니다.

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
- Hosting 환경에서는 `BACKEND_API_URL`을 서버 전용 Environment Variable로 설정합니다. 운영 URL은 코드나 `NEXT_PUBLIC_` 변수에 넣지 않습니다.
- Backend는 기본 `local` profile을 사용합니다. 운영에서는 `SPRING_PROFILES_ACTIVE=prod`를 설정하고, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`을 Backend 실행 환경에 주입합니다. Local에서도 `DB_PASSWORD`와 `JWT_SECRET`은 환경변수로 설정해야 합니다.
- Backend의 공통 context path는 `/api`입니다. JWT secret과 DB 비밀번호는 저장소에 기록하지 않습니다.

## Current Focus

**현재 단계: Account Management Backend**

다음 작업 순서:

1. Account Management Backend
2. Account Management Frontend
3. Owner 이전 및 계정 탈퇴
4. Session 및 Refresh Token 관리
5. Rate limiting 및 보안 강화
6. UX 일관성 개선
7. 모니터링
8. CI/CD
9. 운영 배포

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
- [x] 한국 공휴일 조회와 DB 캐시

### Phase 5 — 검색과 분석

- [x] 거래 검색 및 페이지 조회
- [x] 월간·연간 리포트
- [x] 카테고리·계좌 통계와 전월 비교
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

### Phase 8 — 계정 관리

- [ ] 프로필 및 닉네임 변경
- [ ] 비밀번호 변경
- [ ] OWNER 이전
- [ ] 계정 탈퇴
- [ ] 개인정보 삭제 정책

### Phase 9 — 보안

- [ ] 세션 및 Refresh Token 관리
- [ ] 전체 기기 로그아웃
- [ ] Rate limiting
- [ ] 보안 강화

### Phase 10 — UX와 제품 기능

- [ ] Frontend UX 일관성 개선
- [ ] Dashboard 개선
- [ ] 앱 내 알림

### Phase 11 — 운영

- [ ] Health 및 metrics 운영 구성
- [ ] 모니터링
- [ ] CI
- [ ] CD
- [ ] 프로덕션 배포

## 배포 상태

아직 프로덕션 배포 전이며, 현재는 웹/PWA 기반 기능을 개발하고 있습니다. 구체적인 배포 플랫폼과 운영 환경은 추후 결정합니다.

## 개발 규칙

코드 구조, 구현 컨벤션, 보안 및 Git 규칙은 [`AGENTS.md`](AGENTS.md)를 참고하세요.
