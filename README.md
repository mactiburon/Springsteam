# 🎮 Spring Boot Demo — Plataforma de Juegos

Plataforma de juegos estilo Steam construida con **Spring Boot + Kotlin + PostgreSQL (Supabase)**.
Backend 100% API REST y **frontend de escritorio en Compose Multiplatform (Material 3)**.
Objetivo de aprendizaje: Spring Boot, JPA/Hibernate, WebSockets, y más adelante
Compose Multiplatform, Docker, Kafka y microservicios.

## 🧱 Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.3.21 (Java 21) |
| Framework | Spring Boot 4.1.1 |
| Persistencia | Spring Data JPA / Hibernate 7 |
| Base de datos | PostgreSQL (Supabase) en dev · H2 en memoria en tests |
| Seguridad | Spring Security (BCrypt) + JWT HS256 (Fase 9) |
| Tests | JUnit 5 + MockMvc |
| Frontend | Compose Multiplatform (Material 3, KMP Desktop JVM) |
| HTTP cliente | Ktor (frontend) + kotlinx.serialization |

## 🗺️ Arquitectura y evolución

```
            AHORA (monolito)                     FUTURO
        ┌──────────────────┐          ┌──────────────────────┐
        │   Spring Boot    │          │   API Gateway        │
        │  (una sola app)  │          │    /                 │
        └───────┬──────────┘          │  Microservicios      │
                │                     │     + Kafka          │
        ┌───────┴───────┐             │     + Docker         │
        ↓       ↓       ↓             │     + WebSockets     │
     Usuarios Juegos  Biblioteca      └──────────────────────┘
        │       │       │
        ├───────┼───────┤
        ↓       ↓       ↓
     Amigos   Chat    Búsqueda
                │
                ↓
          PostgreSQL (Supabase)
```

**Etapas planificadas:**
1. 🟢 **Monolito** (ahora): Kotlin + Spring Boot + JPA + PostgreSQL
2. 🟡 **Docker**: dockerizar app y base de datos
3. 🟠 **Kafka**: eventos entre módulos (biblioteca, chat, notificaciones)
4. 🔴 **Microservicios**: separar por dominio cuando tenga sentido

## 📦 Módulos

Cada módulo sigue el patrón **Entity → Repository → Service → Controller → DTO**.

| Módulo | Estado | Descripción |
|--------|--------|-------------|
| ⚙️ Config | ✅ Hecho | Beans (BCrypt), Security temporal, `.env` |
| 👤 Usuarios | ✅ Hecho | Registro, login, perfil, búsqueda |
| 🎮 Juegos | ✅ Hecho | Catálogo y filtros |
| 📚 Biblioteca | ✅ Hecho | Juegos del usuario, favoritos, horas |
| 👥 Amigos | ✅ Hecho | Solicitudes, aceptar, bloquear |
| 🏷️ Motes | ✅ Hecho | Apodos privados entre amigos |
| 🔎 Búsqueda | ✅ Hecho | Búsqueda global + autocompletado |
| 💬 Chat | ✅ Hecho | Conversaciones + mensajería WebSocket |
| 🔔 Notificaciones | ✅ Hecho | Avisos generados por amigos y chat |
| 🔐 Seguridad | ✅ Hecho | Registro/login con JWT HS256 y endpoints protegidos |
| ⚙️ Configuración | ✅ Hecho | Cambiar contraseña, email, username y desactivar cuenta |

## 📁 Estructura del proyecto

```
springbootdemo/
├── build.gradle.kts
├── src/main/kotlin/com/marcmarco/springbootdemo/
│   ├── config/            # Beans y configuración de Spring
│   ├── common/exception/  # Excepciones + manejo global de errores
│   ├── user/              # Módulo de usuarios (Fase 1)
│   │   └── dto/           # Request/Response (nunca exponen password)
│   ├── game/              # Módulo de juegos (Fase 2)
│   ├── library/           # Módulo de biblioteca (Fase 3)
│   ├── friendship/        # Módulo de amigos (Fase 4)
│   ├── nickname/          # Módulo de motes (Fase 5)
│   ├── search/            # Módulo de búsqueda global (Fase 6)
│   ├── chat/              # Conversaciones + WebSocket (Fase 7)
│   │   └── dto/           # Request/Response del chat
│   ├── notification/      # Notificaciones (Fase 8)
│   │   └── dto/           # Response de notificaciones
│   └── security/          # JWT (Fase 9): encoder/decoder, login y auth STOMP
└── src/test/kotlin/.../   # Tests de integración (MockMvc)

frontend/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml
└── composeApp/
    ├── build.gradle.kts
    └── src/
        ├── commonMain/kotlin/com/marcmarco/frontend/
        │   ├── App.kt                # Tema + navegación
        │   ├── ui/                   # Pantallas (Login, Register, Home)
        │   ├── api/                  # Cliente HTTP Ktor + AuthApi + DTOs
        │   └── config/               # URL de la API
        ├── desktopMain/kotlin/.../Main.kt   # Entry point de escritorio
        └── commonTest/kotlin/.../    # Tests comunes (MockEngine)
```

