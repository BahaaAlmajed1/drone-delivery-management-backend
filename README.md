# Drone Delivery Management Backend (Technical Assessment)

This project implements the assessment requirements as a **Spring Boot modular monolith** with:
- REST API
- Self-signed **HMAC JWT** auth issued by an allowlisted endpoint (`/auth/token`)
- **In-memory database by default (H2)** so it runs with zero external dependencies
- Swagger / OpenAPI docs
- Dockerfile + docker-compose
- A small test suite

## Quick start (zero dependencies)

```bash
gradle bootRun
```

API: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Optional (debug): H2 console at `http://localhost:8080/h2`

---

## Run with Docker

```bash
docker compose up --build
```

API: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

---

## Auth (JWT)

Assessment requirement: the JWT is handed out by an endpoint that takes a **name** and a **type of user** (admin, enduser, drone).  
Then all other endpoints validate that JWT as `Authorization: Bearer <token>`.

### Get tokens

**Admin**
```bash
curl -s http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"name":"alice-admin","userType":"ADMIN"}'
```

**Enduser**
```bash
curl -s http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"name":"bob-enduser","userType":"ENDUSER"}'
```

**Drone**
```bash
curl -s http://localhost:8080/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"name":"drone-01","userType":"DRONE"}'
```

The response is:
```json
{ "tokenType": "Bearer", "accessToken": "..." }
```

---

## Main flows

### Enduser: submit an order

```bash
ENDUSER_TOKEN="...";

curl -s http://localhost:8080/enduser/orders \
  -H "Authorization: Bearer $ENDUSER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "origin": {"lat": 24.7136, "lng": 46.6753},
    "destination": {"lat": 24.7743, "lng": 46.7386}
  }'
```

This creates:
- an `orders` row (`SUBMITTED`)
- an initial `jobs` row (`OPEN`) with pickup=origin and dropoff=destination

### Drone: heartbeat (location update + status update)

Assessment requirement: drones update lat/lng and **get a status update as a heartbeat**.

```bash
DRONE_TOKEN="...";

curl -s http://localhost:8080/drone/self/heartbeat \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"location":{"lat":24.7200,"lng":46.6800}}'
```

### Drone: list open jobs and reserve one

```bash
curl -s http://localhost:8080/drone/jobs/open \
  -H "Authorization: Bearer $DRONE_TOKEN"
```

Reserve:
```bash
JOB_ID="...";

curl -s http://localhost:8080/drone/jobs/$JOB_ID/reserve \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -X POST
```

Reservation is concurrency-safe via **optimistic locking** on the `jobs` table. If two drones race, one gets `409 Conflict`.

### Drone: pickup and complete (or fail)

Pickup:
```bash
curl -s http://localhost:8080/drone/jobs/$JOB_ID/pickup \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -X POST
```

Complete:
```bash
curl -s http://localhost:8080/drone/jobs/$JOB_ID/complete \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -X POST
```

Fail:
```bash
curl -s http://localhost:8080/drone/jobs/$JOB_ID/fail \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -X POST
```

### Drone: mark broken (handoff rule)

Assessment additional rule:
> Any time a drone is broken it will stop and put up a job for its goods to be picked up by a different drone (even if it gets marked as fixed).

If a drone marks itself broken while **holding the goods** (i.e., its current job is `IN_PROGRESS`), the service:
1. fails the current job
2. creates a new **handoff job** (`HANDOFF_PICKUP_AND_DELIVER`) with pickup = the broken drone’s **last heartbeat location**
3. sets `excludedDroneId` on that handoff job so the same drone can never reserve it (even if later marked fixed)

```bash
curl -s http://localhost:8080/drone/self/broken \
  -H "Authorization: Bearer $DRONE_TOKEN" \
  -X POST
```

### Enduser: withdraw an order (only if not picked up)

```bash
ORDER_ID="...";

curl -s http://localhost:8080/enduser/orders/$ORDER_ID/withdraw \
  -H "Authorization: Bearer $ENDUSER_TOKEN" \
  -X POST
```

Withdrawal is rejected if the current job is already `IN_PROGRESS` (interpreting “picked up” as the job started).

---

## Admin endpoints

List orders:
```bash
ADMIN_TOKEN="...";

curl -s http://localhost:8080/admin/orders \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Bulk get orders:
```bash
curl -s http://localhost:8080/admin/orders/bulk \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orderIds":["...","..."]}'
```

Change origin/destination (guarded: only before pickup; origin not allowed during handoff):
```bash
curl -s http://localhost:8080/admin/orders/$ORDER_ID \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -X PATCH \
  -d '{"origin":{"lat":24.7000,"lng":46.6500},"destination":{"lat":24.8000,"lng":46.7500}}'
