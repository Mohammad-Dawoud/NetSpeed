# NetSpeed

**A tiny Android app that shows live network speed in your status bar.** No root, account, server, or unnecessary features.

[![Build debug APK](https://github.com/Mohammad-Dawoud/NetSpeed/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Mohammad-Dawoud/NetSpeed/actions/workflows/build-apk.yml) · **Android 12+** · **Kotlin**

## Features

- **Live status-bar icon** with a large numeric speed indicator and a directional arrow: ↓ for dominant download, ↑ for dominant upload.
- **Separate notification readings**, e.g. **Down: 6 Mb/s — Up: 0 b/s**.
- **Automatically scaled units**: b/s, Kb/s and Mb/s in the notification; compact k/M icon units.
- **One-screen controls**: Start and Stop.
- **On-device only**: no accounts, analytics, backend, or data collection.

The icon displays the speed of the dominant direction, **not** the sum of download and upload. Android and One UI determine its final size and visibility.

## Download and install

Once the first signed APK is released, download it from [GitHub Releases](https://github.com/Mohammad-Dawoud/NetSpeed/releases), then open the APK on a phone with **Android 12 (API 31) or newer**.

For current testers, open [Build debug APK](https://github.com/Mohammad-Dawoud/NetSpeed/actions/workflows/build-apk.yml), select a successful run, download `NetSpeed-debug` under **Artifacts**, extract the ZIP, and install `app-debug.apk`.

> **Signing note:** debug APKs produced on separate GitHub runners can have different signatures. Android may refuse in-place upgrades between such builds or from debug to the first signed release. Back up any important app data and uninstall a debug installation if required. Once signed releases start, all updates must use the same release key.

## How it works

NetSpeed reads Android's device-wide received/transmitted byte counters through `TrafficStats`. It calculates the differences over **actual elapsed time** to estimate the recent transfer rate, sampling about once per second when the device is awake. It is **not** an internet bandwidth speed test.

The app runs a foreground service with an ongoing notification while started. Android can delay sampling in sleep mode or restrict background activity depending on device settings.

## Privacy and permissions

NetSpeed requests a **foreground service** and declares **notification** permission where applicable. It does not request root or the INTERNET permission. It reads aggregate byte counts—not packet contents, browsing history, or personal connections.

## Develop

No Android Studio installation is necessary for the GitHub Actions build. To build in a compatible Java 17 + Android SDK environment:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

Versions use [Semantic Versioning](https://semver.org/). See [Changelog](CHANGELOG.md), [Releasing](docs/RELEASING.md), and [Contributing](CONTRIBUTING.md).

**Open-source license is not finalized yet.** The repository remains private until a license is selected and the first signed release is ready.

---

*Built for one job: knowing how fast your phone is transferring data.*
