# Contrato del RESTful API — OsoTerra IoT (para el equipo de backend)

Este documento define el contrato que la **aplicación móvil** espera del **RESTful API
(Spring Boot)**. La app está construida sobre repositorios *mock* que replican estas
respuestas; al implementar estos endpoints, la app cambia de datos falsos a reales sin
modificar la interfaz de usuario.

Cubre las Technical Stories **US43** (autenticación), **US44** (fincas y parcelas),
**US45** (telemetría), además de alertas, cultivos y suscripción. Alineado con los
bounded contexts del informe (capítulo IV).

## Convenciones generales

- **Base URL sugerida:** `/api/v1/`
- **Formato:** JSON en request y response (`Content-Type: application/json`).
- **Autenticación:** `Authorization: Bearer <accessToken>` en todos los endpoints salvo
  login, registro y recuperación de contraseña.
- **Idioma:** la app envía `Accept-Language: es-419`; los mensajes dirigidos al usuario
  se devuelven en ese idioma, con inglés por defecto (US52).
- **Errores:** cuerpo uniforme

  ```json
  { "timestamp": "2026-09-07T08:15:00Z", "status": 400, "error": "Bad Request", "message": "La superficie debe ser mayor a cero", "path": "/api/v1/plots" }
  ```

- **Fechas:** ISO-8601 con zona (`2026-09-07T08:15:00-05:00`).
- **Paginación:** parámetros `page` (0-based) y `size`; respuesta con `content`,
  `page`, `size`, `totalElements`, `totalPages`.

---

## 1. Autenticación e identidad (US43, EP02)