## 🚀 Puesta en marcha

### 1. Variables de entorno
Copia `.env.example` a `.env` y rellena tus credenciales de Supabase:

```powershell
Copy-Item springbootdemo\.env.example springbootdemo\.env
# edita springbootdemo/.env con tus valores reales
```

> ⚠️ `.env` está en `.gitignore`: **nunca se sube al repositorio**. La contraseña de la BD
> se lee en runtime vía `spring.config.import` desde `application.properties`.

### 2. Ejecutar (dev)
Desde IntelliJ: ▶ Run en `SpringbootdemoApplicationKt`.

O desde terminal:
```powershell
cd springbootdemo
.\gradlew.bat bootRun
```

La app arranca en `http://localhost:8080`.

> 🔧 Si el puerto 8080 está ocupado por una instancia antigua:
> `powershell -ExecutionPolicy Bypass -File scripts/kill-8080.ps1`

### 3. Tests
```powershell
cd springbootdemo
.\gradlew.bat test
```

### 4. Frontend (dev)
El frontend es un proyecto Gradle independiente en `frontend/`. Con el backend arrancado
en `http://localhost:8080`:

```powershell
cd frontend
.\gradlew.bat :composeApp:run        # lanza la app de escritorio
.\gradlew.bat :composeApp:desktopTest # tests comunes (MockEngine, sin red)
```

> 📌 El JWT se mantiene en memoria (no persistido todavía). En la Etapa Docker el frontend
> pasará a target web para servirlo junto al backend.

Los tests usan **H2 en memoria en modo PostgreSQL** (sin depender de la red/Supabase) y
cada caso revierte su transacción, así que son reproducibles indefinidamente.

## 🔌 API — Fase 1 (Usuarios)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| POST | `/api/users` | Registro | 201 · 400 · 409 |
| POST | `/api/users/login` | Login (username o email) | 200 · 401 |
| GET | `/api/users` | Listar (opcional `?username=`) | 200 |
| GET | `/api/users/{id}` | Perfil por id | 200 · 404 |
| PUT | `/api/users/{id}` | Editar perfil (parcial) | 200 · 404 |

Respuestas de error con formato uniforme:
```json
{ "status": 409, "error": "Conflict", "message": "El username 'x' ya está en uso", "timestamp": "..." }
```

**Reglas de seguridad aplicadas:**
- La contraseña se almacena con **BCrypt** (nunca en texto plano).
- La contraseña **jamás aparece en ninguna respuesta** de la API.
- Login con credenciales inválidas devuelve el mismo mensaje (no revela qué falló).

## 🔌 API — Fase 2 (Juegos)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| POST | `/api/games` | Crear juego | 201 · 400 · 409 |
| POST | `/api/games/import?count=` | Importar juegos desde RAWG (idempotente) | 200 · 400 |
| GET | `/api/games` | Listar/buscar con filtros | 200 |
| GET | `/api/games/{id}` | Juego por id | 200 · 404 |
| PUT | `/api/games/{id}` | Actualizar juego (todos los campos) | 200 · 404 · 409 |
| DELETE | `/api/games/{id}` | Borrar juego | 204 · 404 |

**Filtros de búsqueda** (combinables, todos opcionales):
`?name=` · `?genre=` · `?developer=` · `?publisher=` · `?releaseDateFrom=AAAA-MM-DD` · `?releaseDateTo=AAAA-MM-DD`

La búsqueda es *case-insensitive* y parcial (contiene el texto), con una sola
consulta JPQL parametrizada que ignora los filtros vacíos (`IS NULL OR LIKE`).

