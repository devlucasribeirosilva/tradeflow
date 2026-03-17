# TradeFlow

Plataforma B2B de Ordens de Compra com processamento distribuído, segurança JWT, multi-tenancy e resiliência.

## Stack

- **Backend:** Java 17 + Spring Boot 3.5 + Spring Security 6
- **Mensageria:** Apache Kafka (Outbox Pattern)
- **Banco de dados:** PostgreSQL 16 + Flyway migrations
- **Cache / Lock:** Redis 7 (cache de saldo + distributed lock com Lua Script)
- **Frontend:** Next.js 14 (App Router) — BFF com httpOnly cookies
- **Testes de carga:** k6
- **Infra:** Docker + Kubernetes (em progresso)

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
- Dashboard com criação de ordens

---

## Segurança

- **Autenticação:** JWT (jjwt 0.12.6) com access token (1h) e refresh token (7 dias)
- **Refresh Token Rotation:** token antigo invalidado a cada uso; reuso detectado como ataque
- **Armazenamento:** refresh token no Redis com TTL; access token em httpOnly cookie no BFF
- **Autorização:** `@PreAuthorize` com roles — BUYER cria ordens, SUPPLIER/ADMIN visualiza
- **Multi-tenancy:** Hibernate Filter injeta `WHERE tenant_id = :tenantId` automaticamente em todas as queries de leitura via AOP

---

## Padrões e Decisões Técnicas

| Padrão | Onde | Motivo |
|---|---|---|
| Outbox Pattern | order-service | Garantia de entrega de eventos (dual-write safety) |
| Idempotency Key | order-service | Prevenção de ordens duplicadas |
| Pessimistic Lock | financial-service | Consistência no débito de saldo |
| Distributed Lock (Lua) | financial-service | Eliminar race conditions em alta concorrência |
| httpOnly Cookie | Next.js BFF | Proteção do JWT contra XSS |
| Schema Isolation | financial-service | Separação de dados financeiros do domínio de ordens |
| Hibernate Filter | order-service | Multi-tenancy transparente sem alterar queries |

---

## Como rodar localmente

### Pré-requisitos
- Java 17
- Node.js 18+
- Docker Desktop

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
- order-service: http://localhost:8080
- financial-service: http://localhost:8081

---

## Variáveis de ambiente

### order-service (`application.properties`)
```properties
jwt.secret=TradeFlowSuperSecretKeyForJWTSigningMustBe256BitsLongAtLeast
jwt.expiration=3600000
jwt.refresh-expiration=604800000
spring.datasource.url=jdbc:postgresql://localhost:5432/tradeflow
spring.kafka.bootstrap-servers=localhost:9092
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

### Teste de autenticação (Insomnia / curl)
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

### Teste de carga (k6)
```bash
k6 run k6/race-condition-test.js
```

Resultado esperado com distributed lock: saldo debitado com precisão, zero lost updates.

---

## Estrutura do projeto

```
tradeflow/
├── backend/
│   ├── order-service/
│   │   └── src/main/java/com/tradeflow/order/
│   │       ├── config/          # KafkaConfig
│   │       ├── domain/
│   │       │   ├── entity/      # Order, OrderItem, Buyer, Supplier, OutboxEvent
│   │       │   ├── enums/       # OrderStatus (FSM)
│   │       │   ├── exception/   # OrderNotFoundException, BusinessException
│   │       │   └── valueobject/ # Money
│   │       ├── messaging/       # OrderEventPublisher, OutboxPoller
│   │       ├── repository/      # OrderRepository, BuyerRepository, ...
│   │       ├── security/        # JwtService, JwtAuthFilter, SecurityConfig, TenantFilterAspect
│   │       ├── usecase/         # CreateOrderUseCase, GetOrderUseCase
│   │       └── web/
│   │           ├── controller/  # OrderController, AuthController
│   │           ├── dto/         # CreateOrderRequest, OrderResponse
│   │           └── handler/     # GlobalExceptionHandler (RFC 7807)
│   └── financial-service/
│       └── src/main/java/com/tradeflow/financial/
│           ├── domain/entity/   # Account, BalanceTransaction
│           ├── messaging/       # OrderEventConsumer, OrderEventPublisher
│           ├── repository/      # AccountRepository, BalanceTransactionRepository
│           └── service/         # BalanceService, DistributedLockService
├── frontend/
│   └── app/
│       ├── api/
│       │   ├── auth/login/      # Route handler — login + set httpOnly cookie
│       │   ├── auth/logout/     # Route handler — clear cookies
│       │   └── orders/          # Route handler — proxy para order-service
│       ├── login/               # Página de login
│       └── dashboard/           # Dashboard de ordens
├── k6/
│   └── race-condition-test.js   # Teste de carga e race condition
├── docker-compose.yml
└── README.md
```

---

## Progresso do desenvolvimento (21 dias)

| Dia | Tema | Status |
|---|---|---|
| 1 | Monorepo, docker-compose, C4 diagram | ✅ |
| 2 | JPA domain modeling, Flyway, Value Objects | ✅ |
| 3 | Repositories, Use Cases, Idempotência | ✅ |
| 4 | REST API com RFC 7807 | ✅ |
| 5 | Kafka + Outbox Pattern | ✅ |
| 6 | Financial Service + Redis Cache | ✅ |
| 7 | Revisão Week 1 | ✅ |
| 8 | JWT + Spring Security 6 + Refresh Token Rotation | ✅ |
| 9 | RBAC com @PreAuthorize + Multi-tenancy Hibernate Filter | ✅ |
| 10 | Diagnóstico de Race Condition com k6 | ✅ |
| 11 | Distributed Lock com Redis Lua Script | ✅ |
| 12 | Next.js BFF + httpOnly Cookies | ✅ |
| 13 | Dashboard completo | 🔄 |
| 14 | Observabilidade (Micrometer + Actuator) | 🔜 |
| 15 | Resilience4j Circuit Breaker | 🔜 |
| 16 | Docker multi-stage build | 🔜 |
| 17 | CI/CD com GitHub Actions | 🔜 |
| 18 | Kubernetes manifests | 🔜 |
| 19 | Helm Chart | 🔜 |
| 20 | Revisão final e testes E2E | 🔜 |
| 21 | Deploy e documentação final | 🔜 |