# 📦 Kafka local con Docker (runbook)

Setup mínimo para probar el **flujo de eventos real** entre microservicios sin instalar Kafka en Windows: se levanta solo el broker en Docker y los servicios Spring Boot se conectan desde `localhost`.

> PostgreSQL no se duplica en Docker: aquí se usa el PostgreSQL local/Supabase en `localhost:5432` (1 base por servicio).

## Qué se levanta

```bash
cd backend/deployments
docker compose up -d kafka
```

- Imagen `apache/kafka:3.9.0` en **modo KRaft** (sin Zookeeper).
- Broker en `localhost:9092`.
- Contenedor: `springsteam-kafka`.

Verificar:

```bash
docker compose ps                 # STATUS = healthy
docker inspect --format '{{.State.Health.Status}}' springsteam-kafka
Test-NetConnection localhost -Port 9092   # TcpTestSucceeded = True
```

Parar:

```bash
docker compose down
```

## Arrancar los servicios con eventos

Cada servicio necesita `EVENTS_ENABLED=true` (default en `application.yml`) y apuntar su BD al PostgreSQL local `:5432` (no `:5433`):

```powershell
# auth-service (8081)
$env:EVENTS_ENABLED='true'; $env:JWT_SECRET='dev-only-jwt-secret-cambiar-antes-de-produccion'
$env:AUTH_DB_URL='jdbc:postgresql://localhost:5432/authdb'; $env:AUTH_DB_USERNAME='postgres'; $env:AUTH_DB_PASSWORD='postgres'
java -jar .\auth-service\build\libs\auth-service-0.1.0-SNAPSHOT.jar
```

Igual para `game-service` (puerto 8082, `GAME_DB_URL`) y `library-service` (puerto 8083, `LIBRARY_DB_URL`). Env var `KAFKA_BOOTSTRAP` no hace falta (default `localhost:9092`).

## Cómo probar el pipeline de eventos

**1. Login → token**

```powershell
$login = Invoke-RestMethod -Uri http://localhost:8081/api/users/login -Method POST -ContentType "application/json" -Body '{"identifier":"usuario","password":"secret123"}'
$headers = @{ Authorization = "Bearer $($login.token)" }
```

**2. Crear un juego (game-service publica `game.created`)**

```powershell
$body = @{ name = 'Ori and the Blind Forest'; genre = 'Metroidvania' } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8082/api/games -Method POST -Headers $headers -ContentType "application/json" -Body $body
```

**3. Comprobar la proyección automática (sin INSERT manual)**

```powershell
$env:PGPASSWORD='postgres'
& "C:\Program Files\PostgreSQL\15\bin\psql.exe" -U postgres -h localhost -p 5432 -d librarydb -c "SELECT id, name, genre FROM games;"
```

Si el juego aparece → el evento cruzó Kafka y `GameProjectionService` lo aplicó.

**4. (Opcional) Ver eventos crudos:**

```powershell
docker exec springsteam-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic game-events --from-beginning --max-messages 5
```

## Troubleshooting

### `InstantiationException: No default constructor for entity ...`

Hibernate no puede instanciar la entidad porque el `data class` de Kotlin no tiene constructor sin argumentos. Fix: valores por defecto en todos los parámetros.

- `auth-user/User.kt`
- `library/LibraryEntry.kt`

### `InvalidDefinitionException ... no Creators, like default constructor` (EventEnvelope)

Jackson (JsonDeserializer de Kafka) no puede deserializar `EventEnvelope` (shared) sin constructor por defecto. Fix: valores por defecto en `type`, `occurredAt` y `payload`.

### `EVENTS_ENABLED` en tests

Los tests corren con `EVENTS_ENABLED=false` y H2 en memoria: no dependen de Docker ni Kafka.

## Consumo de recursos

- Docker Desktop + una VM WSL2: ~1–2 GB RAM.
- Solo el contenedor Kafka activo: ~300–400 MB RAM.
- Si no necesitas Kafka, no reemplaza el arranque estándar; puedes dejar Docker Desktop cerrado y seguir con `EVENTS_ENABLED=false`.