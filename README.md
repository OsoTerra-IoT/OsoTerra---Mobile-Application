# OsoTerra — Mobile Application

Aplicación móvil nativa de **OsoTerra IoT**, la solución de monitoreo continuo y
detección temprana de la **salinización de suelos** agrícolas de la costa peruana.

La app está orientada al **productor agropecuario**: consulta en campo del estado de
sus parcelas y recepción de alertas. Consume el mismo RESTful API que la aplicación
web (ver arquitectura C4 del informe: `Mobile Application [Container: Kotlin / Android]`).

## Stack

- **Kotlin** + **Jetpack Compose** (Material 3)
- Arquitectura **MVVM + Clean Architecture** (capas `data` / `domain` / `ui`)
- **Navigation Compose**
- **Retrofit + OkHttp + kotlinx.serialization** (capa de red lista, hoy tras repos mock)
- **DataStore** (sesión/preferencias — pendiente)
- Inyección de dependencias manual (`AppContainer`)

## Estructura

```
app/src/main/java/com/osoterra/mobile/
├── core/            # utilidades transversales (red, DataResult)
├── data/            # DTOs, API Retrofit, repositorios (mock por ahora)
├── domain/          # modelos y contratos de repositorio
├── di/              # AppContainer (service locator)
└── ui/              # theme, navigation y pantallas (Compose + ViewModels)
    ├── auth/login/  # US11 inicio de sesión
    ├── dashboard/   # US36 tablero del productor
    ├── plot/        # US26 estado actual + US27 histórico
    ├── alerts/      # US32 centro de notificaciones + US33 reconocimiento
    └── profile/     # perfil y cierre de sesión
```

## Estado actual

Base funcional navegable con **datos de ejemplo** (mock). Implementado:

- **Autenticación (EP02):** login (US11), registro de productor/asesor (US09/US10),
  recuperación de contraseña (US12) y **sesión persistente con DataStore** (reabre
  la app ya autenticado).
- **Tablero del productor (US36):** parcelas con categoría de salinidad por color.
- **Detalle de parcela:** estado actual (US26), histórico (US27, gráfico en Canvas),
  cambiar cultivo (US20) y dar de baja (US22).
- **Gestión (EP04):** registro de fincas (US18), creación de parcelas (US19+US20) y
  catálogo de cultivos con umbrales (US21).
- **Alertas (EP07):** centro de notificaciones (US32) y reconocimiento (US33).
- **Perfil:** datos del usuario, accesos a fincas/catálogo y cierre de sesión.

El **contrato que la app espera del RESTful API** está en
[`docs/API-CONTRACT.md`](docs/API-CONTRACT.md) — entregarlo al equipo de backend.

## Cómo abrir

1. Abrir la carpeta en **Android Studio**.
2. Esperar el *Gradle sync* (usa Gradle 9.4.1 / AGP 9.2.1, ya cacheados).
3. Ejecutar en un emulador o dispositivo con **Android 7.0 (API 24)** o superior.

> `local.properties` apunta al SDK local y **no** se versiona.
> La `API_BASE_URL` se configura por *build type* en `app/build.gradle.kts`.

## Próximos pasos

- Conectar la capa de red al RESTful API real (US43–US45) reemplazando los repos mock
  (ver [`docs/API-CONTRACT.md`](docs/API-CONTRACT.md)).
- Notificaciones push con FCM (US31).
- Gestión de dispositivos y calibración (EP05, US23–US25).
- Preferencias de notificación (US35) y estado de suscripción (US15).
