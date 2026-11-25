# Echo Clock

Material You clock for small landscape screens (Echo Show 5 = 960x480). Kotlin + Jetpack Compose + Material 3.

* **Clock** – long-press anywhere (or sidebar "Clock" gear) for face/format settings.
* **Alarms** – exact alarms, repeat days, label, snooze, survive reboot.
* **Media** – pick any installed app, control its playback (needs Notification access once).
* **Sidebar** – Clock / Alarms / Media / clock settings.

## Add your fonts
1. Put `.ttf`/`.otf` files in `app/src/main/res/font/` (lowercase, underscores).
2. Add a `ClockFace(...)` line in `ClockFaces.kt` (example is in the comment).

## Add your alarm mp3s
Put them in `app/src/main/res/raw/` (e.g. `morning_birds.mp3`). They appear in the alarm sound picker automatically.

## Build
* Android Studio: open this folder, Build > Build APK(s).
* Or push to GitHub: the included workflow builds `app-debug.apk` (Actions > artifact).
* Install: `adb install app-debug.apk`

## Notes
* minSdk 26. Dynamic Material You colors on Android 12+; seeded fallback colors below that.
* The app also registers as a Home app, so on a dedicated device you can set it as the default launcher.
* Grant "Notification access" (Media tab button) and allow notifications for alarms.