### POST `/auth/login`
Request:
```json
{ "email": "productor@osoterra.com", "password": "secreta123" }
```
Response `200`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsIn...",
  "expiresIn": 3600,
  "user": { "id": "u-001", "fullName": "Juan Pérez", "email": "productor@osoterra.com", "role": "PRODUCER" }
}
```
`401` si las credenciales son inválidas (sin revelar cuál dato falló).

### POST `/auth/register`
Request (productor US09 / asesor US10):
```json
{
  "fullName": "Juan Pérez",
  "email": "productor@osoterra.com",
  "password": "secreta123",
  "role": "PRODUCER",
  "licenseNumber": null
}
```
`role`: `"PRODUCER"` | `"ADVISOR"`. Para `ADVISOR`, `licenseNumber` es obligatorio.
Response `201`: mismo cuerpo que login (crea la cuenta y devuelve sesión), o `409` si el
correo ya existe.

### POST `/auth/password-reset`
Request: `{ "email": "productor@osoterra.com" }`
Response `200` siempre (no revela si el correo existe). Envía el enlace por correo (US12).

### GET `/auth/me`
Devuelve el usuario de la sesión actual (para restaurar sesión al abrir la app).
Response `200`: objeto `user` como arriba. `401` si el token no es válido.

---

## 2. Fincas (US44, EP04 — Farm Management)

### GET `/farms`
Response `200`: lista de fincas del usuario.
```json
[ { "id": "f-001", "name": "Finca San Isidro", "department": "Lambayeque", "province": "Chiclayo", "district": "Pomalca" } ]
```

### POST `/farms`  (US18)
Request:
```json
{ "name": "Finca San Isidro", "department": "Lambayeque", "province": "Chiclayo", "district": "Pomalca" }
```
Response `201`: la finca creada con su `id`.

---

## 3. Parcelas (US44, EP04 / EP06)

### GET `/plots`  (US36 — tablero del productor)
Response `200`: parcelas del usuario con su estado de salinidad actual.
```json
[
  {
    "id": "p-001",
    "name": "Parcela El Molino",
    "farmName": "Finca San Isidro",
    "areaHectares": 2.5,
    "crop": { "id": "c-arroz", "name": "Arroz", "thresholdDsPerM": 3.0, "toleranceClass": "Moderadamente sensible" },
    "salinityLevel": "CRITICAL",
    "lastEcDsPerM": 4.8,
    "lastReadingAt": "2026-09-07T08:15:00-05:00",
    "deviceOnline": true
  }
]
```
`salinityLevel`: `NORMAL` | `WATCH` | `WARNING` | `CRITICAL` | `NO_DATA`.
`crop`, `lastEcDsPerM`, `lastReadingAt` pueden ser `null`.

### GET `/plots/{id}`  (US26 — estado actual)
Response `200`: una parcela con la misma forma que arriba. `404` si no existe.

### POST `/plots`  (US19 + US20)
Request:
```json
{ "farmId": "f-001", "name": "Parcela El Molino", "areaHectares": 2.5, "latitude": -6.77, "longitude": -79.76, "cropId": "c-arroz" }
```
Reglas: `areaHectares` > 0 (si no, `400`); `cropId` opcional; respeta el cupo del plan
(si se excede, `409`, US16). Response `201`: la parcela creada.

### PATCH `/plots/{id}/crop`  (US20 — asignar/cambiar cultivo)
Request: `{ "cropId": "c-maiz" }`
Response `200`: la parcela actualizada (conserva el histórico de lecturas).

### DELETE `/plots/{id}`  (US22 — dar de baja)
Da de baja la parcela y libera el cupo del plan. Response `204`.
`409` si tiene un dispositivo aún vinculado.

---

## 4. Telemetría (US45, EP06 — Soil Monitoring)

### GET `/plots/{id}/readings?from=&to=&page=&size=`  (US27 — histórico)
`from`/`to` en ISO-8601 (opcionales). Response `200` paginada:
```json
{
  "content": [
    { "id": "r-1", "timestamp": "2026-09-07T06:00:00-05:00", "ecDsPerM": 3.8, "humidityPct": 24.5, "temperatureC": 22.1 }
  ],
  "page": 0, "size": 50, "totalElements": 240, "totalPages": 5
}
```
`ecDsPerM` es el valor **compensado a 25 °C** (la compensación la hace el Edge Service, US48).

### GET `/crops`  (US21 — catálogo de cultivos)
Response `200`:
```json
[ { "id": "c-arroz", "name": "Arroz", "thresholdDsPerM": 3.0, "toleranceClass": "Moderadamente sensible" } ]
```

---

## 5. Alertas (EP07 — Alerting and Notifications)

### GET `/alerts`  (US32 — centro de notificaciones)
Response `200`: alertas del usuario, de la más reciente a la más antigua.
```json
[
  { "id": "a-001", "plotId": "p-001", "plotName": "Parcela El Molino", "severity": "CRITICAL", "message": "La salinidad superó el umbral del arroz (3,0 dS/m).", "createdAt": "2026-09-07T08:16:00-05:00", "acknowledged": false }
]
```
`severity`: `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`.

### POST `/alerts/{id}/acknowledge`  (US33 — reconocer)
Response `200`: la alerta con `acknowledged: true` y la fecha/usuario del reconocimiento.

### POST `/alerts/{id}/actions`  (US34 — acción correctiva) — *futuro*
Request: `{ "type": "RIEGO_DE_LAVADO", "date": "2026-09-07" }`

### Notificaciones push (US31)
El backend integra un servicio push (FCM). El cliente registrará su token con:
`POST /devices/push-token  { "token": "<fcm-token>", "platform": "ANDROID" }` *(por definir)*.

---

## 6. Suscripción (EP03 — Subscriptions) — *para US15*

### GET `/subscription`
Response `200`:
```json
{ "plan": "GRATUITO", "renewsAt": null, "plotsUsed": 1, "plotsAllowed": 1 }
```

---

## Mapa de contrato ↔ capa de la app

| Endpoint | Interfaz en la app (`data/remote/api/OsoTerraApi.kt`) | Repositorio |
|---|---|---|
| `POST /auth/login` | `login()` | `AuthRepository` |
| `GET /plots` | `getPlots()` | `PlotRepository` |
| `GET /plots/{id}/readings` | `getReadings()` | `PlotRepository` |
| `GET /alerts` | `getAlerts()` | `AlertRepository` |

> Ideal: exponer **OpenAPI/Swagger (US51)** en `/swagger-ui` o `/v3/api-docs`. Con eso
> la app puede generar/validar los DTOs automáticamente y evitamos desalineaciones.
