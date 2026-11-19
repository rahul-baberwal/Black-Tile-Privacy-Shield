# Black Tile — Software Privacy Display for Android

**Black Tile** is a lightweight, pure-framework Android privacy utility that allows users to place movable, resizable black tiles over any app on their screen. Inspired by on-screen privacy display concepts, Black Tile shields confidential information (e.g., private messages, passwords, balances, sensitive documents) from over-the-shoulder peeking while using everyday applications in public spaces.

---

## What's New in Version 1.1.3 (Build 6)

- **100% Full-Dark Opacity When Locked:**
  - When privacy tiles are locked, they are guaranteed 100% fully dark & opaque (`alpha = 255`), ensuring zero transparency so nothing underneath is visible to anyone nearby.
- **6 Selectable Overlay Styles & Textures:**
  - **Pitch Black (OLED):** Pure `#000000` for maximum privacy and OLED power efficiency.
  - **Matte Obsidian:** Deep charcoal `#121216` with anti-glare aesthetic.
  - **Midnight Navy:** Solid dark blue `#0B1026` matching the app visual theme.
  - **Dark Slate:** Deep graphite `#161B22` for subtle on-screen blending.
  - **Carbon Fiber Weave:** Hardware-accelerated repeating micro-weave texture over a 100% opaque solid security base.
  - **Security Dot Mesh:** Hardware-accelerated repeating micro-mesh grid over a 100% opaque solid security base.
- **Live Preview & Selection Swatches in Settings:**
  - Instant live switching between overlay styles with immediate propagation to running overlay tiles.
- **Instant UI State Synchronization:**
  - Real-time synchronization between the dashboard (`MainActivity`), active overlays (`OverlayService`), Quick Settings tile, and home-screen widget.
- **Developer Profile & Safe Keystore Management:**
  - Bundled local developer avatar for Rahul Baberwal, strict separation of signing secrets in `local.properties`.
- **Three-Screen Architecture:**
  - **Home Screen (`MainActivity`):** Streamlined dashboard with overlay status, Start/Stop toggle, Add Tile button, quick lock/show switches, and direct navigation.
  - **Settings (`SettingsActivity`):** Fine-tune editing and locked opacity sliders, remove all tiles with confirmation, reset settings to defaults, and review the privacy policy.
  - **About Developer (`AboutActivity`):** Developer profile for Rahul Baberwal, custom circular avatar with initials fallback, direct contact channels, and open-source repository link.
- **Icon-Matching Dark Visual Design:**
  - Deep dark background (`#0B0E24`), rounded card surfaces (`#141A45`), subtle borders (`#262D66`), and soft indigo accents (`#8B93FF`).
  - Built exclusively with vector drawables for crisp rendering at all screen densities.
- **Zero Third-Party Dependencies:**
  - Written in clean Kotlin against the standard Android platform SDK (`android.*`). Zero external libraries, zero AndroidX dependencies, and strictly zero network permissions (`android.permission.INTERNET` is not requested).

---

## Core Features

1. **Multiple Floating Overlay Tiles (`OverlayService`):**
   - Each tile runs in its own system overlay window (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
   - Edge-to-edge rendering with cutout mode support (`LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES`).
   - Create as many concurrent tiles as needed.
2. **Interactive Edit Mode:**
   - Move tiles freely across the display.
   - Resize via the bottom-right triangular grip handle (minimum 80dp).
   - Instant delete handle (✕) in the top-right corner.
3. **Lock Mode with Touch Passthrough:**
   - Hides handles and borders, drawing tiles at locked opacity (default 95%).
   - Adds `FLAG_NOT_TOUCHABLE` so all taps and gestures pass directly to underlying applications.
4. **Quick Settings Tile (`MaskTileService`):**
   - Toggle overlay visibility or start the service from the Android notification shade.
5. **Home-Screen Widget (`MaskWidget`):**
   - Compact 3-button widget: Toggle (Start / Hide / Show), Add Tile, and Lock / Edit.
6. **State Persistence (`MaskPrefs`):**
   - Coordinates, dimensions, opacities, lock mode, and visibility persist locally via `SharedPreferences`.

---

## Important Notice & Limitations

- **Software Mask Only:**
  Black Tile is a software graphical overlay. It cannot physically alter the viewing angles of the display hardware (unlike physical micro-louver privacy filters). Uncovered screen areas remain visible from all angles.
- **Untrusted Touch Prevention (Android 12+):**
  When underlying apps set `FILTER_TOUCHES_WHEN_OBSCURED`, Android's OS-level touch filtering may restrict touches passing through overlays.
- **Secure System Windows:**
  Android OS security prohibits system overlays from drawing over biometric prompts, the system lockscreen, or views protected by `FLAG_SECURE`.

---

## Privacy Policy

Black Tile is committed to complete user privacy:
- **No Internet Access:** The app does not declare `android.permission.INTERNET` and cannot connect to any network.
- **No Analytics or Trackers:** No analytics, tracking, or telemetry libraries are bundled.
- **Local Storage Only:** Tile coordinates and user preferences are stored strictly on-device in private application storage.

---

## Developer

Developed by **Rahul Baberwal**  
Website: [rahulbaberwal.com](https://rahulbaberwal.com)  
GitHub: [github.com/rahul-baberwal](https://github.com/rahul-baberwal)  
Repository: [Black-Tile-Privacy-Shield](https://github.com/rahul-baberwal/Black-Tile-Privacy-Shield)
