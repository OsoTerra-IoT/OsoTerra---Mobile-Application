# OsoTerra — Mobile Application

Aplicación móvil nativa de **OsoTerra IoT**, la solución de monitoreo continuo y
detección temprana de la **salinización de suelos** agrícolas de la costa peruana.

Está pensada para el **productor agropecuario** (consulta en campo, alertas y registro de
fincas, parcelas, dispositivos y calibraciones) y para el **asesor**, que supervisa en modo
de solo lectura las parcelas de los productores que aceptaron su vínculo. Consume el mismo
RESTful API que la aplicación web (arquitectura C4 del informe:
`Mobile Application [Container: Kotlin / Android]`).

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3), Android nativo (no es Flutter)
- Arquitectura **MVVM + Clean Architecture** (capas `data` / `domain` / `ui`)
- **Navigation Compose**
- **Retrofit + OkHttp + kotlinx.serialization** contra el RESTful API del Backend
- **DataStore** para la sesión y las preferencias
- Inyección de dependencias manual (`AppContainer`)
- Gradle 9.4.1, AGP 9.2.1, Kotlin 2.2.10; `minSdk` 24 y `compileSdk`/`targetSdk` 36
- Textos en recursos (`values/strings.xml` en español y `values-en/strings.xml` en inglés); el idioma se cambia dentro de la app

## Estructura

```
app/src/main/java/com/osoterra/mobile/
├── core/       # red (ApiClient), DataResult, idioma
├── data/
│   ├── remote/ # API Retrofit, DTOs y repositorios que llaman al Backend
│   ├── demo/   # modo de prueba: escenarios en memoria para entrevistas de usabilidad
│   └── repository/ # repositorios mock y almacenes locales (sesión, preferencias)
├── domain/     # modelos y contratos de repositorio
├── di/         # AppContainer (elige remoto, mock o demo)
└── ui/         # tema, navegación y pantallas (Compose + ViewModels)
```

## Funciones

- **Autenticación:** login, registro de productor o asesor, recuperar contraseña y sesión persistente.
- **Productor:** tablero de parcelas por nivel de salinidad, detalle con histórico (ECe), cambio de cultivo
  y baja de parcela, fincas, catálogo de cultivos, dispositivos y calibración con muestra de laboratorio
  (se puede escribir el laboratorio), alertas con reconocimiento y acciones correctivas, solicitudes de
  asesoría (aceptar o rechazar), preferencias de notificación y suscripción (ver planes, contratar,
  cambiar y cancelar).
- **Asesor:** tablero de las parcelas de sus clientes, filtros y comparación de hasta 4 parcelas.
  No ve acciones de escritura (crear, calibrar, reconocer alertas…).
- **Suscripción:** lista de planes con precio y límite de parcelas, plan actual con las parcelas usadas
  (X de N), y botones para contratar o cambiar de plan y para cancelar, siempre con confirmación y con
  estados de carga y de error («Reintentar»). Crear una parcela por encima del límite del plan muestra
  un mensaje claro en español e inglés. **El cobro con Stripe no está implementado** (ver abajo).
- **Errores de carga:** las listas muestran un error con «Reintentar», distinto del estado vacío.

## Cómo compilar y probar

Requiere JDK y el SDK de Android; `local.properties` apunta al SDK local y no se versiona.
En Windows se usa `gradlew.bat`; en otros sistemas, `./gradlew`.

| Qué | Comando |
|---|---|
| Compilar el APK de depuración | `./gradlew assembleDebug` |
| Pruebas unitarias | `./gradlew testDebugUnitTest` |
| Backend local desde el emulador | `./gradlew assembleDebug -Posoterra.apiUrl=http://10.0.2.2:8080/api/v1/` |
| Repositorios mock, sin Backend | `./gradlew assembleDebug -Posoterra.fakeApi=true` |
| Abrir ya en el modo de prueba | `./gradlew assembleDebug -Posoterra.demo=true` |

El APK queda en `app/build/outputs/apk/debug/`. También se puede abrir la carpeta en Android Studio y
ejecutar en un emulador o dispositivo con Android 7.0 (API 24) o superior.

### URL base del API

Por defecto, el Backend desplegado en Railway: `https://osoterra-backend-production.up.railway.app/api/v1/`
(`BuildConfig.API_BASE_URL`, propiedad `osoterra.apiUrl`). El tráfico HTTP solo se permite en el build `debug`.
Los endpoints que usa la app están en [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md).

