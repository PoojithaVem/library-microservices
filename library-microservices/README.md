# Library Management System — Microservices

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

      All five services (gateway + 3 services + themselves) register with:
      ┌─────────────────────┐              ┌──────────────────────┐
      │  discovery-server    │              │    config-server      │  :8888
      │  :8761  (Eureka)     │              │  (Spring Cloud Config) │
      │  "who is alive,      │              │  "what should each     │
      │   and where"         │              │   service's config be" │
      └─────────────────────┘              └──────────────────────┘
                                                       ▲
                                              pulled at startup by
                                          api-gateway, book/member/loan-service
                                          from config-repo/*.yml (git-free,
                                          "native" mode for this demo)

      Async, event-driven side (separate from the sync request/response flow above):

      loan-service              ┌──────────────────┐          notification-service
      (borrow/return   ──publish──▶  Kafka :9092    │──consume──▶     :8084
       succeeds first)          │  topic:            │          (logs + stores an
                                 │  loan-events        │           in-memory notification;
                                 │  (3 partitions,      │           GET /api/notifications
                                 │   keyed by memberId) │           to view them)
                                 └──────────────────┘
      A Kafka outage never blocks a borrow/return — the DB write already
      committed before the event is published; only the notification is delayed.
```

**Why this shape:**
- **API Gateway** — single entry point; hides internal topology from clients, centralizes routing.
- **Eureka discovery** — services find each other by name (`lb://book-service`), not hardcoded host:port. Enables horizontal scaling (multiple replicas) with automatic load balancing.
- **Config Server** — centralizes shared/policy configuration (loan period, JWT expiry, Feign timeouts, circuit-breaker thresholds, gateway routes) in one place instead of duplicated across each service's local `application.yml`. Each service still has local values as a fallback (`optional:configserver:...` — if config-server is down, services still boot). This is deliberately separate from Eureka: **discovery answers "who's alive and where," config-server answers "what should I be configured with."**
- **Kafka (event-driven side)** — loan-service publishes a `loan-events` message *after* the borrow/return already committed to the database, so a Kafka outage never blocks the actual business operation. notification-service consumes it completely decoupled — loan-service has no idea notification-service exists, and never calls it directly. This is the async counterpart to the synchronous Feign calls elsewhere in the system: **Feign for "I need an answer right now" (is this book available), Kafka for "something happened, react whenever you can" (send a notification).**
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
| **Docker** | Multi-stage Dockerfile per service (Maven build stage → slim JRE runtime stage), `docker-compose.yml` orchestrating all 7 containers |
| **Kubernetes** | Namespace, ConfigMaps/Secrets, Postgres as a `StatefulSet` (stable storage), services as `Deployment`s with 2 replicas, readiness/liveness probes, resource requests/limits, `LoadBalancer` Service for the gateway |
| **Spring Cloud Config** | `config-server` module + `config-repo/` folder — centralizes shared config (loan period, JWT expiry, Feign timeouts, circuit-breaker thresholds, gateway routes) across all 4 client services, each with a local fallback |
| **Kafka** | `loan-events` topic (3 partitions, keyed by `memberId` for per-member ordering), loan-service as producer (`KafkaTemplate`, idempotent producer, explicit `NewTopic` bean), notification-service as consumer (`@KafkaListener`, consumer group `notification-service`), running in KRaft mode (no Zookeeper) |

---

## Talking points for common interview questions

**"Walk me through what happens when a member borrows a book."**
1. Client calls `POST /api/loans/borrow` on the API Gateway.
2. Gateway routes to a loan-service instance (via Eureka).
3. `LoanService.borrowBook()`:
   - Calls `member-service` via `MemberClient` (Feign) to confirm the member exists.
   - Calls `book-service` via `BookClient` (Feign) to atomically reserve a copy — `Book.availableCopies` is protected by `@Version` optimistic locking, so if two members race for the last copy, one gets an `OptimisticLockException` and retries/fails cleanly instead of over-lending.
   - Persists a new `Loan` row locally.
   - **Publishes a `BORROWED` event to Kafka's `loan-events` topic** — this happens *after* the DB write, so a Kafka hiccup never blocks the borrow itself.
4. If book-service is down or slow, the circuit breaker trips after the configured failure threshold and Feign calls short-circuit to the fallback, which raises a clear `503 Service Unavailable` instead of hanging every request.
5. Separately and asynchronously, notification-service's `@KafkaListener` picks up the event and logs/records a notification — loan-service has no idea this happened, and the client's response in step 4 already returned before this even runs.

**"Why Kafka here instead of just another Feign call to a notification-service?"**
Because a notification isn't required for the borrow to succeed — it's a side effect, not a dependency. If notification-service were called synchronously via Feign and it was slow or down, it would needlessly block or fail every borrow request for a concern that doesn't matter to the borrow's correctness. Publishing an event instead means loan-service's job ends the moment the message is handed to Kafka; notification-service processes it whenever it's ready, and a notification-service outage doesn't touch loan-service's availability at all.

**"How would you extend this to a real Saga if borrowing had more steps?"**
Right now the reserve-then-record sequence has nothing to compensate if the last step fails (the DB write can't practically fail after a successful reserve). If we added more steps after the reservation (e.g., charging a late-fee deposit), and one of those failed, we'd need a compensating call back to `bookClient.releaseCopy()` to undo the reservation — that's the orchestration-style Saga pattern, illustrated here at a small scale.

**"Why is `open-in-view: false`?"**
Forces us to fetch what we need inside the `@Transactional` service method rather than lazily loading in the controller/view layer — avoids N+1 surprises and doesn't hold a DB connection open longer than necessary.

**"Why JWT here instead of sessions?"**
Stateless — any instance of member-service (or any other service validating the token) can authenticate a request without a shared session store, which fits horizontal scaling / multiple replicas cleanly. Trade-off: revocation before expiry isn't possible without adding a denylist — the code comments call this out explicitly.

**"Why key the Kafka messages by memberId?"**
Kafka only guarantees message ordering *within a single partition*, not across the whole topic. Keying by `memberId` means every event for the same member always hashes to the same partition, so if a member borrows then immediately returns, notification-service is guaranteed to process those two events in the order they happened. Different members' events can land on different partitions and process fully in parallel — you get ordering exactly where it matters and parallelism everywhere else.

---

## Running locally with Docker Compose

```bash
cd library-microservices
docker compose up --build
```

This starts, in order: Postgres + config-server + Kafka (parallel) → discovery-server → book/member/loan-service + notification-service (pulling their config from config-server at boot, loan-service and notification-service also waiting on Kafka) → api-gateway.

First build takes longer than before — Kafka's image alone is a few hundred MB, and there's now a 6th Spring Boot service to compile.

You can confirm config-server is serving the right values before the other services even start by hitting it directly:
```
http://localhost:8888/loan-service/default
```
That returns the merged config loan-service will receive (config-repo/loan-service.yml + config-repo/application.yml).

Wait ~60-90s for everything to register with Eureka (check http://localhost:8761), then hit the gateway at `http://localhost:8080`.

**To verify the Kafka pipeline specifically**: borrow a book (see Postman flow below), then within a few seconds check
```
http://localhost:8080/api/notifications
```
You should see a new entry describing that borrow — proof the event actually traveled loan-service → Kafka → notification-service with no direct call between the two.

Import `postman/Library-Management.postman_collection.json` into Postman — it has Register → Login → Create Book → Borrow → Return → **Get Recent Notifications** pre-built, with a `{{token}}` variable you paste the JWT into after login.

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
├── config-server/        # Spring Cloud Config - centralized configuration
├── config-repo/          # The actual config files config-server serves
├── api-gateway/          # Spring Cloud Gateway - single entry point
├── book-service/         # Catalog + availability (JPA, optimistic locking)
├── member-service/       # Auth (JWT), member profiles (Spring Security)
├── loan-service/         # Borrow/return orchestration (FeignClient, Resilience4j, Kafka producer)
├── notification-service/ # Kafka consumer - reacts to loan events asynchronously
├── k8s/                  # Kubernetes manifests
├── postman/              # Postman collection for manual testing
├── postgres-init/        # DB bootstrap script (database-per-service)
└── docker-compose.yml
```

## What's deliberately simplified (be upfront about this if asked)
- `ddl-auto: update` instead of Flyway/Liquibase migrations — fine for a demo, not for real prod.
- JWT secret is a plaintext env var default — in real prod this comes from a vault/secret manager (Azure Key Vault, etc.), never source control.
- No distributed tracing (Zipkin/Jaeger) wired up — the natural next addition given the microservices shape.
- Config-server uses **native mode** (a local folder) instead of a git-backed repo — simpler for a self-contained demo, but a real production setup would point `spring.cloud.config.server.git.uri` at an actual git repo so config changes are versioned and reviewable like code.
- No API Gateway-level authentication — JWT is currently validated at member-service; a production gateway would often validate/propagate identity centrally.
- loan-service publishes to Kafka as a **direct call after the DB commit**, not via the **Outbox pattern**. In the rare window between "loan saved" and "event published," a crash could lose the event. A real production system would write the event to an outbox table in the *same transaction* as the loan, then have a separate relay (e.g. Debezium CDC) publish it — guaranteeing the DB write and the event can never get out of sync. Worth naming this trade-off unprompted if asked "is this fully reliable?"
- notification-service stores notifications **in-memory**, not in its own database — see the comment in `NotificationService.java`. Fine for demoing the Kafka pipeline; a real version would persist to its own `notification_db` (keeping database-per-service) or push straight to an email/SMS/push provider.
- Kafka runs as a **single broker with replication factor 1** — no fault tolerance if that one broker dies. Real production Kafka runs 3+ brokers so a broker failure doesn't lose data.


GUI tool (easier for repeated browsing, nicer for a demo)

Install a free database client — DBeaver (https://dbeaver.io) or TablePlus are the most popular, or pgAdmin. Connect with these details:

Host:     localhost
Port:     5432
Database: book_db   (or member_db, loan_db)
Username: library_user
Password: library_pass

This gives you a proper table view — click a table, see rows, run queries with autocomplete, no memorizing psql commands.

Quick check right now

Run this one-liner to confirm all three of your databases and their tables exist:

bash
docker exec -it library-postgres psql -U library_user -d postgres -c "\l"

Then check each has data:

bash
docker exec -it library-postgres psql -U library_user -d book_db -c "SELECT * FROM books;"
docker exec -it library-postgres psql -U library_user -d member_db -c "SELECT * FROM members;"
docker exec -it library-postgres psql -U library_user -d loan_db -c "SELECT * FROM lo