### Importación desde RAWG

`POST /api/games/import?count=40` (exige `Bearer`, `count` de 1 a 100) rellena el
catálogo desde la [API de RAWG](https://rawg.io/apidocs) (key gratuita en `RAWG_API_KEY`
del `.env`). Es **idempotente**: no duplica juegos por nombre (case-insensitive). Mapea
`name`, `released`→`releaseDate`, `background_image`→`cover`, y el primer `genre`,
`developer` y `publisher`. Sin key configurada devuelve `400`.

## 🔌 API — Fase 3 (Biblioteca)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| POST | `/api/library` | Añadir juego a biblioteca | 201 · 400 · 404 · 409 |
| GET | `/api/library` | Listar juegos de un usuario | 200 · 404 |
| GET | `/api/library/{gameId}` | Detalle de una entrada | 200 · 404 |
| PUT | `/api/library/{gameId}` | Actualizar favorito / horas | 200 · 404 |
| DELETE | `/api/library/{gameId}` | Quitar de biblioteca | 204 · 404 |

Desde la Fase 9 el usuario autenticado se identifica con el **JWT (Bearer)** — ya no se
envía `?userId=`.

**Filtros de lista** (combinables):
`?name=` (nombre del juego) · `?favorites=true` (solo marcados como favorito)

La respuesta incluye el objeto `game` anidado con los datos del juego. Las horas son
un `Double` (soporta decimales para sesiones parciales).

## 🔌 API — Fase 4 (Amigos)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| POST | `/api/friendships` | Enviar solicitud (body `{addresseeId}`) | 201 · 400 · 404 · 409 |
| GET | `/api/friendships` | Listar relaciones del usuario autenticado | 200 |
| GET | `/api/friendships/{id}` | Detalle de una relación | 200 · 404 |
| PUT | `/api/friendships/{id}/accept` | Aceptar solicitud (solo el destinatario) | 200 · 400 · 404 · 409 |
| PUT | `/api/friendships/{id}/block` | Bloquear al otro usuario | 200 · 400 · 404 |
| DELETE | `/api/friendships/{id}` | Eliminar relación (cancelar/desamistar) | 204 · 400 · 404 |

**Estados**: `PENDING` → `ACCEPTED` → (bloquear) `BLOCKED`.

La relación es **unidireccional por pareja**: no puede existir a la vez A→B y B→A
(se comprueba en ambas direcciones). Solo los dos implicados pueden actuar sobre ella
(solo el destinatario puede aceptar).

## 🔌 API — Fase 5 (Motes)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| PUT | `/api/nicknames/{targetId}` | Poner/actualizar mote (body `{nickname}`) | 200 · 400 · 404 |
| GET | `/api/nicknames` | Listar los motes del usuario autenticado | 200 |
| GET | `/api/nicknames/{targetId}` | Obtener un mote concreto | 200 · 404 |
| DELETE | `/api/nicknames/{targetId}` | Borrar un mote | 204 · 404 |

Los motes son **privados**: cada usuario tiene su propia lista (clave `owner_id`+`target_id`),
pueden convivir un mote de A hacia B y otro de B hacia A sin colisión.

**Regla de negocio**: solo se puede poner un mote a un usuario con el que exista una
relación `ACCEPTED` (en cualquiera de las dos direcciones).

## 🔌 API — Fase 6 (Búsqueda)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| GET | `/api/search` | Búsqueda global (juegos + usuarios) | 200 |
| GET | `/api/search/suggestions` | Autocompletado de juegos y usuarios | 200 |

`GET /api/search?q=texto` devuelve:
```json
{ "games": [ /* GameResponse */ ], "users": [ /* UserResponse */ ] }
```
`GET /api/search/suggestions?q=texto&limit=5` devuelve una lista de nombres de juegos
y usernames (machados únicos).

La búsqueda es *case-insensitive* y cubre juegos por **nombre, género, desarrollador y
publisher**, y usuarios por **username y displayName**. Query vacía → listas vacías.

## 🔌 API — Fase 7 (Chat)

