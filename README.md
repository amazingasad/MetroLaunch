# MetroOS — a MeeGo × Metro Launcher OS for Android

A custom Android launcher that behaves like an operating system: a live-tile
start screen, a horizontal task switcher, system-wide MeeGo edge swipes, a
built-in browser, dialer, messaging, weather, notes and calculator, plus
three native Jetpack Compose games (Chess with minimax AI, 2048, Tetris).

100% Kotlin + Jetpack Compose. No local build required — GitHub Actions
compiles the APK in the cloud and uploads it as an artifact.

---

## 1. Repository layout

```
android/
├── .github/workflows/build-apk.yml   # CI/CD: build + artifact + release
├── design/                           # High-fidelity SVG UI mockups (Phase 0)
│   ├── 01_home_start_screen.svg
│   ├── 02_task_switcher.svg
│   ├── 03_quick_settings.svg
│   └── 04_in_app_surfaces.svg
├── settings.gradle.kts
├── build.gradle.kts
└── app/
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/                      # themes, accessibility config, icon
        └── java/com/metroos/launcher/
            ├── MainActivity.kt           # shell navigation + edge gestures
            ├── MetroApp.kt
            ├── DesignPreview.kt          # Compose preview renderers
            ├── data/                     # AppRepository, TileStore, RecentsStore
            ├── service/                  # EdgeGestureService, GestureRouterActivity
            ├── shell/                    # Home, Drawer, Switcher, QuickSettings, AppHost
            ├── apps/                     # Browser, Dialer, Messages, Weather, Notes, Calculator
            ├── games/                    # Chess, 2048, Tetris (engines + UI)
            └── ui/theme/                 # Metro theme + glassmorphism
```

## 2. Get the APK without compiling locally (GitHub Actions)

1. Create a new GitHub repository (e.g. `metroos-launcher`).
2. Copy the **contents of the `android/` folder** into the repository root
   (the workflow path `.github/workflows/build-apk.yml` must sit at the repo
   root's `.github/`).
3. Push:
   ```bash
   git init
   git add .
   git commit -m "MetroOS launcher v1.0"
   git branch -M main
   git remote add origin https://github.com/<you>/metroos-launcher.git
   git push -u origin main
   ```
4. Open the repo on github.com → **Actions** → the running
   *Build MetroOS APK* workflow → wait for the green check.
5. Scroll to **Artifacts** → download **MetroOS-debug-apk** (a zip
   containing `app-debug.apk`). Unzip, copy to your phone and install.
6. Optional: tag a release (`git tag v1.0 && git push --tags`) and the
   workflow attaches the release APK to a GitHub Release.

No local Android Studio / SDK installation is needed at any point.

## 3. Set MetroOS as your launcher

1. Install the APK.
2. Press the device **Home** button → Android asks *“Use a different app”*
   → choose **MetroOS** → **Always**.
3. The transparent shell renders over your system wallpaper.

## 4. Enable the system-wide edge gestures

The MeeGo edge-swipe compositor runs as an Accessibility Service so it can
catch swipes while third-party apps are in the foreground:

1. Open **Settings → Accessibility → Installed apps** (on Samsung:
   *Accessibility → Installed services*).
2. Find **MetroOS Edge Gestures** and toggle it **On**.
3. Confirm the *full control* dialog.
4. System-wide gestures (Android 14+ observes raw motion events; on older
   versions the in-shell gestures still work everywhere inside the launcher):
   - **Swipe up from bottom edge** → minimize app / go home
   - **Swipe down from top edge** → translucent quick-settings panel
   - **Swipe in from left/right edge** → task switcher card deck

## 5. Runtime permissions used by built-in apps

| Feature | Permission | Asked when |
|---|---|---|
| Contacts / dialer | `READ_CONTACTS`, `CALL_PHONE` | Opening Phone |
| Messaging | `READ_SMS`, `SEND_SMS` | Opening Messages |
| Call history | `READ_CALL_LOG` | Recents tab |
| Weather / browser | `INTERNET` | At install (normal) |
| App list | `QUERY_ALL_PACKAGES` | At install (normal) |

Deny any of them and the relevant app degrades gracefully with a re-request
affordance instead of crashing.

## 6. Architecture notes

- **Animation engine** — every structural motion runs on spring physics
  (`Animatable`, `animateDpAsState`, `animateTo` with `spring(...)`). Presets
  live in `util/Springs.kt`; card dismissal derives damping from fling
  velocity (`velocityDamping`).
- **Edge gestures** — `util/EdgeGestures.kt` is a non-consuming pointer
  detector used inside the shell; `service/EdgeGestureService.kt` performs
  the same detection system-wide via Accessibility raw motion events and
  routes to `GestureRouterActivity`, a transparent overlay activity.
- **Live tiles** — `data/TileStore.kt` persists the grid; `shell/LiveTile`
  performs the periodic 3D flip (`rotationY` through 180° with mirrored
  back-face rendering so text never appears reversed). Long-press resizes
  (1x1 / 2x1 / 2x2) or removes a tile.
- **Games** — pure-logic classes (`ChessEngine`, `Game2048Logic`,
  `TetrisLogic`) are framework-free and unit-testable; the Compose UIs only
  render state and feed input, so there are no recomposition-driven state
  mutation loops.
- **Memory** — contact/SMS/weather loads cap result counts and use SQL-side
  sorting; the WebView is destroyed on disposal; ticker coroutines are
  scoped to `LaunchedEffect` so they cancel with the composition.

## 7. Automated audit

The CI workflow runs `gradle lint`; the Kotlin sources were reviewed for:
recomposition loops (all tickers scoped to `LaunchedEffect`, snapshot state
read at draw time), leaked listeners (`RecentsStore` listeners removed in
`DisposableEffect`), unhandled exceptions (all system-API calls wrapped in
`runCatching` with safe defaults), and WebView lifecycle (`onPause`/`destroy`
on dispose).

## 8. Customizing

- Default tiles & colors: `data/TileStore.kt → defaultTiles()`.
- Accent palette: `ui/theme/Theme.kt`.
- AI strength: `games/ChessGame.kt` → `bestMove(..., depth = 3)` (raise for
  a stronger but slower opponent).
- Weather location: `apps/WeatherRepository.kt → fetchForecast(latitude, longitude)`.
