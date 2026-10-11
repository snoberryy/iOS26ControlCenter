# iOS 26 Control Center — Android Kotlin starter

This is a native Kotlin starter project for an iOS 26-inspired Control Center overlay.

## Included
- Liquid-glass-inspired dark panels
- A small top-right handle to open the overlay
- Brightness slider (requires Modify system settings access)
- Volume slider
- Flashlight tile (requires camera permission)
- Wi-Fi/Internet settings panels, Bluetooth settings, Airplane Mode settings, Display settings, and Settings shortcuts
- Basic fade animation

## Important limitations
- Android prevents ordinary third-party apps from replacing the system Control Center or intercepting the actual status-bar swipe. The app uses a small handle immediately below the status bar instead.
- On Android 10+, ordinary apps cannot directly turn Wi-Fi on/off; the tile opens Android's Wi-Fi panel.
- Bluetooth and airplane mode tiles open the relevant system settings because direct toggling is restricted.
- The music card is a visual placeholder; true media-session integration is a separate feature.
- This is a starter project, not a pixel-perfect Apple implementation. Layout and visual behavior need testing on your exact device.
- The project uses Android Gradle Plugin 8.6.1 and Kotlin 2.0.21; build with a compatible Gradle/JDK 17 setup and Android SDK 35.

## Build on a computer with Android Studio
1. Open this folder in Android Studio.
2. Let Gradle sync and install any requested SDK components.
3. Connect your phone with USB debugging, or build an APK via Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install `app/build/outputs/apk/debug/app-debug.apk`.

## Build on Android only
A full Android Gradle build on a phone is possible but can be difficult due to RAM/storage and SDK setup. Use an Android IDE that supports Gradle projects, or Termux with JDK 17, Gradle, and Android SDK command-line tools. Open this folder in the IDE and run the Gradle task `assembleDebug`. You may need to install compatible SDK/build-tools packages first.

## First run
1. Install and open the app.
2. Tap "Set up and start".
3. Allow "Display over other apps".
4. Allow "Modify system settings".
5. Allow camera permission for the flashlight.
6. Return to the app and tap "Set up and start" again if needed.
7. Swipe down on the small handle at the top-right, just below the status bar.

## Package
`com.snoberry.ios26cc`
