# Hydration Tracker

Track drinking water through the day. The repo has two clients that share the same behavior:

- a single-page web app (`index.html`)
- a native Android app in `android/` (Kotlin, Jetpack Compose, Material 3)

## Features

- Progress ring for today's intake against a daily goal (2,000 ml by default)
- One-tap logging: + Glass (250 ml) and + Bottle (500 ml), plus quick adds of 100, 200, 330, and 500 ml
- Custom amount, and drink types: Water, Sparkling, Tea, Coffee, Juice, Other
- Today's log with a time on every drink, delete, and clear-the-day (with confirmation)
- Last 7 days chart, 7-day average, days at goal, and best day
- Settings for the daily goal, glass size, bottle size, and ml / oz
- Data stays on the device. A new day starts at **local midnight**

The Android app also has:

- Drink reminders you can turn on or off, with an interval and active hours (8:00 AM–10:00 PM by default). On Android 13 and later the app asks for notification permission. Reminders are rescheduled after reboot, timezone changes, and app updates. If exact alarms aren't allowed, they still fire, just less precisely.
- A home-screen widget showing today's progress and a + Glass action

Amounts are stored in millilitres. Fluid ounces use the US fluid ounce (29.5735 ml), same as the web app.

## Web app

Open https://CyroustheGreat.github.io/hydration-tracker/ once GitHub Pages is enabled (Settings → Pages → Deploy from a branch → main / root).

Or open `index.html` in any browser. No build step and no server.

Drinks are saved in `localStorage`. The day key is the local calendar date. An earlier version used `toISOString()`, which is UTC, so the day rolled over in the late afternoon for time zones west of Greenwich (around 5 PM in Vancouver during daylight time). Entries already saved in a browser stay under the key they were written with.

## Android app

Requires JDK 17 or newer and the Android SDK (API 35 and build-tools 35). Point `ANDROID_HOME` at the SDK, or put `sdk.dir` in `android/local.properties` (that file is gitignored).

```bash
cd android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug APK is written to:

`android/app/build/outputs/apk/debug/app-debug.apk`

Install it on a device or emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Unit tests cover daily totals, ml/oz conversion, the local-date rollover (including a Vancouver evening that is already the next day in UTC), and the 7-day stats.

Turn reminders on from Settings. Allow notifications when Android asks. Add the "Hydration" widget from the home-screen widget picker.
