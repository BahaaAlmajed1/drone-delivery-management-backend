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