### REST

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| POST | `/api/conversations` | Crear conversación (o devolver la existente) | 201 · 200 · 400 · 404 |
| GET | `/api/conversations` | Conversaciones del usuario autenticado (con último mensaje) | 200 |
| GET | `/api/conversations/between?otherUserId=` | Conversación directa o 404 si no existe | 200 · 404 |
| GET | `/api/conversations/{id}/messages?limit=` | Historial de mensajes (default `limit=100`) | 200 · 400 · 404 |
| POST | `/api/conversations/{id}/messages` | Enviar mensaje y **broadcast STOMP** (body `{content}`) | 201 · 400 · 404 |

`POST /api/conversations` es **idempotente**: si ya existe una conversación entre los dos
usuarios devuelve `200` con la existente; si no, la crea con `201`.

### WebSocket (STOMP)

- Endpoint de *handshake*: `ws://localhost:8080/ws`
- Suscribirse al historial: `/topic/conversations/{id}`
- Enviar un mensaje: tema `/app/chat/{id}` con body `{ "content": "..." }`
- El CONNECT del handshake debe autenticarse con el JWT (cabecera `Authorization: Bearer ...`)

Cada mensaje se **persiste antes** de notificar, así que quien se conecte después puede
recuperar el historial por REST. El broadcast llega a todos los suscritos al topic de la
conversación.

## 🔌 API — Fase 8 (Notificaciones)

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| GET | `/api/notifications` | Listar notificaciones del usuario autenticado (más recientes primero) | 200 · 401 |
| GET | `/api/notifications/unread-count` | Nº de no leídas `{ "count": n }` | 200 · 401 |
| GET | `/api/notifications/{id}` | Detalle de una notificación (solo destinatario) | 200 · 401 · 404 |
| PUT | `/api/notifications/{id}/read` | Marcar como leída | 200 · 401 · 404 |
| PUT | `/api/notifications/read-all` | Marcar todas como leídas | 200 · 401 |
| DELETE | `/api/notifications/{id}` | Eliminar una notificación | 204 · 401 · 404 |

Las notificaciones **no se crean por API**: las genera el sistema de forma automática y
transaccional cuando ocurre un evento:

- `FRIEND_REQUEST` → al enviar una solicitud de amistad (al destinatario)
- `FRIEND_ACCEPTED` → al aceptar una solicitud (al solicitante)
- `NEW_MESSAGE` → al enviar un mensaje en una conversación (al otro participante)

Cada notificación incluye `type`, `message` legible, `actor` (quién la causó),
`referenceId` (id del recurso relacionado) y el flag `read`. Comprobar el estado de
lectura se hace contra la propia notificación: solo el destinatario puede verla,
marcarla como leída o borrarla (un tercero obtiene `404`).

## 🔌 API — Fase 9 (Seguridad y JWT)

Toda la API (salvo los puntos abiertos abajo) exige autenticación con un JWT en la
cabecera `Authorization: Bearer <token>`.

### Asignación de identidad

Los endpoints que antes recibían `?userId=`, `senderId`, `requesterId` o `initiatorId`
ahora derivan el usuario **del token** (`sub` = id de usuario), así que el cliente solo
envía los datos de negocio:

- `POST /api/friendships` → body `{ "addresseeId": n }`
- `POST /api/conversations` → body `{ "participantId": n }`
- `POST /api/conversations/{id}/messages` → body `{ "content": "..." }`
- `PUT /api/nicknames/{targetId}` → body `{ "nickname": "..." }`

### Endpoints públicos

| Método | Ruta |
|--------|------|
| POST | `/api/users` (registro) |
| POST | `/api/users/login` (devuelve el JWT) |
| GET | `/api/users/**` |
| GET | `/api/games/**` |
| GET | `/api/search/**` |

El resto requiere `Bearer`. `POST /api/users/login` (username o email + password) devuelve:

```json
{ "token": "...", "tokenType": "Bearer", "expiresIn": 86400, "user": { /* UserResponse */ } }
```

> ⚠️ En producción define `JWT_SECRET` (≥ 32 caracteres) en `.env`. El valor de desarrollo
> (`dev-only-jwt-secret...`) solo sirve para pruebas locales.

### WebSocket

El *handshake* `/ws` y el frame STOMP `CONNECT` deben llevar la cabecera
`Authorization: Bearer <token>`; la sesión queda autenticada y el autor de cada mensaje
se toma del JWT.

## 🔌 API — Fase 10 (Configuración de cuenta)

Todos los endpoints exigen `Bearer` (el usuario se identifica por el `sub` del JWT):

