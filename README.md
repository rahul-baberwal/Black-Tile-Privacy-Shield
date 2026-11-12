# Black Tile — Software Privacy Display for Android

**Black Tile** is a lightweight, pure-framework Android privacy utility that allows users to place movable, resizable black tiles over any app on their screen. Inspired by privacy display concepts like Samsung's Privacy Display, Black Tile shields confidential information (e.g., chat conversations, passwords, balances, sensitive documents) from over-the-shoulder peeking while using everyday applications.

---

## Architecture & Technology Stack

- **Architecture:** Clean, zero-dependency Android framework implementation.
- **Language:** Kotlin.
- **UI System:** Plain `android.app.Activity` and XML layouts (`res/layout/activity_main.xml`, `widget_mask.xml`).
- **Dependencies:** **Zero third-party libraries and zero AndroidX runtime dependencies**. Built strictly on standard Android framework APIs (`android.*`, `org.json.*`).
- **Target Platform:** `compileSdk 34`, `targetSdk 34`, `minSdk 26` (Android 8.0+).

---

## Core Features

1. **Multiple Floating Overlay Tiles (`OverlayService`):**
   - Each tile runs in its own system overlay window (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
   - Flag configuration: `FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_IN_SCREEN`.
   - On Android 9.0+ (API 28+), configured with `layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` for edge-to-edge coverage.
   - Users can create and manipulate as many concurrent tiles as needed.

2. **Interactive Edit Mode (Unlocked):**
   - Custom `MaskView` renders the deep black privacy fill bounded by a high-contrast dashed white border.
   - **Move:** Dragging anywhere inside the tile repositions it across the screen, clamped safely so it cannot be dragged off-screen.
   - **Resize Handle:** A triangular grip in the bottom-right corner allows smooth resizing down to a safe minimum of 80dp.
   - **Close Button (✕):** An integrated top-right close handle instantly removes the tile.

3. **Lock Mode (Touch Passthrough):**
   - When locked, handles and borders disappear, leaving only the solid black mask.
   - The window adds `FLAG_NOT_TOUCHABLE`, allowing all touch and scroll gestures to pass directly through to whatever application is running underneath.

4. **Opacity Adjustment:**
   - Adjustable between 20% and 100% via the dashboard slider, dynamically updating the fill alpha across all active tiles.

5. **Quick Settings Tile (`MaskTileService`):**
   - Integrated Android Quick Settings tile (`TileService`) in the notification shade.
   - Single-tap starts the overlay or toggles tile visibility.
   - Automatically prompts for `SYSTEM_ALERT_WINDOW` permission if not yet granted (using modern `startActivityAndCollapse(PendingIntent)` on API 34+).

6. **Home-Screen Widget (`MaskWidget`):**
   - Compact, horizontally resizable 3-button widget (`RemoteViews`):
     - **Start / Hide / Show**: Context-aware toggle reflecting current service state.
     - **+ Tile**: Instantly spawns a new tile.
     - **Lock / Edit**: Toggles touch passthrough and visual handles.
   - Dynamically broadcasts state updates to stay in sync with the foreground service.

7. **Foreground Service & Low-Importance Notification:**
   - Runs as a persistent foreground service (`specialUse` type on API 34+ with `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).
   - Low-importance notification channel with 3 quick-action buttons: Lock/Edit, Hide/Show, and Stop.

8. **State Persistence (`MaskPrefs`):**
   - Tile coordinates, dimensions, lock state, visibility, and opacity are serialized to JSON and stored in `SharedPreferences`.

---

## Known Limitations

1. **Software Mask Only (No Physical Viewing-Angle Narrowing):**
   - Black Tile is an on-screen graphical mask. Unlike specialized hardware privacy screens (e.g., micro-louver privacy filters or hardware-switchable liquid crystal displays), it cannot physically restrict the viewing angles of the display panel itself. Anyone looking directly at uncovered portions of the screen will see them normally.
2. **Android 12+ (API 31+) Touch Block Behavior:**
   - On Android 12 and higher, Android's Untrusted Touch Prevention security policy blocks or limits certain touches passing through opaque system overlays when an underlying app requests `FILTER_TOUCHES_WHEN_OBSCURED`.
3. **Secure System Windows:**
   - By Android OS security design, `TYPE_APPLICATION_OVERLAY` windows cannot draw over secure system UI components, such as the system lock screen, biometric authentication dialogs, or `FLAG_SECURE` protected banking views.
4. **Orientation and Pixel Positioning:**
   - Tile positions are stored as pixel coordinates (`(x, y)` in px). Rotating the screen between portrait and landscape may require moving tiles to re-align them with the new display dimensions.

---

## Building and Running

1. Open the project in Android Studio (Jellyfish / Koala or newer) with JDK 17 configured.
2. Build the project using Gradle:
   ```bash
   gradle assembleDebug
   ```
3. Run on a device or emulator running Android 8.0 (API 26) through Android 14+ (API 34+).
4. On first launch, tap **"Grant Display Over Other Apps"** to authorize system overlay privileges.
5. Grant notification permissions if prompted on Android 13+.
6. Tap **"Start Overlay"** to launch your privacy display!
