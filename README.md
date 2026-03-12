# TradeFlow

Plataforma B2B de Ordens de Compra com processamento assíncrono distribuído.

## Stack
- **Backend:** Java 17 + Spring Boot 3 + Kafka + PostgreSQL + Redis
- **Frontend:** Next.js 14 (App Router)
- **Infra:** Docker + Kubernetes

## Arquitetura (C4 — Nível Container)
```
┌─────────────────────────────────────────────────────┐
│                   TradeFlow System                  │
│                                                     │
│  ┌──────────────┐        ┌────────────────────┐     │
│  │  Next.js BFF │──────▶│   order-service    │     │
│  │  (port 3000) │  HTTP  │   (port 8080)      │     │
│  └──────────────┘        └────────┬───────────┘     │
│                                   │                 │
│                    ┌──────────────▼──────────┐      │
│                    │         Kafka           │      │
│                    │  order.created          │      │
│                    │  order.validated        │      │
│                    └──────────────┬──────────┘      │
│                                   │                 │
│                    ┌──────────────▼──────────┐      │
│                    │  financial-service      │      │
│                    │  (port 8081)            │      │
│                    └─────────────────────────┘      │
│                                                     │
│  ┌─────────────┐              ┌─────────────┐       │
│  │ PostgreSQL  │              │    Redis    │       │
│  │ (port 5432) │              │ (port 6379) │       │
│  └─────────────┘              └─────────────┘       │
└─────────────────────────────────────────────────────┘
```

## Como rodar localmente
```bash
docker compose up -d
cd backend/order-service && ./mvnw spring-boot:run
```