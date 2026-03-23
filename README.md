# kafkaboard

![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791)
![Kafka](https://img.shields.io/badge/Kafka-3.7.0-231F20)

Kafka cluster management tool.

## Features
- `🔐` JWT authentication with refresh tokens, logout revocation, and rate limiting.
- `🧩` Multi-cluster support backed by Supabase PostgreSQL persistence.
- `❤️` Cluster health monitoring with broker and topic visibility.
- `🗂️` Topic management for create, list, delete, and latest-message inspection.
- `📉` Real-time consumer group lag streaming over WebSocket.
- `📈` Lag trend visualization support for the frontend via live data updates.
- `⚡` Bucket4j-based abuse protection for auth and API endpoints.
- `🌙` Dark mode friendly API for the React dashboard.

## Architecture
```text
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   React UI  │────▶│ Spring Boot  │────▶│    Kafka    │
│  (Vite)     │     │   Backend    │     │   Cluster   │
└─────────────┘     └──────┬───────┘     └─────────────┘
                           │
                    ┌──────▼───────┐
                    │  Supabase    │
                    │ PostgreSQL   │
                    └──────────────┘
```

## Quick Start
1. Requirements: Java 21, Docker, Node.js 20+
2. Clone the repositories:
```bash
git clone <your-backend-repo-url> kafkaboard
git clone <your-frontend-repo-url> kafkaboard-ui
```
3. Copy `.env.example` to `.env` and fill in your secrets:
```bash
cp .env.example .env
```
4. Start the backend stack:
```bash
docker-compose up --build
```
5. Start the frontend from the `kafkaboard-ui` repository or run its Docker image.
6. Open `http://localhost:5173`.

## Self-Hosted Docker
Run Kafkaboard without source code by shipping the frontend and backend images together.

1. Copy the self-hosted environment file:
```bash
cp .env.selfhosted.example .env.selfhosted
```
2. Fill in:
```env
POSTGRES_PASSWORD=...
JWT_SECRET=...
ENCRYPTION_KEY=...
FRONTEND_PORT=8090
BACKEND_PORT=8080
```
3. Make sure the frontend and backend images are available locally or replace the image names with your registry paths.
4. Start the stack:
```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml up -d
```
5. Open:
```text
http://localhost:${FRONTEND_PORT}
```

Notes:
- This stack includes its own PostgreSQL container.
- `FRONTEND_PORT` and `BACKEND_PORT` are user-configurable in `.env.selfhosted`.
- If the user's Kafka broker runs on the host machine, they should add it in Kafkaboard as `host.docker.internal:9092`, not `localhost:9092`.
- `host.docker.internal` is prewired in the backend container through `extra_hosts` so the backend can reach Kafka running on the host.

## Self-Hosted User Guide
This is the recommended flow for users who want to run Kafkaboard locally and inspect a Kafka broker on their own machine or network.

### What The User Needs
- Docker Desktop or Docker Engine with Compose support
- The `docker-compose.selfhosted.yml` file
- The `.env.selfhosted` file
- Access to the published Kafkaboard frontend and backend images
- A running Kafka broker

### What Happens After Startup
When the user runs the compose stack, Docker starts:
- `postgres`: local application database
- `backend`: Spring Boot API
- `frontend`: Nginx serving the Kafkaboard UI

The browser talks to the local frontend container, and the frontend proxies API and WebSocket traffic to the local backend container.

### Step 1: Prepare The Environment File
Copy the example file:

```bash
cp .env.selfhosted.example .env.selfhosted
```

Fill in at least:

```env
KAFKABOARD_BACKEND_IMAGE=ghcr.io/<owner>/kafkaboard-backend:latest
KAFKABOARD_UI_IMAGE=ghcr.io/<owner>/kafkaboard-ui:latest
POSTGRES_PASSWORD=change-this-postgres-password
JWT_SECRET=change-this-to-a-strong-secret-min-32-chars
ENCRYPTION_KEY=change-this-32-char-encryption-key
FRONTEND_PORT=8090
BACKEND_PORT=8080
```

Rules:
- `JWT_SECRET` should be a strong secret with at least 32 characters.
- `ENCRYPTION_KEY` must be exactly 32 characters.
- `FRONTEND_PORT` is the browser port the user will open.
- `BACKEND_PORT` is optional for direct backend access, logs, or debugging.

### Step 2: Start Kafkaboard
Run:

```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml up -d
```

To check container status:

```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml ps
```

To see logs:

```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml logs -f
```

### Step 3: Open The UI
In the browser, open:

```text
http://localhost:<FRONTEND_PORT>
```

Example:

```text
http://localhost:8090
```

### Step 4: Create An Account
On first use:
- open the landing page
- go to register
- create a local account

This account is stored only in the local self-hosted database unless the user points the app to another database.

### Step 5: Connect Kafka
After login, add a cluster from the UI.

#### If Kafka Runs On The Same Machine As Docker
Do not use `localhost:9092`.

Use:

```text
host.docker.internal:9092
```

Reason:
- the backend runs inside a container
- inside that container, `localhost` means the container itself, not the user's host machine

#### If Kafka Runs On Another Machine
Use the reachable hostname or IP:

```text
192.168.1.50:9092
```

or:

```text
kafka.internal.example:9092
```

#### Security Settings
Choose the cluster settings in the UI to match the broker:
- `PLAINTEXT`
- `SASL_PLAINTEXT`
- `SASL_SSL`

If SASL is enabled, also provide:
- username
- password
- mechanism such as `PLAIN`, `SCRAM-SHA-256`, or `SCRAM-SHA-512`

### Step 6: Validate The Connection
Use the built-in connection test in the cluster form.

If the test succeeds, save the cluster and continue with:
- overview
- topics
- messages
- consumer groups

### Common Local Setup Examples
#### Example A: Local Kafka On The Host
- Kafka broker: `host.docker.internal:9092`
- Frontend URL: `http://localhost:8090`

#### Example B: Remote Development Kafka
- Kafka broker: `dev-kafka.example.com:9092`
- Frontend URL: `http://localhost:8090`

### Common Problems
#### UI Opens But Login Fails
- check backend logs
- verify `JWT_SECRET` and `ENCRYPTION_KEY`
- verify PostgreSQL container is healthy

#### Kafka Connection Test Fails
- make sure the broker is running
- if Kafka is on the host, use `host.docker.internal:9092`
- verify firewall, port, and SASL/SSL settings

#### Frontend Opens On The Wrong Port
- update `FRONTEND_PORT` in `.env.selfhosted`
- restart the compose stack

### Stop Or Remove The Stack
Stop containers:

```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml down
```

Stop and remove database data too:

```bash
docker compose --env-file .env.selfhosted -f docker-compose.selfhosted.yml down -v
```

## API Documentation

### Auth
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `POST` | `/api/auth/register` | No | Register a new user and return access + refresh tokens |
| `POST` | `/api/auth/login` | No | Login and return access + refresh tokens |
| `POST` | `/api/auth/refresh` | No | Exchange a valid refresh token for a new access token |
| `POST` | `/api/auth/logout` | No | Revoke a refresh token |

### Clusters
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/clusters` | Yes | List clusters owned by the authenticated user |
| `POST` | `/api/clusters` | Yes | Save a new Kafka cluster |
| `POST` | `/api/clusters/test-connection` | Yes | Validate Kafka connectivity before saving |
| `DELETE` | `/api/clusters/{id}` | Yes | Delete a saved cluster |

### Health
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/clusters/{id}/health` | Yes | Fetch cluster health, node count, and topic count |

### Topics
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/clusters/{id}/topics` | Yes | List topics for a cluster |
| `POST` | `/api/clusters/{id}/topics` | Yes | Create a topic |
| `DELETE` | `/api/clusters/{id}/topics/{topicName}` | Yes | Delete a topic |

### Consumer Groups
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/clusters/{id}/consumer-groups` | Yes | Fetch consumer group lag snapshot |

### Messages
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `GET` | `/api/clusters/{id}/topics/{topicName}/messages?limit=N` | Yes | Fetch latest messages from a topic |

### WebSocket
| Method | Endpoint | Auth | Description |
| --- | --- | --- | --- |
| `WS` | `/ws/clusters/{clusterId}/lag?token=...` | Yes | Stream live consumer group lag updates |

## Environment Variables
| Variable | Description | Example |
| --- | --- | --- |
| `DB_PASSWORD` | Supabase database password | `super-secret-db-password` |
| `JWT_SECRET` | JWT signing secret, minimum 32 chars | `change-this-to-a-strong-secret-key` |
| `SPRING_DATASOURCE_URL` | JDBC URL for Supabase PostgreSQL | `jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?preferQueryMode=simple` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres.jwtucgtlouqpgvgexrsh` |
| `SPRING_FLYWAY_URL` | Flyway JDBC URL | `jdbc:postgresql://aws-1-eu-west-1.pooler.supabase.com:6543/postgres?preferQueryMode=simple` |
| `SPRING_FLYWAY_USER` | Flyway database username | `postgres.jwtucgtlouqpgvgexrsh` |

## Technology Stack
| Layer | Technology | Version |
| --- | --- | --- |
| Backend runtime | Java | 21 |
| Backend framework | Spring Boot | 3.x |
| Security | Spring Security, JWT, Refresh Token | Current |
| Database | Supabase PostgreSQL | PostgreSQL 17 |
| Migrations | Flyway | Current |
| Kafka access | Spring Kafka, AdminClient, KafkaConsumer | Current |
| Rate limiting | Bucket4j | 8.10.1 |
| Real-time transport | Spring WebSocket | Current |
| Frontend | React + Vite + Tailwind CSS | Current |

## Development Notes
- `.env` is ignored and should never be committed.
- Docker Compose starts Kafka and the backend; the frontend has its own Docker setup in the `kafkaboard-ui` repository.
- WebSocket authentication uses the access token as a query parameter because native browser WebSocket APIs cannot set arbitrary headers.
