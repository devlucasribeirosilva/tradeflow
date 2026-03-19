# Stage 1 — Build
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# Stage 2 — Runtime
FROM node:20-alpine

WORKDIR /app

RUN addgroup -S tradeflow && adduser -S tradeflow -G tradeflow

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

RUN chown -R tradeflow:tradeflow /app

USER tradeflow

EXPOSE 3000

ENV NODE_ENV=production

ENTRYPOINT ["node", "server.js"]