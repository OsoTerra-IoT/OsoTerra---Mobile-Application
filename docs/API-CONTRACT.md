# Contrato del RESTful API que consume la app

La app ya no espera un contrato propio: consume el **RESTful API de OsoSense** tal como lo
implementa el Backend (Spring Boot, repositorio `OsoTerra---Backend`). La documentación
completa y navegable está en Swagger UI del Backend: `http://localhost:8080/swagger-ui.html`.

## Convenciones

- **Base URL:** `/api/v1/` (`BuildConfig.API_BASE_URL`; en el emulador `http://10.0.2.2:8080/api/v1/`).
- **Autenticación:** `Authorization: Bearer <token>` que devuelve `POST /auth/signin`. Son públicos
  `/auth/**`, `GET /crops` y `GET /subscription-plans`.
- **Errores:** `{ "message": "...", "fieldErrors": { "campo": "detalle" } }` con 400, 401, 403, 404 o 409.
  `data/remote/RemoteSupport.kt` los traduce a mensajes en español.
- **Fechas:** `LocalDateTime` de Java sin zona (`2026-09-30T08:00:00`).

## Endpoints que usa la app

| Bounded context | Endpoint | Uso en la app |
|---|---|---|
| IAM | `POST /auth/signin`, `POST /auth/signup`, `POST /auth/password-reset-requests` | Login, registro, recuperar contraseña |
| IAM | `GET /users/me/linked-accounts` | Productores de un asesor (vínculo aceptado) |
| Subscription and Billing | `GET /subscriptions/mine`, `GET /subscription-plans` | Pantalla de suscripción |
| Farm Management | `GET /crops`, `GET /farms/mine`, `GET /farms?ownerId=`, `POST /farms` | Catálogo de cultivos y fincas |
| Farm Management | `GET /plots?farmId=`, `GET /plots/{id}`, `POST /plots`, `POST /plots/{id}/crop`, `POST /plots/{id}/deactivation` | Parcelas |
| Farm Management | `GET /devices?plotId=`, `POST /devices`, `POST /devices/{id}/attachment` | Dispositivos |
| Soil Monitoring | `GET /soil-readings?plotId=`, `GET/POST /calibration-records` | Estado actual, histórico y calibración |
| Salinity Alerting | `GET /salinity-alerts?plotId=`, `POST /salinity-alerts/{id}/acknowledgement` | Centro de alertas |

## Reglas que la app replica

- **Nivel de salinidad** = lectura compensada a 25 °C / umbral del cultivo: ≤ 1 normal, < 1,2 en
  observación, < 1,5 advertencia, ≥ 1,5 crítico (misma regla con la que el Backend gradúa las alertas).
- Severidad del Backend → app: `WATCH` → media, `WARNING` → alta, `CRITICAL` → crítica.

## Pendiente en el Backend

- Guardar departamento y provincia del usuario (hoy solo la Web App los envía).
- Preferencias de notificación: el Backend ya expone `GET/PUT /notification-preferences/mine`; la app
  aún las guarda localmente con DataStore.
