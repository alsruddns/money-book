# GitHub Actions CI

`.github/workflows/ci.yml`은 다음 branch로 push되거나 해당 branch를 대상으로 Pull Request가 열리거나 갱신될 때 실행됩니다.

- `feature/dev`
- `develop`
- `release`
- `main`

모든 job은 `ubuntu-latest`에서 독립적으로 실행됩니다. 동일 ref의 이전 실행은 새 실행이 시작되면 취소됩니다.

## Jobs

### Backend test and build

- Eclipse Temurin Java 21을 설치합니다.
- Gradle Wrapper와 Gradle dependency cache를 사용합니다.
- `./gradlew --no-daemon clean build`로 테스트와 build를 한 번에 수행합니다. `build` task가 `test`를 포함하므로 별도 test/build 중복 실행은 없습니다.

### Frontend test, lint, typecheck, and build

- Node.js 20, pnpm 9.12.1을 사용합니다.
- pnpm store를 `frontend/pnpm-lock.yaml` 기준으로 cache합니다.
- `pnpm install --frozen-lockfile`, `pnpm test`, `pnpm lint`, `pnpm exec tsc --noEmit`, `pnpm build`를 실행합니다.
- build validation에는 `BACKEND_API_URL=http://localhost:8080`, `SITE_URL=https://example.invalid`를 사용합니다. `.invalid`은 배포용 도메인이 아니며 build 결과를 배포하거나 저장소 artifact로 올리지 않습니다.

### Docker image and Compose validation

- `docker compose -f compose.prod.yml config --quiet`로 Compose interpolation과 service 구성을 확인합니다.
- Docker image build에 BuildKit GitHub Actions cache를 사용합니다. Push에서만 branch cache를 저장하고, Pull Request에서는 기본 branch 및 접근 가능한 branch cache를 읽기만 합니다.
- Caddyfile을 Caddy image의 `caddy validate`로 검사합니다.
- Backend/Frontend image는 `moneybook-backend:ci`, `moneybook-frontend:ci` tag로 로컬 runner에만 build합니다. Registry login/push와 Compose runtime smoke는 실행하지 않습니다.
- PostgreSQL/JWT 값은 job 안에서만 쓰는 명시적 CI dummy 값이며 production secret이 아닙니다.

Workflow permission은 `contents: read`만 요청합니다. 배포 credential, GitHub production secret, SSH key는 사용하지 않습니다. CI green은 코드를 merge하기 전 검증 기준으로 사용합니다.

## Branch protection

GitHub repository settings에서 `main`과 `release`의 Pull Request merge 조건에 이 workflow의 다음 status check들을 필수로 설정하는 것을 권장합니다.

- `Backend test and build`
- `Frontend test, lint, typecheck, and build`
- `Docker image and Compose validation`

`develop`에도 같은 required check를 적용할 수 있습니다. Branch protection 설정은 GitHub UI/API의 repository 설정이며 이 저장소 workflow는 이를 변경하지 않습니다. CD/배포는 구현하지 않았습니다.
