# Contrato del RESTful API que consume la app

La app ya no espera un contrato propio: consume el **RESTful API de OsoSense** tal como lo
implementa el Backend (Spring Boot, repositorio `OsoTerra---Backend`). La documentación
completa y navegable está en Swagger UI del Backend: `http://localhost:8080/swagger-ui.html`.

## Convenciones

- **Base URL:** `/api/v1/` (`BuildConfig.API_BASE_URL`). Por defecto el Backend desplegado en Railway,
  `https://osoterra-backend-production.up.railway.app/api/v1/`; para un Backend local desde el emulador:
  `./gradlew assembleDebug -Posoterra.apiUrl=http://10.0.2.2:8080/api/v1/` (solo el build `debug` admite HTTP).
- **Autenticación:** `Authorization: Bearer <token>` que devuelve `POST /auth/signin`. Son públicos
  `/auth/**`, `GET /crops` y `GET /subscription-plans`.
- **Errores:** `{ "message": "...", "fieldErrors": { "campo": "detalle" } }` con 400, 401, 403, 404 o 409.
  `data/remote/RemoteSupport.kt` los traduce a mensajes en español (401, 403, 404, 409 y el mensaje del Backend).
  Las pantallas de listas distinguen ese error de una lista vacía y ofrecen «Reintentar».
- **Fechas:** `LocalDateTime` de Java sin zona (`2026-09-30T08:00:00`).

## Endpoints que usa la app

Todos están declarados en `data/remote/api/OsoTerraApi.kt`. Los usan los repositorios de
`data/remote/RemoteRepositories.kt`.

| Bounded context | Endpoint | Uso en la app |
|---|---|---|
| IAM | `POST /auth/signin`, `POST /auth/signup`, `POST /auth/password-reset-requests` | Login, registro (productor `FARMER` o asesor `ADVISOR`), recuperar contraseña |
| IAM | `GET /users/me/linked-accounts` | Productores de un asesor (vínculo aceptado) |
| IAM | `GET /advisory-links/mine`, `POST /advisory-links/{id}/acceptance`, `POST /advisory-links/{id}/revocation` | El productor ve, acepta o rechaza solicitudes de asesoría |
| Subscription and Billing | `GET /subscription-plans`, `GET /subscriptions/mine` | Planes (precio, ciclo, límite de parcelas) y plan actual con `quotaConsumed` real |
| Subscription and Billing | `POST /subscriptions`, `POST /subscriptions/{id}/cancellation` | Contratar o cambiar de plan y cancelar. El cobro (webhook de Stripe) no lo usa la app |
| Farm Management | `GET /crops`, `GET /farms/mine`, `GET /farms?ownerId=`, `POST /farms` | Catálogo de cultivos y fincas |
| Farm Management | `GET /plots?farmId=`, `GET /plots/{id}`, `POST /plots`, `POST /plots/{id}/crop`, `POST /plots/{id}/deactivation` | Parcelas |
| Farm Management | `GET /devices?plotId=`, `POST /devices`, `POST /devices/{id}/attachment` | Dispositivos |
| Soil Monitoring | `GET /soil-readings?plotId=` | Estado actual, histórico y lectura sugerida del sensor al calibrar |
| Soil Monitoring | `GET /calibration-records?deviceId=`, `POST /calibration-records` | Calibración con muestra de laboratorio |
| Salinity Alerting | `GET /salinity-alerts?plotId=`, `POST /salinity-alerts/{id}/acknowledgement`, `POST /salinity-alerts/{id}/corrective-actions` | Centro de alertas, reconocimiento y acciones correctivas |
| Salinity Alerting | `GET /notification-preferences/mine`, `PUT /notification-preferences/mine` | Preferencias de notificación |

`GET /users/me` está declarado pero la app no lo usa: el usuario sale de la respuesta de `POST /auth/signin`.
Tampoco usa los endpoints de Analytics.

## Permisos según el rol

El Backend deja que el productor dueño lea y cambie sus datos, y que el asesor con vínculo aceptado
solo los lea (si intenta escribir, responde 403). La app lo replica: con rol asesor no se muestran
crear finca, registrar dispositivo, calibrar, cambiar cultivo, dar de baja ni reconocer alertas o
registrar acciones (`ui/common/Role.kt`).

## Calibración

`POST /calibration-records` recibe el valor de laboratorio, la fecha de muestreo, el **nombre del
laboratorio** (lo escribe la persona; por defecto «Laboratorio de referencia») y
`deviceReadingAtSampling`. `GET /soil-readings` solo filtra por `plotId`, no por fecha, así que la app
descarga las lecturas de la parcela y **sugiere la lectura cruda (`rawConductivityDsM`) más cercana a la
fecha del muestreo** (contra el mediodía de ese día; la última si la fecha no es válida). La persona puede
cambiar ese valor en el campo «Lectura del sensor», que es obligatorio.

## Suscripción

- `GET /subscriptions/mine` devuelve todas las suscripciones del usuario con `quotaConsumed` (parcelas
  activas) igual en todas. El plan actual es la `ACTIVE` de mayor `quotaTotal`, que es el límite que aplica
  el Backend; la `PENDING_PAYMENT` más reciente se muestra como «pendiente de pago».
- `POST /subscriptions {subscriptionPlanId}` crea la suscripción: un plan gratuito queda `ACTIVE` y uno de
  pago `PENDING_PAYMENT` hasta el pago. No cierra la anterior, así que al activar un plan gratuito la app
  cancela la activa previa con `POST /subscriptions/{id}/cancellation`.
- **No se implementa el cobro con Stripe**: la app no inicia ni confirma pagos.
- `POST /plots` responde **409** si el productor ya tiene tantas parcelas activas como el límite de su plan;
  la app muestra un mensaje propio (es/en) en vez del texto del Backend.

## Reglas que la app replica

- **Nivel de salinidad** = lectura compensada a 25 °C / umbral del cultivo: ≤ 1 normal, > 1 y < 1,2 en
  vigilancia, ≥ 1,2 y < 1,5 alto, ≥ 1,5 muy alto (misma regla con la que el Backend gradúa las alertas;
  ver `salinityLevel` en `data/remote/RemoteSupport.kt`).
- Severidad del Backend → app: `WATCH` → en vigilancia, `WARNING` → alta, `CRITICAL` → crítica.

## Pendiente

- Guardar departamento y provincia del usuario (hoy solo la Web App los envía).
- Cobro de los planes de pago con Stripe: la app solo registra la solicitud y queda pendiente de pago.
- Notificaciones push con FCM (US31): no implementadas.
- Llamadas en cascada: el tablero hace una consulta de lecturas y dispositivos por parcela (N+1).