El enlace «Modo de prueba» del login activa escenarios en memoria sin Backend.

Los escenarios 7 y 8 muestran la contingencia de salinidad del dispositivo (lavado automático con bomba) como si estuviera integrada: usan solo datos de prueba en memoria y el Backend real aún no envía este dato.

## Suscripción: qué hace y qué no

La app usa los endpoints que ya existen en el Backend: `GET /subscription-plans`, `GET /subscriptions/mine`
(con `quotaConsumed` real: las parcelas activas del productor), `POST /subscriptions` y
`POST /subscriptions/{id}/cancellation`. Solo el productor contrata o cancela; el asesor consulta.

- **Plan gratuito:** el Backend lo activa al instante. Como `POST /subscriptions` no cierra la suscripción
  anterior y el límite de parcelas es el mayor entre las activas, la app cancela la anterior para que el
  límite sea el del plan nuevo.
- **Plan de pago (Basic, Pro):** el Backend lo deja en `PENDING_PAYMENT` hasta que llegue el pago por su
  webhook. **El cobro con Stripe queda fuera de esta app**: el cambio llama a `POST /subscriptions`, muestra
  el resultado («pendiente de pago») y conserva el plan vigente. La persona no paga desde la app.
- **Modos mock (`-Posoterra.fakeApi=true`) y de prueba (demo):** replican esas mismas reglas en memoria
  (`data/repository/InMemorySubscriptions.kt`) con los tres planes que siembra el Backend (Free 2 parcelas,
  Basic 10, Pro 50); en el modo de prueba el plan parte del más pequeño que cabe las parcelas del escenario
  y se reinicia al cargar otro escenario. Sirve para ver el mensaje de límite: cambia a Free y crea una
  parcela de más.
- **Si se cancela el único plan activo,** el Backend deja de aplicar límite de parcelas (sin plan activo no
  hay tope). Es una regla del Backend, no de la app.

## Calibración: lectura del sensor

`POST /calibration-records` pide `deviceReadingAtSampling`, la lectura cruda del sensor al tomar la muestra.
`GET /soil-readings` solo filtra por `plotId` (no por fecha ni hora), así que la app descarga las lecturas
de la parcela del dispositivo y **sugiere la más cercana a la fecha del muestreo** (se compara contra el
mediodía de ese día, porque la fecha no trae hora; si la fecha no es válida usa la última). La sugerencia
se vuelve a calcular al cambiar la fecha, **el campo «Lectura del sensor» es editable** y, si el dispositivo
no tiene lecturas, hay que escribirla. El laboratorio también se puede escribir.

## Pasos pendientes del usuario (requieren sus credenciales)

Esto **no** está hecho en el código: la firma de release y Firebase necesitan secretos que solo tiene el
equipo, y el build `release` sigue sin firmar.

1. **Generar el keystore de release** (guárdalo fuera del repositorio; `*.jks` y `*.keystore` están en
   `.gitignore`):

   ```
   keytool -genkeypair -v -keystore osoterra-release.jks -alias osoterra -keyalg RSA -keysize 2048 -validity 10000
   ```

   Luego definir en `~/.gradle/gradle.properties` (no en el repo) `OSOTERRA_STORE_FILE`, `OSOTERRA_STORE_PASSWORD`,
   `OSOTERRA_KEY_ALIAS` y `OSOTERRA_KEY_PASSWORD`, y añadir un `signingConfigs { create("release") { ... } }` en
   `app/build.gradle.kts` que los lea y asignarlo a `buildTypes.release`.
2. **Firebase App Distribution:** crear el proyecto en la consola de Firebase, registrar la app
   `com.osoterra.mobile`, descargar `google-services.json` a `app/`, añadir los plugins `com.google.gms.google-services`
   y `com.google.firebase.appdistribution` y el bloque `firebaseAppDistribution { groups = "..." }` en `release`.
3. **Publicar** (con la CLI de Firebase iniciada con `firebase login`):

   ```
   ./gradlew assembleRelease appDistributionUploadRelease
   ```

## Pendiente

- Notificaciones push con FCM.
- Cobro de los planes de pago (Stripe): la app no cobra; el plan queda pendiente de pago.
- Firma de release y distribución con Firebase App Distribution (pasos arriba; el build `release` no está firmado).
- Reducir las llamadas en cascada al cargar el tablero.
