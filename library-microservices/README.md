# Library Management System — Microservices

commands to run(notes) - 
docker compose stop member-service
docker compose start member-service
docker compose down
docker compose up --build

A small, deliberately-scoped microservices project built to demonstrate the full stack: **Java, Spring Boot, Spring Data JPA, Spring Security (JWT), Microservices patterns (service discovery, API gateway, FeignClient, circuit breaker), PostgreSQL, Docker, and Kubernetes.**

It's not trying to be a big system — it's trying to be a *correct, explainable* one. Every design decision below is something you should be ready to justify in an interview.

---

## Architecture

```
                        ┌─────────────────┐
                        │   API Gateway    │  :8080
                        │ (Spring Cloud    │  single entry point, routes by path
                        │    Gateway)      │
                        └────────┬─────────┘
                                 │
              ┌──────────────────┼──────────────────┐
              │                  │                  │
      ┌───────▼───────┐  ┌───────▼────────┐  ┌───────▼────────┐
      │  book-service  │  │ member-service │  │  loan-service   │
      │     :8081      │  │     :8082      │  │     :8083       │
      │  (catalog +    │  │ (auth + JWT +  │  │ (orchestrates   │
      │   availability)│  │  profiles)     │  │  borrow/return  │
      └───────┬────────┘  └───────┬────────┘  │  via Feign)     │
              │                   │           └───────┬─────────┘
              │                   │                   │  Feign calls
              │                   │◄──────────────────┤  (book + member)
              │                                        │
      ┌───────▼────────────────────▼────────────────────▼───────┐
      │                      PostgreSQL                          │
      │        book_db      │    member_db    │    loan_db       │
      │   (database-per-service — no cross-service joins)        │
      └────────────────────────────────────────────────────────  ┘

      All four services register with:
      ┌─────────────────────┐
      │  discovery-server    │  :8761  (Netflix Eureka)
      │  service registry     │
      └─────────────────────┘
```

**Why this shape:**
- **API Gateway** — single entry point; hides internal topology from clients, centralizes routing.
- **Eureka discovery** — services find each other by name (`lb://book-service`), not hardcoded host:port. Enables horizontal scaling (multiple replicas) with automatic load balancing.
- **FeignClient** — loan-service talks to book-service and member-service declaratively, backed by a Resilience4j circuit breaker + fallback, so a downstream outage degrades gracefully instead of cascading.
- **Database per service** — each service owns its schema; loan-service stores `bookId`/`memberId` as plain values (not JPA relationships across service boundaries) and resolves them live via Feign. This is the trade-off you make for service independence: no cross-service SQL joins, so you accept eventual consistency and a bit more network chatter in exchange for services that can be deployed, scaled, and changed independently.

---

## Tech stack mapping (what to say in the interview)

| Area | Where it shows up |
|---|---|
| **Java Core / Streams** | DTO mapping via `.stream().map().toList()`, Optional chains in service layers |
| **Spring Boot** | All 4 services — auto-config, `@ConfigurationProperties`-style env binding, actuator health checks |
| **Spring Data JPA** | `Book`, `Member`, `Loan` entities; `@Version` optimistic locking on `Book`; sequence generators (not IDENTITY, so batching works); `open-in-view: false`; JPQL query in `BookRepository`; paginated loan history |
| **Spring Security + JWT** | `member-service`: `SecurityConfig`, `JwtAuthenticationFilter`, `JwtUtil`, BCrypt password hashing, stateless sessions |
| **Microservices patterns** | Eureka discovery, API Gateway, **FeignClient** with fallback factories, Resilience4j circuit breaker + timeouts, database-per-service |
| **PostgreSQL** | 3 logical databases, sequence-based IDs, optimistic locking via `@Version` |
| **Docker** | Multi-stage Dockerfile per service (Maven build stage → slim JRE runtime stage), `docker-compose.yml` orchestrating all 6 containers |
| **Kubernetes** | Namespace, ConfigMaps/Secrets, Postgres as a `StatefulSet` (stable storage), services as `Deployment`s with 2 replicas, readiness/liveness probes, resource requests/limits, `LoadBalancer` Service for the gateway |

