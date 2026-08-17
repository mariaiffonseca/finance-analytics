# Finance Analytics — Android

Kotlin + Jetpack Compose client. See `docs/foundation/01_ANDROID_BLUEPRINT.md`
and `docs/foundation/02_ANDROID_ARCHITECTURE.md` for the architecture this
app follows.

## Build & run

Open `android/` in Android Studio, or from the command line:

```bash
cd android
./gradlew assembleDebug
```

## Tests

```bash
cd android
./gradlew test              # unit tests (JVM)
./gradlew connectedAndroidTest   # instrumented/Compose tests — needs a connected device or emulator
./gradlew lintDebug
```

## Analytics API (PR-014)

The app talks to the Analytics API from PR-013
(`analytics/src/finance_analytics/api`) over HTTP for the Insights screen.
Nothing else in the app depends on it — local transaction import, storage
and browsing all work without the API running (see
`docs/execution/07_ANDROID_INTEGRATION/PR-014_ANDROID_ANALYTICS_API_INTEGRATION.md`).

Run the API locally first (see `analytics/README.md`):

```bash
cd analytics
uv run uvicorn finance_analytics.api.app:app --reload
```

By default the app is built to reach that server through `10.0.2.2:8000` —
the Android emulator's alias for the host machine's own `127.0.0.1`, which
is where the command above binds. This is set once, in
`app/build.gradle.kts`'s `ANALYTICS_API_BASE_URL` `BuildConfig` field, and
read from a single place in code (`core/network/ApiConfig.kt`) — no URL is
hard-coded elsewhere.

To point at a different host — e.g. a physical device on the same network,
which cannot reach `10.0.2.2` — add this to the gitignored
`android/local.properties` (create the file if it doesn't exist) and rebuild:

```properties
ANALYTICS_API_BASE_URL=http://<your-machine-LAN-IP>:8000/
```

There is no deployed/production Analytics API yet; this endpoint is local
development only. Do not commit real API URLs or credentials into
`local.properties` or anywhere else in the repo.
