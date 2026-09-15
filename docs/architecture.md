# 📐 Arquitectura

## 1. Diagrama de componentes

```mermaid
flowchart TB
    subgraph Client["Cliente"]
        FE["Frontend Compose\n(desktop JVM)"]
    end

    subgraph Edge["Capa de entrada"]
        GW["API Gateway\n:8080"]
    end

    subgraph Core["Microservicios"]
        AUTH["auth-service\n:8081"]
        GAME["game-service\n:8082"]
        LIB["library-service\n:8083"]
        AUDIT["audit-service\n:8084"]
    end

    subgraph Data["Persistencia y mensajería"]
        PG[("PostgreSQL 16\n:5433")]
        KAFKA{{"Kafka 3.9 KRaft\n:9092"}}
        RAWG[("RAWG API")]
    end

    FE -->|"HTTP + JWT Bearer"| GW
    GW -->|"/api/users/**"| AUTH
    GW -->|"/api/games/**"| GAME
    GW -->|"/api/library/**"| LIB
    GW -->|"/api/audit/**"| AUDIT

    AUTH -->|"lectura/escritura"| PG
    GAME -->|"lectura/escritura"| PG
    LIB -->|"lectura/escritura"| PG
    AUDIT -->|"solo escritura"| PG

    GAME -->|"importar catálogo"| RAWG

    AUTH -->|"usuario.registered"| KAFKA
    GAME -->|"juego.created/updated/deleted"| KAFKA
    LIB -->|"library.added/updated/removed"| KAFKA
    KAFKA -->|"todos los eventos"| AUDIT
    KAFKA -->|"game.*"| LIB
```

## 2. Flujo de eventos

```mermaid
sequenceDiagram
    autonumber
    participant G as game-service
    participant L as library-service
    participant A as audit-service
    participant K as Kafka

    rect rgb(240, 248, 255)
        Note over G,K: Alta de juego
        G->>K: publish("game-events", "game.created", {gameId, name, ...})
    end

    rect rgb(255, 249, 235)
        Note over K,L: Proyección local
        K-->>L: consume "game-events"
        L->>L: upsert Game en tabla local `games`
    end

    rect rgb(250, 255, 240)
        Note over K,A: Auditoría
        K-->>A: consume "user-events", "game-events", "library-events"
        A->>A: persiste AuditEvent (payload + userId/gameId denormalizados)
    end
```

## 3. Secuencia JWT (autenticación)

```mermaid
sequenceDiagram
    autonumber
    participant FE as Frontend
    participant GW as Gateway
    participant AUTH as auth-service
    participant SVC as Servicio (game/library/audit)
    participant DB as PostgreSQL

    FE->>GW: POST /api/users/login
    GW->>AUTH: reenvía (login)
    AUTH->>AUTH: verifica BCrypt(username/email + password)
    AUTH-->>FE: { token: JWT, tokenType: Bearer, expiresIn: 86400 }
    Note over FE: sub = userId
    FE->>GW: GET /api/library (Authorization: Bearer <JWT>)
    GW->>SVC: reenvía con la misma cabecera
    SVC->>SVC: valida JWT (misma clave HS256 compartida)
    SVC->>SVC: extrae userId desde `sub`
    SVC->>DB: consulta datos del usuario autenticado
    DB-->>SVC: resultado
    SVC-->>FE: JSON (sin exponer identidad en la URL)
```

## 4. Formato de eventos (`EventEnvelope`)

Todos los eventos usan la misma envoltura (`shared/event/EventEnvelope.kt`):

```json
{
  "type": "game.created",
  "occurredAt": "2026-09-15T10:00:00Z",
  "payload": { "gameId": 42, "name": "Celeste", "genre": "Platformer" }
}
```

### Payloads por dominio

| Evento | Payload |
|--------|---------|
| `user.registered` | `{ userId, username }` |
| `user.deactivated` | `{ userId, username }` |
| `game.created` · `game.updated` | `{ gameId, name, description?, genre?, releaseDate?, developer?, publisher?, cover?, createdAt }` |
| `game.deleted` | `{ gameId, name }` |
| `library.added` · `library.updated` | `{ userId, gameId, isFavorite, hoursPlayed }` |
| `library.removed` | `{ userId, gameId }` |

## 5. Puertos y rutas

| Componente | Puerto | Rutas |
|------------|--------|-------|
| api-gateway | 8080 | `/api/users/**`, `/api/games/**`, `/api/library/**`, `/api/audit/**` |
| auth-service | 8081 | `/api/users/**` |
| game-service | 8082 | `/api/games/**` |
| library-service | 8083 | `/api/library/**` |
| audit-service | 8084 | `/api/audit/**` |
| PostgreSQL | 5433 | — |
| Kafka | 9092 | `user-events`, `game-events`, `library-events` |

## 6. Base de datos

Una sola instancia PostgreSQL para desarrollo con **una base por servicio** (aislamiento como en producción, mismo servidor):

```
postgres:5433
├── authdb     ← tablas de usuarios (U único por username/email)
├── gamedb     ← juegos del catálogo
├── librarydb  ← games (proyección) + library_entries
└── auditdb    ← audit_events
```

- Cada base tiene sus propias migraciones **Flyway** con `ddl-auto: validate` (valida que las entidades JPA cuadran con el esquema, nunca lo modifica).
- `librarydb.library_entries.game_id` → FK → `librarydb.games(id)` con **`ON DELETE CASCADE`**: borrar un juego desde game-service (vía evento `game.deleted`) limpia automáticamente las entradas de biblioteca.

## 7. Seguridad

- **auth-service** (único emisor): bcrypt para contraseñas, emite JWT HS256 con `sub` = userId.
- **Gateway**: reenvía sin modificar las cabeceras; no valida JWT (los servicios lo hacen).
- **Servicios**: cada uno valida el JWT con el **mismo secreto** (`app.jwt.secret`) vía `JwtSecurityConfig` compartido.
- **Endpoints públicos:**
  - `POST /api/users` (registro)
  - `POST /api/users/login` (login → JWT)
  - `GET /api/games/**` (catálogo abierto)
  - `/actuator/**` (health/info)
- El resto requiere `Authorization: Bearer <JWT>`.