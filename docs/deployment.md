# Production Docker 운영 안내

이 문서는 단일 VM에서 Docker Compose로 MoneyBook을 실행하기 위한 준비 문서입니다. 저장소에는 컨테이너 구성만 추가했으며 실제 서버 배포, DNS 변경, 운영 DB 변경, 인증서 발급은 수행하지 않았습니다.

## 구성

```text
Internet
   │ 80 / 443
 Caddy ─── Frontend (Next.js :3000)
   │
   └────── Backend (Spring Boot :8080) ─── PostgreSQL 16 (:5432)
```

- 외부 공개 포트는 Caddy의 `80/tcp`, `443/tcp`, `443/udp`뿐입니다.
- `/api/**`는 Caddy가 경로를 바꾸지 않고 Backend로 전달합니다. Backend context path가 `/api`이므로 prefix를 유지합니다.
- 나머지 경로는 Next.js로 전달합니다. 프로덕션 경로에서는 Next.js rewrite와 Caddy API proxy를 중복 적용하지 않습니다.
- Frontend와 Backend는 호스트 port를 publish하지 않습니다. PostgreSQL도 호스트 port를 publish하지 않습니다.
- Caddy만 Frontend용 `edge`와 Backend용 `proxy` 네트워크 모두에 연결됩니다. Backend는 PostgreSQL 전용 `database` 네트워크에도 연결됩니다.
- Caddy의 자동 HTTPS는 실제 공개 도메인이 설정된 뒤 동작합니다. 로컬 `localhost` 검증은 Caddy 내부 개발 인증서를 사용하며 공개 인증기관 발급을 요청하지 않습니다.

## 사전 준비

- Docker Engine과 Docker Compose plugin
- Linux VM, 공개 IP, 방화벽에서 허용된 TCP 80/443 및 UDP 443
- 실제 도메인과 해당 도메인의 DNS A/AAAA 레코드
- 32바이트 이상의 암호학적으로 안전한 JWT secret

도메인과 DNS는 이번 저장소 작업에서 설정하지 않습니다.

## 환경 설정

```sh
cp .env.production.example .env.production
```

`.env.production`에서 도메인, 데이터베이스 값, JWT secret을 실제 배포 값으로 바꿉니다. 이 파일은 Git에 추가하지 않습니다. `POSTGRES_*`와 `DB_*`는 같은 DB 이름/계정/비밀번호를 가리키도록 설정하고, `DB_URL`은 `jdbc:postgresql://postgres:5432/<DB명>` 형식이어야 합니다. `JWT_SECRET`에는 최소 32바이트의 무작위 값을 사용합니다. 예시 값은 사용할 수 있는 secret이 아닙니다.

`SITE_DOMAIN`은 Caddy의 site address이고 `SITE_URL`은 `https://<실제 도메인>`이어야 합니다. `SITE_URL`, 검색엔진 verification 값, `BACKEND_API_URL`은 Next.js 이미지 build 단계에서 사용됩니다. Metadata, canonical, sitemap은 build 시점 값이므로 도메인을 바꿀 때 Frontend 이미지를 다시 빌드해야 합니다. `BACKEND_API_URL`은 Next 설정의 rewrite 검증/생성에 필요해 `http://backend:8080`으로 build되지만, 실제 배포 ingress의 `/api/**`는 Caddy가 직접 처리합니다.

## 시작 및 상태 확인

```sh
docker compose --env-file .env.production -f compose.prod.yml config
docker compose --env-file .env.production -f compose.prod.yml up -d --build
docker compose --env-file .env.production -f compose.prod.yml ps
docker compose --env-file .env.production -f compose.prod.yml logs --tail=100
```

PostgreSQL healthcheck가 통과한 뒤 Backend가 시작하고, Backend readiness 확인 후 Caddy가 트래픽을 받습니다. Frontend는 Backend와 독립적으로 시작합니다. Backend readiness probe는 `/api/actuator/health/readiness`를 사용하며 인증 없이 상태만 공개합니다. Prometheus 및 기타 actuator 경로는 공개 proxy에서 별도 route로 열지 않습니다.

HTTP smoke 확인은 DNS/도메인을 준비한 뒤 실행합니다.

```sh
curl -I https://your-domain.example/
curl -fsS https://your-domain.example/robots.txt
curl -fsS https://your-domain.example/sitemap.xml
curl -fsS https://your-domain.example/api/health
```

