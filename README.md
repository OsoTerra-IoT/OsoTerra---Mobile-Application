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
  asesoría (aceptar o rechazar), preferencias de notificación y consulta de la suscripción.
- **Asesor:** tablero de las parcelas de sus clientes, filtros y comparación de hasta 4 parcelas.
  No ve acciones de escritura (crear, calibrar, reconocer alertas…).
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

## Pendiente

- Notificaciones push con FCM.
- Compra y cambio de plan en la suscripción (hoy solo se consulta).
- Firma de release y distribución con Firebase App Distribution (el build `release` no está firmado).
- Reducir las llamadas en cascada al cargar el tablero.
