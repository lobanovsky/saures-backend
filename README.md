# saures-backend

Сервис для сбора показаний счётчиков через облачный API [SAURES](https://saures.ru).  
Написан на Kotlin + Ktor. Запускается как HTTP-сервер с ежедневной автосинхронизацией.

---

## Быстрый старт

### Требования

- JDK 17+
- Gradle 8+
- PostgreSQL

### Сборка

```bash
./gradlew jar
```

Собирает fat JAR в `build/libs/saures-backend-1.0.0.jar`.

### Переменные окружения

| Переменная | По умолчанию | Описание |
|---|---|---|
| `SAURES_EMAIL` | — | Логин от личного кабинета SAURES (обязательно) |
| `SAURES_PASSWORD` | — | Пароль (обязательно) |
| `DB_URL` | `jdbc:postgresql://localhost:5456/saures` | JDBC URL базы данных |
| `DB_USER` | `saures` | Пользователь БД |
| `DB_PASSWORD` | `saures` | Пароль БД |
| `SERVER_PORT` | `8080` | Порт HTTP-сервера |
| `ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173,http://localhost:8080` | Разрешённые CORS origins через запятую |

### Запуск

```bash
export SAURES_EMAIL=your@email.ru
export SAURES_PASSWORD=yourpassword
java -jar build/libs/saures-backend-1.0.0.jar
```

При старте автоматически создаётся таблица `readings` и запускается планировщик ежедневной синхронизации:

```
Database connected. Table 'readings' ready.
Server started on http://0.0.0.0:8080
[Scheduler] Next sync at 2026-05-01T00:00 (in 5h 36m)
```

Логи пишутся в консоль и в `logs/saures.log` (ротация по 5 МБ, хранится до 10 файлов).

---

## HTTP API

Базовый URL: `http://localhost:8080`

### `GET /devices`

Возвращает список всех счётчиков по всем объектам (живые данные из SAURES API).

**Ответ `200 OK`:**

```json
[
  {
    "meterId": 131305,
    "meterName": "ГВС",
    "meterType": "Горячая вода",
    "unit": "м³",
    "state": "Ошибок нет",
    "objectLabel": "Квартира",
    "objectAddress": "Москва, 17-й проезд Марьиной рощи, д.1, кв. 94",
    "sensorSn": "48E72975EC58"
  }
]
```

---

### `GET /readings/current`

Возвращает текущие показания всех счётчиков (живые данные из SAURES API).

**Ответ `200 OK`:**

```json
[
  {
    "meterId": 131305,
    "meterName": "ГВС",
    "meterType": "Горячая вода",
    "unit": "м³",
    "state": "Ошибок нет",
    "objectLabel": "Квартира",
    "objectAddress": "Москва, 17-й проезд Марьиной рощи, д.1, кв. 94",
    "sensorSn": "48E72975EC58",
    "valuePrimary": 687.27,
    "valueExtra": ""
  }
]
```

---

### `GET /readings/current/{meterId}`

Текущие показания конкретного счётчика (живые данные из SAURES API).

```bash
GET /readings/current/131305
```

Возвращает тот же формат, что `/readings/current`, отфильтрованный по `meterId`.

**Ответ `400 Bad Request`** (если `meterId` не число):

```json
{ "error": "meterId must be an integer" }
```

---

### `POST /sync`

Запускает немедленную синхронизацию: опрашивает SAURES API и сохраняет показания в базу данных.

**Запрос:** тело не требуется.

**Ответ `200 OK`:**

```json
{
  "synced": 2,
  "readings": [
    {
      "id": 1,
      "syncedAt": "2026-04-30T18:23:40",
      "objectLabel": "Квартира",
      "objectAddress": "Москва, 17-й проезд Марьиной рощи, д.1, кв. 94",
      "sensorSn": "48E72975EC58",
      "meterId": 131305,
      "meterName": "ГВС",
      "meterType": "Горячая вода",
      "unit": "м³",
      "state": "Ошибок нет",
      "valuePrimary": 687.27,
      "valueExtra": ""
    }
  ]
}
```

---

### `GET /readings`

Возвращает историю синхронизаций из базы данных, отсортированную по убыванию времени.

**Query-параметры:**

| Параметр | По умолчанию | Описание |
|---|---|---|
| `limit` | `100` | Максимальное количество записей |
| `meter_id` | — | Фильтр по ID счётчика |

```bash
GET /readings
GET /readings?limit=50
GET /readings?meter_id=131305
GET /readings?meter_id=131305&limit=30
```

Возвращает тот же формат элементов, что `POST /sync` → `readings[]`.

---

### `GET /health`

**Ответ `200 OK`:**

```json
{ "status": "ok", "timestamp": "2026-04-30T18:23:36.014720" }
```

**Ответ `500`** (для `/devices`, `/readings/current`, `POST /sync` при ошибке):

```json
{ "error": "описание ошибки" }
```

---

## Схема базы данных

Таблица `readings` создаётся автоматически при первом запуске.

```sql
CREATE TABLE readings (
    id             BIGSERIAL PRIMARY KEY,
    synced_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    object_label   VARCHAR(255) DEFAULT '',
    object_address VARCHAR(512) DEFAULT '',
    sensor_sn      VARCHAR(64)  DEFAULT '',
    meter_id       INTEGER NOT NULL,
    meter_name     VARCHAR(255) DEFAULT '',
    meter_type     VARCHAR(255) DEFAULT '',
    unit           VARCHAR(32)  DEFAULT '',
    state          VARCHAR(255) DEFAULT '',
    value_primary  DOUBLE PRECISION NOT NULL,
    value_extra    TEXT DEFAULT ''
);
```

Поле `value_extra` — дополнительные каналы многоканальных счётчиков (тепловые, электрические), разделённые `;`.

---

## Архитектура

```
saures-backend/
├── api/                   # HTTP-клиент SAURES (Ktor Client + модели)
│   ├── SauresApiClient.kt
│   └── model/
├── config/Config.kt       # Конфигурация из переменных окружения
├── db/                    # Слой базы данных (Exposed + HikariCP)
│   ├── DatabaseFactory.kt
│   ├── ReadingsTable.kt
│   └── ReadingsRepository.kt
├── server/                # HTTP-сервер, роуты, синхронизация
│   ├── Server.kt
│   ├── ServerModels.kt
│   └── SyncService.kt
├── service/               # Логика сбора показаний из SAURES API
│   ├── AuthenticatedSession.kt
│   └── ReadingsCollector.kt
└── Main.kt                # Точка входа: запуск сервера
```
