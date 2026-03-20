# TradeFlow

Plataforma B2B de Ordens de Compra com processamento distribuído, segurança JWT, multi-tenancy e resiliência.

> Projeto desenvolvido em 21 dias como exercício de arquitetura de sistemas distribuídos com Spring Boot, Kafka, Redis, Next.js e Kubernetes.

## Stack

- **Backend:** Java 17 + Spring Boot 3.5 + Spring Security 6
- **Mensageria:** Apache Kafka (Outbox Pattern)
- **Banco de dados:** PostgreSQL 16 + Flyway migrations
- **Cache / Lock:** Redis 7 (cache de saldo + distributed lock com Lua Script)
- **Frontend:** Next.js 14 (App Router) — BFF com httpOnly cookies
- **Observabilidade:** Micrometer + Spring Actuator + Prometheus
- **Resiliência:** Resilience4j Circuit Breaker
- **Testes de carga:** k6
- **Infra:** Docker + Kubernetes + Helm

---

## Arquitetura (C4 — Nível Container)

```
┌─────────────────────────────────────────────────────────────┐
│                      TradeFlow System                       │
│                                                             │
│  ┌──────────────┐   HTTP    ┌─────────────────────────┐    │
│  │  Next.js BFF │──────────▶│      order-service      │    │
│  │  (port 3000) │           │      (port 8080)        │    │
│  └──────────────┘           └────────────┬────────────┘    │
│                                          │ Outbox Pattern  │
│                         ┌────────────────▼──────────────┐  │
│                         │            Kafka              │  │
│                         │  topics: order.created        │  │
│                         │          order.validated      │  │
│                         │          order.settled        │  │
│                         └────────────────┬──────────────┘  │
│                                          │                 │
│                         ┌────────────────▼──────────────┐  │
│                         │      financial-service        │  │
│                         │      (port 8081)              │  │
│                         └───────────────────────────────┘  │
│                                                             │
│  ┌──────────────┐              ┌──────────────────────┐    │
│  │  PostgreSQL  │              │        Redis         │    │
│  │  (port 5432) │              │     (port 6379)      │    │
│  │  schema:     │              │  - JWT refresh tokens│    │
│  │    public    │              │  - balance cache     │    │
│  │    financial │              │  - distributed locks │    │
│  └──────────────┘              └──────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Módulos

### order-service (porta 8080)
Responsável pela criação e gestão do ciclo de vida das ordens de compra.

- Domínio rico com entidades `Order`, `OrderItem`, `Buyer`, `Supplier`
- Value Object `Money` com suporte a múltiplas moedas
- FSM (Finite State Machine) no `OrderStatus` com validação de transições
- Idempotência via `idempotency_key` único por ordem
- Outbox Pattern para publicação confiável de eventos no Kafka
- JWT + Spring Security 6 (STATELESS)
- RBAC com `@PreAuthorize` por role (BUYER, SUPPLIER, ADMIN)
- Multi-tenancy com Hibernate Filter (`tenant_id`)
- Métricas customizadas com Micrometer (`orders.created`, `orders.creation.time`)
- Circuit Breaker no OutboxPoller com Resilience4j
- Flyway migrations (`V1__order_schema.sql`, `V2__outbox_events.sql`)

### financial-service (porta 8081)
Responsável pela gestão de saldos e reservas financeiras.

- Consumidor Kafka do tópico `order.created`
- Reserva de saldo com `SELECT FOR UPDATE` (pessimistic lock)
- Distributed Lock com Redis Lua Script para prevenir race conditions
- Cache de saldo no Redis com TTL de 30 segundos
- Isolamento de schema PostgreSQL (`financial`)
- Flyway migrations próprias

### frontend (porta 3000)
BFF (Backend For Frontend) em Next.js 14.

- JWT armazenado em **httpOnly cookie** (seguro contra XSS)
- API Routes como proxy para o order-service
- Login com seleção de role
- Dashboard com listagem de ordens e status badges

---

## Segurança

- **Autenticação:** JWT (jjwt 0.12.6) com access token (1h) e refresh token (7 dias)
- **Refresh Token Rotation:** token antigo invalidado a cada uso; reuso detectado como ataque
- **Armazenamento:** refresh token no Redis com TTL; access token em httpOnly cookie no BFF
- **Autorização:** `@PreAuthorize` com roles — BUYER cria ordens, SUPPLIER/ADMIN visualiza
- **Multi-tenancy:** Hibernate Filter injeta `WHERE tenant_id = :tenantId` automaticamente via AOP

---

## Padrões e Decisões Técnicas

| Padrão | Onde | Motivo |
|---|---|---|
| Outbox Pattern | order-service | Garantia de entrega de eventos (dual-write safety) |
| Idempotency Key | order-service | Prevenção de ordens duplicadas |
| Pessimistic Lock | financial-service | Consistência no débito de saldo |
| Distributed Lock (Lua) | financial-service | Eliminar race conditions em alta concorrência |
| httpOnly Cookie | Next.js BFF | Proteção do JWT contra XSS |
| Schema Isolation | financial-service | Separação de dados financeiros |
| Hibernate Filter | order-service | Multi-tenancy transparente sem alterar queries |
| Circuit Breaker | order-service | Resiliência na publicação de eventos Kafka |

---

## Como rodar localmente

### Pré-requisitos
- Java 17
- Node.js 20+
- Docker Desktop
- k6 (opcional, para testes de carga)

### Subir infraestrutura
```bash
docker compose up -d
```

Serviços iniciados: PostgreSQL 16, Redis 7, Kafka + Zookeeper

### Rodar order-service
```bash
cd backend/order-service
./mvnw spring-boot:run
```

### Rodar financial-service
```bash
cd backend/financial-service
./mvnw spring-boot:run
```

### Rodar frontend
```bash
cd frontend
npm install
npm run dev
```

### Acessar
- Frontend: http://localhost:3000/login
- order-service API: http://localhost:8080
- financial-service API: http://localhost:8081
- Actuator health: http://localhost:8080/actuator/health
- Prometheus metrics: http://localhost:8080/actuator/prometheus

---

## Deploy com Docker Compose

```bash
docker compose up -d --build
```

Todos os serviços sobem automaticamente com as variáveis de ambiente configuradas.

---

## Deploy com Kubernetes

### Usando Kustomize
```bash
kubectl apply -k k8s/base
```

### Usando Helm
```bash
helm install tradeflow ./helm/tradeflow
```

### Sobrescrevendo valores por ambiente
```bash
helm install tradeflow ./helm/tradeflow \
  --set orderService.replicas=3 \
  --set postgres.password=senhaSegura \
  --set orderService.jwt.secret=novoSecret
