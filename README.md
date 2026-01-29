# Drone Delivery Management Backend
Spring Boot modular monolith for the drone delivery assessment. It exposes REST APIs, issues HMAC-signed JWTs, and runs
against an in-memory H2 database by default.

API: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Quick start

1. Ensure Java 17 is available and correct java path is set in your environment variables (PATH, JAVA_HOME).

If `JAVA_HOME` is not set in your environment, set it once in the `.java-home` file (first line only) so the Gradle
wrapper can find Java on every platform.
**Note**: if you ar running embedded Intellij java sdk, you can find the sdk path by going to File > Project Structure >
SDKs and copy the path into `.java-home` file

2. Make sure you have gradle installed or have an IDE Compatible with gradle projects (Intellij has it by default), then
   open the build.gradle
   file and press reload gradle dependencies icon
3. Start the app by running main method by using IDE (com.example.dronedelivery.DroneDeliveryApplication.main) or by
   running:

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8080` and Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

## Auth process

Authentication is intentionally simple for the assessment:

1. Call `POST /auth/token` with a name and role (`ADMIN`, `ENDUSER`, `DRONE`).
2. Use the returned access token as `Authorization: Bearer <token>` for all other requests.

## Scheduler behavior

The assignment scheduler periodically matches **OPEN** jobs to the closest available drone based on the drone’s last
heartbeat location. It ignores drones that are broken or already on a job. When a drone misses heartbeats for too long,
it is automatically marked **BROKEN**; if it was mid-delivery, a handoff job is created so another drone can finish the
delivery.

## Configs you are likely to change

All configs live in `application.yml` (or can be overridden with environment variables).

| Config key                                  | Default              | Why you might change it                                |
|---------------------------------------------|----------------------|--------------------------------------------------------|
| `app.scheduler.assignment-interval-seconds` | `10`                 | Adjust how frequently the scheduler assigns open jobs. |
| `app.scheduler.heartbeat-timeout-minutes`   | `5`                  | Tune how quickly inactive drones are marked as broken. |
| `spring.datasource.url`                     | `jdbc:h2:mem:testdb` | Point to a real database instead of H2.                |
| `spring.jpa.hibernate.ddl-auto`             | `create-drop`        | Change schema management for persistent environments.  |

## Run tests

### Unit tests (src/test/java/com/example/dronedelivery/service) (Please Run with Application OFF)

These tests run with the Spring test context and you must **not** app running:

```bash
./gradlew test --tests 'com.example.dronedelivery.service.*'
```

### Integration tests (API package, Please run with app RUNNING)

1. Start the app in one terminal (or by running using IDE):

```bash
./gradlew bootRun
```

2. Run the API package (src/test/java/com/example/dronedelivery/api) tests from another terminal (or by running using
   IDE):

```bash
./gradlew integrationTest --tests 'com.example.dronedelivery.api.*'
```

## Current design

- It is a single Spring Boot service for now so everything is easy to trace and debug.
- Data stays in a single database (H2 by default) with JPA entities for drones, orders, and jobs.
- Job reservation uses optimistic locking so concurrent drones don’t double-reserve the same job.
- The scheduler automates “closest drone” assignment and the heartbeat timeout protects against silent failures.

## TODOs / Future improvements

- Split into smaller services once traffic grows and use **gRPC** between services (auth, orders, drones, jobs).
- Add an event stream for state changes using **KAFKA** (job reserved, job started, drone broken).
- Replace H2 with **Postgres/CockroachDb** or any ACID Scalable DB and add read caching **(Redis)** for hot endpoints.
- Introduce observability (metrics + tracing) via **Prometheus/Grafana** before scaling.
