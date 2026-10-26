# navan-commerce

HTTP API for **flight / hotel / rail / Navan-fee transactions** and **frozen-snapshot invoices**.
Java 17 · Spring Boot 3 · Spring Data JPA · PostgreSQL · Flyway.

## Documentation

| Doc | What's inside |
|---|---|
| [ARCHITECTURE.md](ARCHITECTURE.md) | High-level diagram, design decisions + rejected alternatives, patterns, high-scale notes |
| [DB_SCHEMA.md](DB_SCHEMA.md) | Full DDL (tables + indexes) and schema design decisions |
| [API_CONTRACT.md](API_CONTRACT.md) | Every endpoint with curl + real sample responses, error codes, conventions |

## Prerequisites

- Java 17+ (`java -version`)
- **PostgreSQL — the only external dependency**

## Setup

```bash
# 1. Install and start PostgreSQL (macOS)
brew install postgresql@16
brew services start postgresql@16

# 2. Create the database
createdb navan_commerce
createdb navan_commerce_test

# 3. Setup username and password of the database
# replace it with your local credentials (for username: in terminal use 'whoami')
current default port : 5432
username: mayankbandil
password: 

```

**3. Point the app at your database.** Edit `src/main/resources/application-dev.yaml`
(replace with your own values):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/navan_commerce   # your host:port/database
    username: your_postgres_user                           # e.g. your macOS username
    password: your_postgres_password                       # brew default: empty/local trust

server:
  port: 8089
```

(Alternatively set env vars `DB_URL` / `DB_USER` / `DB_PASSWORD` — `application.yaml` reads them.)

Schema is created automatically by Flyway on first run — no manual SQL needed.

## Run

```bash
# runs with the dev profile → all values come from application-dev.yaml
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Verify:

```bash
curl localhost:8089/health        # {"status":"UP"}
```

Swagger UI (interactive docs): http://localhost:8089/swagger-ui/index.html

## Test

```bash
./mvnw test
```

Integration tests use a separate `test` database on the same Postgres instance
(auto-created, migrated, truncated between tests) — dev data is never touched.

## Try it

Seed payloads for every transaction type live in `task-b-travel-inventory/fixtures/task-b/seed-payloads.json`;
full request/response examples for every endpoint are in [API_CONTRACT.md](API_CONTRACT.md).