```

### Desinstalar
```bash
helm uninstall tradeflow
```

---

## Variáveis de ambiente

### order-service (`application.properties`)
```properties
jwt.secret=TradeFlowSuperSecretKeyForJWTSigningMustBe256BitsLongAtLeast
jwt.expiration=3600000
jwt.refresh-expiration=604800000
spring.datasource.url=jdbc:postgresql://localhost:5432/tradeflow
spring.kafka.bootstrap-servers=localhost:9092
management.endpoints.web.exposure.include=health,info,metrics,prometheus,circuitbreakers
management.health.circuitbreakers.enabled=true
```

### financial-service (`application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tradeflow?currentSchema=financial
spring.flyway.schemas=financial
spring.flyway.default-schema=financial
spring.kafka.bootstrap-servers=localhost:9092
```

### frontend (`.env.local`)
```env
ORDER_SERVICE_URL=http://localhost:8080
NEXT_PUBLIC_APP_NAME=TradeFlow
```

---

## Testes

### Autenticação e criação de ordem
```bash
# Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"lucas","tenantId":"tenant-001","role":"BUYER"}'

# Criar ordem
curl -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "buyerId": "11111111-1111-1111-1111-111111111111",
    "supplierId": "22222222-2222-2222-2222-222222222222",
    "idempotencyKey": "order-001",
    "items": [{"productName":"Notebook","quantity":1,"unitPrice":3500.00,"currency":"BRL"}]
  }'