---

## Talking points for common interview questions

**"Walk me through what happens when a member borrows a book."**
1. Client calls `POST /api/loans/borrow` on the API Gateway.
2. Gateway routes to a loan-service instance (via Eureka).
3. `LoanService.borrowBook()`:
   - Calls `member-service` via `MemberClient` (Feign) to confirm the member exists.
   - Calls `book-service` via `BookClient` (Feign) to atomically reserve a copy — `Book.availableCopies` is protected by `@Version` optimistic locking, so if two members race for the last copy, one gets an `OptimisticLockException` and retries/fails cleanly instead of over-lending.
   - Persists a new `Loan` row locally.
4. If book-service is down or slow, the circuit breaker trips after the configured failure threshold and Feign calls short-circuit to the fallback, which raises a clear `503 Service Unavailable` instead of hanging every request.

**"How would you extend this to a real Saga if borrowing had more steps?"**
Right now the reserve-then-record sequence has nothing to compensate if the last step fails (the DB write can't practically fail after a successful reserve). If we added more steps after the reservation (e.g., charging a late-fee deposit), and one of those failed, we'd need a compensating call back to `bookClient.releaseCopy()` to undo the reservation — that's the orchestration-style Saga pattern, illustrated here at a small scale.

**"Why is `open-in-view: false`?"**
Forces us to fetch what we need inside the `@Transactional` service method rather than lazily loading in the controller/view layer — avoids N+1 surprises and doesn't hold a DB connection open longer than necessary.

**"Why JWT here instead of sessions?"**
Stateless — any instance of member-service (or any other service validating the token) can authenticate a request without a shared session store, which fits horizontal scaling / multiple replicas cleanly. Trade-off: revocation before expiry isn't possible without adding a denylist — the code comments call this out explicitly.

---

## Running locally with Docker Compose

```bash
cd library-microservices
docker compose up --build
```

This starts, in order: Postgres (with 3 databases auto-created) → discovery-server → book/member/loan-service → api-gateway.

Wait ~60-90s for everything to register with Eureka (check http://localhost:8761), then hit the gateway at `http://localhost:8080`.

Import `postman/Library-Management.postman_collection.json` into Postman — it has Register → Login → Create Book → Borrow → Return pre-built, with a `{{token}}` variable you paste the JWT into after login.

## Running on Kubernetes

```bash
# Build images locally first (or push to a registry and update the image: fields in k8s/*.yaml)
docker build -t library/discovery-server:latest ./discovery-server
docker build -t library/book-service:latest ./book-service
docker build -t library/member-service:latest ./member-service
docker build -t library/loan-service:latest ./loan-service
docker build -t library/api-gateway:latest ./api-gateway

kubectl apply -f k8s/
kubectl get pods -n library-system -w
```

On minikube/kind (no real LoadBalancer), reach the gateway with:
```bash
kubectl port-forward -n library-system svc/api-gateway 8080:80
```

---

## Project structure
```
library-microservices/
├── discovery-server/    # Eureka registry
├── api-gateway/          # Spring Cloud Gateway - single entry point
├── book-service/         # Catalog + availability (JPA, optimistic locking)
├── member-service/       # Auth (JWT), member profiles (Spring Security)
├── loan-service/         # Borrow/return orchestration (FeignClient, Resilience4j)
├── k8s/                  # Kubernetes manifests
├── postman/              # Postman collection for manual testing
├── postgres-init/        # DB bootstrap script (database-per-service)
└── docker-compose.yml
```

## What's deliberately simplified (be upfront about this if asked)
- `ddl-auto: update` instead of Flyway/Liquibase migrations — fine for a demo, not for real prod.
- JWT secret is a plaintext env var default — in real prod this comes from a vault/secret manager (Azure Key Vault, etc.), never source control.
- No distributed tracing (Zipkin/Jaeger) wired up — the natural next addition given the microservices shape.
- No API Gateway-level authentication — JWT is currently validated at member-service; a production gateway would often validate/propagate identity centrally.
