# Origin-Flow 🌐⚡

> **Spatial Multi-App Context Engine for Android**  
> Bringing floating radial intent overlays and persistent context workspace stacks to Android via Accessibility Services, `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, and Jetpack Compose.

---

## 🌟 Key Features

1. **Accessibility Context Hook (`OriginFlowAccessibilityService`)**
   - Intercepts text selection, long-press gestures, and system clipboard events across any running application.
   - Extracts bounding coordinates `(x, y)` to anchor spatial overlays directly at the user's touch focus.

2. **Radial "Intent-Wheel" UI (Jetpack Compose)**
   - Dark glassmorphism design (`Color(0xDD1E1E1E)`, specular highlights, soft elevation).
   - Physics-based entrance animation using Compose `Animatable` (spring scale, alpha, rotation, and radial expansion).
   - 4 radial actions:
     - 📄 **Summarize** (270° / Top)
     - 📅 **Auto-Schedule** (0° / Right)
     - ⚡ **Solve/Extract** (90° / Bottom)
     - 💾 **Save to Stack** (180° / Left)

3. **Floating Workspace Stack (`FloatingWorkspaceCard`)**
   - Movable floating window rendered with `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
   - Smoothly collapses into a compact floating bubble puck with a live count badge.
   - Expands into a full multi-app context card displaying AI-processed output, original sources, and clipboard actions.

4. **Service-Hosted Jetpack Compose Architecture**
   - Uses a custom `OverlayLifecycleOwner` providing `LifecycleOwner`, `SavedStateRegistryOwner`, and `ViewModelStoreOwner` directly to the `ComposeView` view tree, eliminating the need for an `Activity` host.

5. **Interactive Web Simulator (`index.html`)**
   - A fully functional browser-based smartphone simulation with mock Gmail, Slack, and Chrome apps.
   - Real-time text selection detection, interactive radial wheel, draggable workspace card, and automated walkthrough tour.

---

## 📂 Project Structure

```
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── xml/accessibility_service_config.xml
│   │   └── java/com/originflow/
│   │       ├── MainActivity.kt                       # Permission check & test sandbox
│   │       └── system/
│   │           ├── overlay/
│   │           │   ├── OverlayLifecycleOwner.kt     # Compose in Service lifecycle bridge
│   │           │   └── OverlayWindowManager.kt      # WindowManager overlay orchestrator
│   │           ├── service/
│   │           │   └── OriginFlowAccessibilityService.kt # Text/gesture listener
│   │           └── ui/
│   │               ├── intentwheel/
│   │               │   ├── IntentWheelAction.kt
│   │               │   └── IntentWheelOverlay.kt    # Radial Compose UI
│   │               └── workspace/
│   │                   ├── WorkspaceItem.kt
│   │                   └── FloatingWorkspaceCard.kt # Movable & collapsible card
│   └── build.gradle.kts
├── index.html                                        # Web simulator interface
├── style.css                                         # Dark glassmorphic styles
├── app.js                                            # Web simulator interaction logic
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

---

## 🚀 Getting Started

### 1. View Web Simulator
Simply open `index.html` in your browser or run:
```bash
python -m http.server 8085
```
Visit `http://localhost:8085` to interact with the simulated smartphone.

### 2. Android Build & Deployment
Open the project in **Android Studio** and run on your device or emulator.

#### Grant Permissions via ADB:
```bash
# Grant "Display over other apps"
adb shell appops set com.originflow SYSTEM_ALERT_WINDOW allow

# Enable Origin-Flow Accessibility Service
adb shell settings put secure enabled_accessibility_services com.originflow/com.originflow.system.service.OriginFlowAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

---

## 📄 License
MIT License.
