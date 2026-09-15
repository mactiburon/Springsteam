# 🎮 Springsteam — Plataforma de Juegos

Plataforma estilo Steam con arquitectura de microservicios: backend en **Spring Boot + Kotlin**, frontend en **Compose Multiplatform**.

## Arquitectura

```
                        ┌──────────────┐
                        │   Frontend   │
                        │   (Compose)  │
                        └──────┬───────┘
                               │ HTTP
                        ┌──────▼───────┐
                        │   Gateway    │
                        │   :8080      │
                        └──┬───┬───┬───┘
                           │   │   │
              ┌────────────┘   │   └────────────┐
              ▼                ▼                 ▼
       ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
       │ auth-service│ │ game-service│ │library-svc  │
       │    :8081    │ │    :8082    │ │   :8083     │
       └──────┬──────┘ └──────┬──────┘ └──────┬──────┘
              │               │               │
              │         ┌─────▼─────┐         │
              │         │  Kafka    │◄────────┘
              │         └─────┬─────┘
              │               │
              ▼               ▼
       ┌─────────────┐ ┌─────────────┐
       │ PostgreSQL  │ │audit-service│
       │  authdb +   │ │   :8084     │
       │  gamedb +   └──────┬──────┘
       │  librarydb +        │
       │  auditdb            ▼
       └────────────── PostgreSQL (auditdb)
```

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.1.20 · Java 21 |
| Framework | Spring Boot 3.5.3 · Spring Cloud 2025.0.0 |
| Seguridad | JWT HS256 (OAuth2 Resource Server) |
| Mensajería | Apache Kafka 3.9.0 (KRaft mode) |
| Base de datos | PostgreSQL 16 · H2 en tests |
| Migraciones | Flyway |
| Build | Gradle 8.14 (multi-module) |
| Infra | Docker Compose (dev) |

## Servicios

| Servicio | Puerto | Descripción | Tests |
|----------|--------|-------------|-------|
| **auth-service** | 8081 | Usuarios, registro, login, JWT | 27 |
| **game-service** | 8082 | Catálogo CRUD + importación RAWG | 22 |
| **library-service** | 8083 | Biblioteca + categorías + wishlist + proyección local | 94 |
| **audit-service** | 8084 | Registro de todos los eventos de dominio | 19 |
| **api-gateway** | 8080 | Routing → servicios | — |
| **Total** | | | **162** |

## Quick Start

### 1. Infraestructura (Docker)

```bash
cd backend/deployments
docker compose up -d
```

Levanta PostgreSQL (puerto 5433) con 4 bases de datos (`authdb`, `gamedb`, `librarydb`, `auditdb`) y Kafka en modo KRaft.

### 2. Arrancar servicios

En terminal separada para cada uno:

```bash
cd backend
./gradlew :auth-service:bootRun
./gradlew :game-service:bootRun
./gradlew :library-service:bootRun
./gradlew :audit-service:bootRun
./gradlew :api-gateway:bootRun
```

### 3. Variables de entorno (opcionales)

Todas tienen valores por defecto para desarrollo local:

| Variable | Default | Descripción |
|----------|---------|-------------|
| `JWT_SECRET` | `dev-only-jwt-secret-cambiar-antes-de-produccion` | Secreto HS256 (≥32 chars) |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Broker de Kafka |
| `RAWG_API_KEY` | *(vacía)* | API key de RAWG (para importar juegos) |
| `EVENTS_ENABLED` | `true` | Habilitar productor/consumidor Kafka |

### 4. Tests

```bash
cd backend
./gradlew build
```

Los tests usan **H2 en memoria en modo PostgreSQL** — no necesitan Docker ni Kafka. Cada test corre en su propia transacción que se revierte al final.

## Eventos Kafka

Los servicios se comunican asincrónicamente a través de 3 topics:

| Topic | Publicador | Consumidor | Eventos |
|-------|-----------|------------|---------|
| `user-events` | auth-service | audit-service | `user.registered`, `user.deactivated` |
| `game-events` | game-service | library-service, audit-service | `game.created`, `game.updated`, `game.deleted` |
| `library-events` | library-service | audit-service | `library.added`, `library.updated`, `library.removed` |

## Estructura del proyecto

```
backend/
├── shared/                  ← Librería compartida (seguridad, eventos, errores)
├── auth-service/            ← Autenticación y usuarios
├── game-service/            ← Catálogo de juegos
├── library-service/         ← Biblioteca personal por usuario
├── audit-service/           ← Registro de eventos de dominio
├── api-gateway/             ← Spring Cloud Gateway
└── deployments/             ← Docker Compose + init SQL

frontend/
└── composeApp/              ← Compose Multiplatform (Desktop JVM)
```

## API por servicio

Ver documentación detallada en [`docs/services/`](docs/services/).

| Servicio | Endpoints principales |
|----------|----------------------|
| auth-service | `POST /api/users` (registro) · `POST /api/users/login` (JWT) · `GET /api/users` |
| game-service | `GET /api/games` · `POST /api/games` · `POST /api/games/import` |
| library-service | `GET/POST /api/library` · `PUT/DELETE /api/library/{gameId}` · `GET/POST /api/categories` · `PUT/DELETE /api/categories/{id}` · `POST/DELETE /api/categories/{categoryId}/library/{gameId}` · `GET/POST /api/wishlist` · `DELETE /api/wishlist/{gameId}` |
| audit-service | `GET /api/audit` · `GET /api/audit/{id}` |

## Decisiones técnicas clave

- **JWT compartido**: todos los servicios usan el mismo `app.jwt.secret` para validar tokens que emite auth-service.
- **Identidad del usuario**: el `sub` del JWT es el `userId`. Los servicios lo extraen con `Jwt.userId()` — no consultan auth-service.
- **Proyección local**: library-service mantiene una copia de la tabla `games` sincronizada vía Kafka, para resolver biblioteca con juego anidado sin llamadas síncronas.
- **Cada evento = su propia transacción**: el `@Transactional` se aplica por mensaje Kafka, nunca por batch, para aislar fallos.
- **`app.events.enabled=false`** en tests: desactiva productor/consumidor Kafka y evita dependencias de infraestructura.