| Método | Ruta | Descripción | Códigos |
|--------|------|-------------|---------|
| PUT | `/api/users/me/password` | Cambiar contraseña (body `{currentPassword, newPassword}`) | 200 · 400 · 401 |
| PUT | `/api/users/me/email` | Cambiar email (body `{email}`) | 200 · 400 · 409 · 401 |
| PUT | `/api/users/me/username` | Cambiar username (body `{username}`) | 200 · 400 · 409 · 401 |
| DELETE | `/api/users/me/account` | Desactivar cuenta (body `{password}`) | 204 · 400 · 401 |

- Cambiar contraseña exige la actual correcta; la nueva debe ser distinta y de ≥ 6 caracteres.
- Cambiar email/username a un valor ya usado por **otro** usuario devuelve `409`; conservar
  el propio valor no se considera conflicto.
- Desactivar la cuenta es un **soft-delete**: la fila persiste (`active = false`), el login
  queda bloqueado y el perfil público deja de existir (`404`).
- Las operaciones no invalidan el token anterior; pero una cuenta desactivada deja de
  aparecer en listados/búsquedas y no puede iniciar sesión.

## 🎨 Frontend — Roadmap (Compose Multiplatform)

| Fase | Módulo | Estado | Descripción |
|------|--------|--------|-------------|
| F0 | Andamiaje | ✅ Hecho | Scaffold Compose Multiplatform (Desktop JVM), Material 3, cliente Ktor, estructura |
| F1 | 🔐 Autenticación | ✅ Hecho | Registro + login, sesión con JWT (pantallas Login/Register/Home) |
| F2 | 👤 Usuarios | ✅ Hecho | Listado + búsqueda, perfil propio editable (`PUT /api/users/me`) y perfil ajeno |
| F3 | 🎮 Juegos | ✅ Hecho | Catálogo con filtros (nombre, género, desarrollador, publisher, fechas) y detalle |
| F4 | 📚 Biblioteca | ✅ Hecho | Mi colección: añadir desde el catálogo, favoritos, horas, quitar |
| F5 | 👥 Amigos | ⏳ Pendiente | Solicitudes, aceptar, bloquear |
| F6 | 🏷️ Motes | ⏳ Pendiente | Apodos privados entre amigos |
| F7 | 🔎 Búsqueda | ⏳ Pendiente | Búsqueda global + sugerencias |
| F8 | 💬 Chat | ⏳ Pendiente | Conversaciones + WebSocket (STOMP) en Compose |
| F9 | 🔔 Notif. + ⚙️ Config. | ⏳ Pendiente | Bandeja, cambiar credenciales, desactivar cuenta |

## 📜 Roadmap

- [x] **Fase 0** — Configuración base (`.env`, estructura de paquetes)
- [x] **Fase 1** — 👤 Usuarios (+ tests de integración)
- [x] **Fase 2** — 🎮 Juegos (+ tests de integración)
- [x] **Fase 3** — 📚 Biblioteca (+ tests de integración)
- [x] **Fase 4** — 👥 Amigos (+ tests de integración)
- [x] **Fase 5** — 🏷️ Motes (+ tests de integración)
- [x] **Fase 6** — 🔎 Búsqueda (+ tests de integración)
- [x] **Fase 7** — 💬 Chat (WebSocket) (+ tests de integración)
- [x] **Fase 8** — 🔔 Notificaciones (+ tests de integración)
- [x] **Fase 9** — 🔐 Seguridad (JWT)
- [x] **Fase 10** — ⚙️ Configuración de cuenta
- [x] **F0** — Frontend: andamiaje Compose Multiplatform
- [x] **F1** — Frontend: 🔐 Autenticación (registro + login + JWT)
- [x] **F2** — Frontend: 👤 Usuarios (listado, búsqueda y perfil)
- [x] **F3** — Frontend: 🎮 Juegos (catálogo con filtros y detalle)
- [x] **F4** — Frontend: 📚 Biblioteca (añadir, favoritos, horas)
- [ ] **F5–F9** — Frontend: amigos, motes, búsqueda, chat, notificaciones
- [ ] **Etapa 2** — 🐳 Docker (backend + frontend web)
- [ ] **Etapa 3** — 📨 Kafka
- [ ] **Etapa 4** — 🔴 Microservicios