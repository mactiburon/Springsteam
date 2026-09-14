# 🎮 Spring Boot Demo — Plataforma de Juegos

Plataforma de juegos estilo Steam construida con **Spring Boot + Kotlin + PostgreSQL (Supabase)**.
Backend 100% API REST. Objetivo de aprendizaje: Spring Boot, JPA/Hibernate, WebSockets, y más adelante
Docker, Kafka y microservicios.

## 🧱 Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin 2.3.21 (Java 21) |
| Framework | Spring Boot 4.1.1 |
| Persistencia | Spring Data JPA / Hibernate 7 |
| Base de datos | PostgreSQL (Supabase) en dev · H2 en memoria en tests |
| Seguridad | Spring Security (BCrypt) — JWT próximo (Fase 9) |
| Tests | JUnit 5 + MockMvc |

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
| 📚 Biblioteca | ⏳ Pendiente | Juegos del usuario, favoritos, horas |
| 👥 Amigos | ⏳ Pendiente | Solicitudes, bloqueos |
| 🏷️ Motes | ⏳ Pendiente | Apodos privados entre amigos |
| 💬 Chat | ⏳ Pendiente | Conversaciones + WebSocket |
| 🔔 Notificaciones | ⏳ Pendiente | Avisos al usuario |
| 🔐 Seguridad | ⏳ Pendiente | JWT (protocolo de la Fase 9) |
| ⚙️ Configuración | ⏳ Pendiente | Ajustes de cuenta |

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
│   ├── library/           # (próximo)
│   ├── friendship/        # (próximo)
│   ├── nickname/          # (próximo)
│   ├── chat/              # (próximo)
│   └── notification/      # (próximo)
└── src/test/kotlin/.../user/   # Tests de integración (MockMvc)
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
| GET | `/api/games` | Listar/buscar con filtros | 200 |
| GET | `/api/games/{id}` | Juego por id | 200 · 404 |
| PUT | `/api/games/{id}` | Actualizar juego (todos los campos) | 200 · 404 · 409 |
| DELETE | `/api/games/{id}` | Borrar juego | 204 · 404 |

**Filtros de búsqueda** (combinables, todos opcionales):
`?name=` · `?genre=` · `?developer=` · `?publisher=` · `?releaseDateFrom=AAAA-MM-DD` · `?releaseDateTo=AAAA-MM-DD`

La búsqueda es *case-insensitive* y parcial (contiene el texto), con una sola
consulta JPQL parametrizada que ignora los filtros vacíos (`IS NULL OR LIKE`).

## 📜 Roadmap

- [x] **Fase 0** — Configuración base (`.env`, estructura de paquetes)
- [x] **Fase 1** — 👤 Usuarios (+ tests de integración)
- [x] **Fase 2** — 🎮 Juegos (+ tests de integración)
- [ ] **Fase 3** — 📚 Biblioteca
- [ ] **Fase 4** — 👥 Amigos
- [ ] **Fase 5** — 🏷️ Motes
- [ ] **Fase 6** — 🔎 Búsqueda
- [ ] **Fase 7** — 💬 Chat (WebSocket)
- [ ] **Fase 8** — 🔔 Notificaciones
- [ ] **Fase 9** — 🔐 Seguridad (JWT)
- [ ] **Fase 10** — ⚙️ Configuración de cuenta
- [ ] **Etapa 2** — 🐳 Docker
- [ ] **Etapa 3** — 📨 Kafka
- [ ] **Etapa 4** — 🔴 Microservicios