```

### Teste de carga e race condition
```bash
k6 run k6/race-condition-test.js
```

Resultado esperado com distributed lock: saldo debitado com precisão, zero lost updates.

### Health check
```bash
curl http://localhost:8080/actuator/health
```

---

## Estrutura do projeto

```
tradeflow/
├── backend/
│   ├── order-service/
│   │   ├── Dockerfile
│   │   └── src/main/java/com/tradeflow/order/
│   │       ├── config/          # KafkaConfig
│   │       ├── domain/
│   │       │   ├── entity/      # Order, OrderItem, Buyer, Supplier, OutboxEvent
│   │       │   ├── enums/       # OrderStatus (FSM)
│   │       │   ├── exception/   # OrderNotFoundException, BusinessException
│   │       │   └── valueobject/ # Money
│   │       ├── messaging/       # OrderEventPublisher, OutboxPoller (Circuit Breaker)
│   │       ├── repository/      # OrderRepository, BuyerRepository, ...
│   │       ├── security/        # JwtService, JwtAuthFilter, SecurityConfig, TenantFilterAspect
│   │       ├── usecase/         # CreateOrderUseCase, GetOrderUseCase, ListOrdersUseCase
│   │       └── web/
│   │           ├── controller/  # OrderController, AuthController
│   │           ├── dto/         # CreateOrderRequest, OrderResponse
│   │           └── handler/     # GlobalExceptionHandler (RFC 7807)
│   └── financial-service/
│       ├── Dockerfile
│       └── src/main/java/com/tradeflow/financial/
│           ├── domain/entity/   # Account, BalanceTransaction
│           ├── messaging/       # OrderEventConsumer, OrderEventPublisher
│           ├── repository/      # AccountRepository, BalanceTransactionRepository
│           └── service/         # BalanceService, DistributedLockService
├── frontend/
│   ├── Dockerfile
│   └── app/
│       ├── api/
│       │   ├── auth/login/      # Route handler — login + set httpOnly cookie
│       │   ├── auth/logout/     # Route handler — clear cookies
│       │   └── orders/          # Route handler — proxy para order-service
│       ├── login/               # Página de login
│       └── dashboard/           # Dashboard de ordens
├── k8s/
│   └── base/                    # Kubernetes manifests (Kustomize)
├── helm/
│   └── tradeflow/               # Helm Chart
│       ├── Chart.yaml
│       ├── values.yaml
│       └── templates/
├── k6/
│   └── race-condition-test.js   # Teste de carga e race condition
├── .github/
│   └── workflows/
│       └── ci.yml               # GitHub Actions CI/CD pipeline
├── docker-compose.yml
└── README.md
```

---

## CI/CD

Pipeline GitHub Actions com 4 jobs:

| Job | O que faz |
|---|---|
| `test-order-service` | Roda testes unitários com PostgreSQL e Redis reais |
| `test-financial-service` | Roda testes unitários com PostgreSQL e Redis reais |
| `lint-frontend` | ESLint no código Next.js |
| `build-docker` | Build das 3 imagens Docker (roda após os testes) |

---

## Jornada de desenvolvimento (21 dias)

| Dia | Tema | Destaques |
|---|---|---|
| 1 | Monorepo + Docker Compose + C4 | Estrutura base do projeto |✅
| 2 | JPA Domain Modeling | Order, OrderItem, Money (Value Object), OrderStatus (FSM) |✅
| 3 | Use Cases + Idempotência | CreateOrderUseCase, GetOrderUseCase |✅
| 4 | REST API + RFC 7807 | GlobalExceptionHandler, ProblemDetail |✅
| 5 | Kafka + Outbox Pattern | OutboxPoller com @Scheduled |✅
| 6 | Financial Service + Redis Cache | BalanceService, SELECT FOR UPDATE |✅
| 7 | Revisão Week 1 | — |✅
| 8 | JWT + Spring Security 6 | Refresh Token Rotation com Redis |✅
| 9 | RBAC + Multi-tenancy | @PreAuthorize, Hibernate Filter via AOP |✅
| 10 | Diagnóstico de Race Condition | k6: 535 ordens, apenas 36 processadas |✅
| 11 | Distributed Lock (Lua Script) | k6: 820 ordens, saldo debitado com precisão |✅
| 12 | Next.js BFF + httpOnly Cookies | JWT nunca exposto ao browser |✅
| 13 | Dashboard de Ordens | Listagem com status badges, refresh automático |✅
| 14 | Observabilidade | Micrometer, Actuator, Prometheus, health checks |✅
| 15 | Resilience4j Circuit Breaker | Fallback no OutboxPoller, health indicator |✅
| 16 | Docker multi-stage build | Imagens otimizadas JDK→JRE, node:20-alpine |✅
| 17 | GitHub Actions CI/CD | 4 jobs: test, lint, build Docker |✅
| 18 | Kubernetes Manifests | Deployments, Services, ConfigMap, Secret, Kustomize |✅
| 19 | Helm Chart | Chart parametrizável, helm lint, helm template  |✅
| 20 | Revisão final + Testes E2E | Todos os cenários validados |✅
| 21 | Deploy + Documentação final | README completo, projeto finalizado |✅

---

## Dados de teste

- **Buyer ID:** `11111111-1111-1111-1111-111111111111`
- **Supplier ID:** `22222222-2222-2222-2222-222222222222`
- **Tenant ID:** `tenant-001`
- **Saldo inicial:** 50.000,00 BRL

## Credenciais locais

- **PostgreSQL:** `tradeflow` / `tradeflow123`
- **Redis:** sem autenticação
- **Login BFF:** qualquer username + `tenant-001` + role `BUYER`/`SUPPLIER`/`ADMIN`