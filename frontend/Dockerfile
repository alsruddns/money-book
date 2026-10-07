FROM node:20-bookworm-slim AS base
ENV PNPM_HOME="/pnpm"
ENV PATH="$PNPM_HOME:$PATH"
RUN corepack enable && corepack prepare pnpm@9.12.1 --activate

FROM base AS dependencies
WORKDIR /app
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

FROM base AS build
WORKDIR /app
COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

ARG BACKEND_API_URL
ARG SITE_URL=
ARG GOOGLE_SITE_VERIFICATION=
ARG NAVER_SITE_VERIFICATION=
ENV NODE_ENV=production \
    BACKEND_API_URL=${BACKEND_API_URL} \
    SITE_URL=${SITE_URL} \
    GOOGLE_SITE_VERIFICATION=${GOOGLE_SITE_VERIFICATION} \
    NAVER_SITE_VERIFICATION=${NAVER_SITE_VERIFICATION} \
    NEXT_TELEMETRY_DISABLED=1

RUN pnpm build

FROM node:20-bookworm-slim AS runtime
ENV NODE_ENV=production \
    PORT=3000 \
    HOSTNAME=0.0.0.0 \
    NEXT_TELEMETRY_DISABLED=1
WORKDIR /app

RUN groupadd --system --gid 10001 nodejs \
    && useradd --system --uid 10001 --gid nodejs --home-dir /app --shell /usr/sbin/nologin nextjs

COPY --from=build --chown=nextjs:nodejs /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER 10001:10001
EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD node -e "fetch('http://127.0.0.1:3000/').then(r => process.exit(r.ok ? 0 : 1)).catch(() => process.exit(1))"

CMD ["node", "server.js"]
