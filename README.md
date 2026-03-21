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
