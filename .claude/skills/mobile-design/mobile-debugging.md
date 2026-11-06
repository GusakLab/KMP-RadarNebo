# Mobile Debugging Guide

## 🧠 MOBILE DEBUGGING MINDSET

**Key Differences:**
1.  **Native Layer:** JS code works but app crashes? Likely native (Java/Obj-C).
2.  **Deployment:** Can't just "refresh". State gets lost or stuck.
3.  **Network:** SSL Pinning, proxy settings harder.
4.  **Device Logs:** `adb logcat` and `Console.app` are ground truth.

---

## 🚫 AI DEBUGGING ANTI-PATTERNS

| ❌ Default | ✅ Mobile-Correct |
|------------|-------------------|
| "Add console.logs" | React Native DevTools / Reactotron |
| "Check network tab" | Charles Proxy / Proxyman |
| "It works on simulator" | **Test on Real Device** (HW specific bugs) |
| "Reinstall node_modules" | **Clean Native Build** (Gradle/Pod cache) |
| Ignored native logs | Read `logcat` / Xcode logs |

---

## 1. The Toolset

### ⚡ React Native & Expo

| Tool | Purpose | Best For |
|------|---------|----------|
| **Reactotron** | State/API/Redux | JS side debugging |
| **React Native DevTools** | Console, sources, breakpoints, React components, profiler (RN 0.76+; Flipper removed from RN templates in 0.74) | Default JS debugger |
| **Expo Tools** | Element inspector | Quick UI checks |

### 🛠️ Native Layer (The Deep Dive)

| Tool | Platform | Command | Why Use? |
|------|----------|---------|----------|
| **Logcat** | Android | `adb logcat` | Native crashes, ANRs |
| **Console** | iOS | via Xcode | Native exceptions, memory |
| **Layout Insp.** | Android | Android Studio | UI hierarchy bugs |
| **View Insp.** | iOS | Xcode | UI hierarchy bugs |

---

## 2. Common Debugging Workflows

### 🕵️ "The App Just Crashed" (Red Screen vs Crash to Home)

**Scenario A: Red Screen (JS Error)**
- **Cause:** Undefined is not an object, import error.
- **Fix:** Read on-screen stack trace. Usually clear.

**Scenario B: Crash to Home Screen (Native Crash)**
- **Cause:** Native module failure, memory OOM, permission used without declaration.
- **Tools:**
    - **Android:** `adb logcat *:E` (Filter for Errors)
    - **iOS:** Open Xcode → Window → Devices → View Device Logs

> **💡 Pro Tip:** If app crashes immediately on launch, almost 100% a native configuration issue (Info.plist, AndroidManifest.xml).

### 🌐 "API Request Failed" (Network)

**Web:** Open Chrome DevTools → Network.
**Mobile:** *Usually not easy to see.*

**Solution 1: React Native DevTools (Network panel on recent RN versions) / Reactotron**
- View requests in monitoring app.

**Solution 2: Proxy (Charles/Proxyman)**
- **Hard but powerful.** Shows ALL traffic, even from native SDKs.
- Requires SSL cert installed on device.

### 🐢 "The UI is Laggy" (Performance)

**Don't guess.** Measure.
- **React Native:** Performance Monitor (Shake menu).
- **Android:** "Profile GPU Rendering" in Developer Options.
- **Issues:**
    - **JS FPS drop:** Heavy calculation in JS thread.
    - **UI FPS drop:** Too many views, intricate hierarchy, heavy images.

---

## 3. Platform-Specific Nightmares

### Android
- **Gradle Sync Fail:** Usually Java version mismatch or duplicate classes.
- **Emulator Network:** Emulator `localhost` is `10.0.2.2`, NOT `127.0.0.1`.
- **Cached Builds:** Use `./gradlew clean`.

### iOS
- **Pod Issues:** `pod deintegrate && pod install`.
- **Signing Errors:** Check Team ID and Bundle Identifier.
- **Cache:** Xcode → Product → Clean Build Folder.

---

## 📝 DEBUGGING CHECKLIST

- [ ] **JS or Native crash?** (Red screen or home screen?)
- [ ] **Clean build done?** (Native caches aggressive)
- [ ] **On real device?** (Simulators hide concurrency bugs)
- [ ] **Native logs checked?** (Not just terminal output)
