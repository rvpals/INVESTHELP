# Building the Android APK — Troubleshooting Guide

## Quick Build Steps (Normal Case)

```
cd ANDROID_APP
build_apk.bat
```

Or from a PowerShell / cmd terminal with JAVA_HOME already set:

```powershell
$env:JAVA_HOME = "C:\Program Files\jdk-17.0.2"   # adjust to actual path
cd ANDROID_APP
.\gradlew.bat clean assembleRelease
```

Output APK: `ANDROID_APP\app\build\outputs\apk\release\investhelp_v<VERSION>.apk`

---

## Problem #1 — Wrong JDK (Most Common)

### Symptom
```
ERROR: JAVA_HOME is set to an invalid directory
```
or
```
Dependency requires at least JVM runtime version 11. This build uses a Java 8 JVM.
```

### Cause
`env.bat` sets `JAVA_HOME=C:\Program Files\jdk-17.0.2` but that directory may not exist.
The system default Java (`C:\Program Files\Java\jdk1.8.0_211`) is Java 8 — too old for Gradle 8.7.3.

### Fix — Find the actual JDK 17 path

```powershell
# Check common locations
Test-Path "C:\Program Files\jdk-17.0.2"
Get-ChildItem "C:\Program Files\Eclipse Adoptium\" -ErrorAction SilentlyContinue
Get-ChildItem "C:\Program Files\Microsoft\" | Where-Object { $_.Name -match 'jdk' }
Get-ChildItem "C:\Users\$env:USERNAME\AppData\Local\Programs\" | Where-Object { $_.Name -match 'jdk|java' }

# Check Android Studio bundled JDK (if AS installed)
Get-ChildItem "C:\Program Files\Android\Android Studio\" | Where-Object { $_.Name -match 'jbr|jre' }

# Check what java is on PATH
(Get-Command java).Source
java -version
```

Once found, update `ANDROID_APP\env.bat`:
```bat
if not defined JAVA_HOME set JAVA_HOME=C:\path\to\jdk-17
```

### Fix — Install JDK 17 if missing

Run `ANDROID_APP\install_dependency.bat` after setting the correct path in `env.bat`.
Or download manually from [Adoptium](https://adoptium.net/temurin/releases/?version=17) and install.

### Fix — Set JAVA_HOME inline when building from Claude Code

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"   # ← fill in actual path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "e:\Code\Claude_Project\INVESTHELP\ANDROID_APP"
.\gradlew.bat clean assembleRelease 2>&1
```

---

## Problem #2 — Build Hangs / No Output

### Symptom
`build_apk.bat` starts but never finishes. The background process shows very low CPU usage.

### Cause
`build_apk.bat` has `pause` commands that wait for a keypress — incompatible with background/headless execution.

### Fix
Never run `build_apk.bat` in background mode. Instead call `gradlew.bat` directly:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
cd "e:\Code\Claude_Project\INVESTHELP\ANDROID_APP"
.\gradlew.bat clean assembleRelease 2>&1
```

---

## Problem #3 — "Invalid Package" / APK Won't Install

### Cause A — Cached unsigned artifact
A failed `packageRelease` task leaves a partial unsigned APK. A subsequent non-clean build reuses it.

**Fix:** Always use `clean assembleRelease`, never just `assembleRelease` alone.

### Cause B — Wrong keystore password
The keystore is PKCS12 format. `keyPassword` must equal `storePassword` in `keystore.properties`.

**Fix:** Ensure `ANDROID_APP\keystore.properties` has:
```
storePassword=<your-password>
keyPassword=<same-password>   # PKCS12 uses one password for both
```

And `ANDROID_APP\app\build.gradle.kts` signing config has:
```kotlin
storeType = "PKCS12"
```

### Cause C — Different signing key than installed app
Old debug APKs (or APKs signed with a different key) cannot be upgraded in-place.

**Fix:** Uninstall the old app on the device first, then install the new APK.

### Verify APK signature
```powershell
# Check APK is v2/v3 signed (no .SF/.RSA = expected for minSdk 29+)
& "C:\path\to\android-sdk\build-tools\35.0.0\apksigner.bat" verify --verbose path\to\app.apk
```

---

## Problem #4 — Locked Files During Build

### Symptom
```
Unable to unlink version.properties: Invalid argument
```

### Cause
Gradle daemon or another process has `version.properties` open.

### Fix
```powershell
# Stop all Gradle daemons
cd ANDROID_APP
.\gradlew.bat --stop

# Kill any lingering Java processes
Get-Process | Where-Object { $_.Name -eq 'java' } | Stop-Process -Force
```

Then retry the build.

---

## Key Files

| File | Purpose |
|------|---------|
| `ANDROID_APP\env.bat` | Sets `JAVA_HOME` — **edit this first** when JDK path changes |
| `ANDROID_APP\keystore.properties` | Signing credentials (gitignored) |
| `ANDROID_APP\local.properties` | `sdk.dir` path to Android SDK (gitignored) |
| `ANDROID_APP\version.properties` | Auto-incremented version — do not edit manually |
| `ANDROID_APP\gradle.properties` | JVM args, proxy settings |

## Confirming Build Success

```powershell
# APK was produced and version incremented
Get-Item "ANDROID_APP\app\build\outputs\apk\release\*.apk" | Select-Object Name, LastWriteTime
Get-Content "ANDROID_APP\version.properties"
```

If `LastWriteTime` is recent and version incremented, the build succeeded.

---

## Notes for Claude Code

- **Never use `build_apk.bat` in background mode** — it has `pause` commands that hang.
- **Always set `JAVA_HOME` explicitly** before calling `gradlew.bat` — do not rely on system PATH.
- **Find the actual JDK path first** by asking the user or running the search commands above.
- **Always use `clean assembleRelease`** to avoid cached unsigned artifacts.
- Run `.\gradlew.bat --stop` before a clean build to release file locks.
- The correct JDK path must be confirmed at the start of each build session, as it may differ per machine.