로컬에서는 `.env.production`의 `SITE_DOMAIN=localhost`, `SITE_URL=https://localhost`를 사용해 내부 인증서 동작을 확인할 수 있습니다. `curl -k`는 로컬 Caddy CA를 신뢰하도록 설정하지 않은 검증 환경에서만 사용합니다.

## 종료 및 업데이트

```sh
docker compose --env-file .env.production -f compose.prod.yml down
docker compose --env-file .env.production -f compose.prod.yml up -d --build
```

`down`에 `-v`를 추가하면 PostgreSQL 데이터 볼륨을 삭제할 수 있으므로 운영 중 사용하지 않습니다. 지금은 수동 업데이트입니다. 배포 전 실행 중인 이미지와 Git revision을 기록하고, 검증한 revision에서 이미지를 빌드합니다. Push/Pull Request 검증 CI는 [GitHub Actions 안내](ci.md)에 설명되어 있으며, CD/자동 배포는 아직 구현되지 않았습니다.

이전 애플리케이션 revision으로 되돌리는 경우에도 이미 적용된 Flyway migration은 자동으로 되돌아가지 않습니다. migration이 포함된 배포는 이전 이미지와의 schema 호환성을 먼저 검토하고, 필요하면 백업 복원 계획을 세웁니다. Flyway 변경은 forward-only 원칙을 따릅니다.

## 데이터 백업 및 복원

Application의 JSON backup과 PostgreSQL 전체 DB 백업은 별개입니다. 운영 전에는 `pg_dump` 백업을 별도 저장소로 내보내고, 초기 운영은 매일 수행하며 7~14일 보관하는 정책을 권장합니다. 이 문서는 자동 스케줄러를 설치하지 않습니다.

Custom format 백업:

```sh
docker compose --env-file .env.production -f compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' \
  > moneybook-$(date +%F).dump
```

복원은 서비스 중단 후, 대상 DB 내용을 덮어쓸 수 있음을 확인하고 수행합니다.

```sh
cat moneybook-YYYY-MM-DD.dump | \
  docker compose --env-file .env.production -f compose.prod.yml exec -T postgres \
  sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists'
```

백업 파일은 VM과 분리된 안전한 위치에도 보관하고, 정기적으로 복원 검증을 수행합니다. DB 비밀번호를 command line 인자로 직접 전달하거나 저장소에 백업 파일을 추가하지 않습니다.

## 네트워크, Client IP 및 로그

Caddy는 들어온 `Forwarded` 헤더를 제거하고 실제 연결 주소에서 `X-Forwarded-For`를 다시 설정합니다. Backend는 Caddy만 연결된 proxy 네트워크 안에 있고 production profile에서 Spring forwarded header 처리를 활성화합니다. 따라서 rate limiter의 기존 `request.getRemoteAddr()`는 신뢰된 Caddy가 전달한 Client IP를 사용합니다. Backend port를 host에 공개하거나 Backend에 다른 untrusted proxy/container를 연결하지 마세요.

컨테이너는 stdout/stderr로 로그를 남기며 Compose의 `json-file` 로그는 파일당 10 MB, 3개까지 보관합니다. 상태 확인 및 tail:

```sh
docker compose --env-file .env.production -f compose.prod.yml logs -f backend
```

Backend JVM 메모리는 고정 Xmx를 지정하지 않습니다. VM 메모리에 맞게 `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=65.0` 등으로 제한을 검토하고, 운영 중 실제 heap 및 container memory 사용량을 확인하세요.

## 문제 해결

- Backend가 healthy가 되지 않으면 `docker compose ... logs backend postgres`에서 DB 접속, Flyway, schema validation 오류를 확인합니다.
- Caddy가 인증서를 얻지 못하면 domain DNS, 80/443 방화벽, `SITE_DOMAIN`, `SITE_URL`을 확인합니다. localhost는 공개 인증서를 받지 않습니다.
- 브라우저 API가 404라면 reverse proxy가 `/api` prefix를 제거하지 않는지 확인합니다.
- canonical/sitemap이 잘못된 origin을 표시하면 `SITE_URL`을 고친 뒤 Frontend 이미지를 다시 빌드합니다.
- 볼륨을 삭제하기 전에 유효한 외부 DB backup이 있는지 확인합니다.