```

List drones:
```bash
curl -s http://localhost:8080/admin/drones \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Mark drone broken/fixed (broken triggers the same handoff logic if it was in progress):
```bash
DRONE_ID="...";

curl -s http://localhost:8080/admin/drones/$DRONE_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -X POST \
  -d '{"status":"BROKEN"}'
```

---

## Local dev (without Docker)

By default, the app runs against an **in-memory H2** DB with `create-drop` schema generation.

If you want to point to another JDBC database later, override `SPRING_DATASOURCE_URL` (and credentials) while keeping
the same Spring Boot configuration.

---

## Run tests

```bash
gradle test
```

Tests run against an in-memory H2 database (schema `create-drop`).

### API acceptance tests (how to use)

The API-driven tests exercise the same flows shown above using real HTTP calls and JWTs.
They are grouped by role and use `POST /auth/token` to fetch a token for each user type
before calling the endpoints:

- `AuthApiTest`: verifies token issuance for `ADMIN`, `ENDUSER`, and `DRONE`.
- `EndUserApiTest`: submit, withdraw, list, and get orders; includes the “cannot withdraw after pickup” corner case.
- `DroneApiTest`: reserve, pickup, complete, fail, mark broken (handoff), and a concurrent reservation test.
- `AdminApiTest`: list orders, bulk fetch, update order, list drones, and set drone status.

To run only the API tests:

```bash
gradle test --tests 'com.example.dronedelivery.api.*'
```

Note: if you are using the Gradle wrapper, make sure `gradlew` is executable and the wrapper JAR is present:

```bash
chmod +x gradlew
./gradlew test
```

---

## Design doc (current architecture)

### Overview

- **Architecture**: Spring Boot modular monolith with REST APIs.
- **Auth**: HMAC JWT issued by `/auth/token`, then used as `Authorization: Bearer <token>`.
- **Data**: H2 in-memory DB by default, JPA entities for drones, orders, jobs.
- **Concurrency control**: Optimistic locking on jobs for reservation race handling.

### Domain model (key entities)

- **Drone**: status, last location, heartbeat, current job.
- **DeliveryOrder**: created by enduser, status, current job, origin/destination.
- **Job**: pickup/dropoff, status lifecycle, assigned drone, optional excluded drone for handoff.

### Core flows

- **Submit order** → creates order + OPEN job.
- **Reserve job** → sets job RESERVED and assigns drone.
- **Pickup** → job IN_PROGRESS, order IN_DELIVERY.
- **Complete/Fail** → job terminal, order DELIVERED/FAILED.
- **Broken drone** → fail job + create handoff job at drone’s last location (excluded drone enforced).

---

## Future plans / TODOs

### Near-term improvements

- Add contract tests for error handling (403/409/400) across all endpoints.
- Add metrics for job reservation races and handoff frequency.
- Expand seed data to include bulk admin scenarios and multiple handoff chains.

### Scaling plan: move to microservices + async architecture

As usage grows (large fleets and high order volume), the monolith can be split into services and
made asynchronous to reduce coupling and improve throughput:

**Target services**

- **Auth Service** (JWT issuance, token introspection)
- **Order Service** (order lifecycle, progress, admin changes)
- **Drone Service** (heartbeat, location, status, assignment)
- **Job Service** (reservation/pickup/complete/fail and handoff logic)
- **Notification Service** (events → push/alerts to clients)

**New technologies to introduce**

- **gRPC** for internal service-to-service communication (lower latency, typed contracts).
- **Kafka** (or **NATS**) for event streaming: `JobReserved`, `JobStarted`, `JobCompleted`, `DroneBroken`, etc.
- **Outbox pattern** for reliable event publishing.
- **Redis** for caching hot reads (open jobs, live drone locations).
- **PostgreSQL** (or CockroachDB) for horizontal scale and stronger consistency guarantees.
- **Observability stack**: OpenTelemetry + Prometheus/Grafana for tracing and metrics.

**Async flow examples**

- Reservation produces `JobReserved` → Order Service updates status asynchronously.
- Drone heartbeat emits `DroneLocationUpdated` → consumers update ETA/progress views.
- Broken drone emits `DroneBroken` → Job Service creates handoff + Notification Service alerts.
