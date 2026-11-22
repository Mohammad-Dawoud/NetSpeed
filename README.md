# NetSpeed

NetSpeed measures the combined upload and download speed across the phone using Android’s `TrafficStats` counters. Tap **Start** to sample once per second while the phone is awake and show the result in an ongoing notification. Tap **Stop** to remove the notification and end sampling. The initial build targets Android 12 (API 31), including the Galaxy S10 on One UI 4.1.

## Build and download

1. Push this project to a GitHub repository with Actions enabled. The push starts **Build debug APK**. You can also run it from the repository’s **Actions** tab. [GitHub guide to pushing a local project](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)
2. Sign in to GitHub and open **Actions → Build debug APK**. Select the latest successful run and download **NetSpeed-debug** from **Artifacts**. [GitHub artifact download guide](https://docs.github.com/en/actions/how-tos/manage-workflow-runs/download-workflow-artifacts)
3. Extract the ZIP and open `app-debug.apk` on your phone. If prompted, allow **Install unknown apps** for the browser or file manager you used, then tap **Install**.
4. Open **NetSpeed** and tap **Start**. Allow notifications if prompted.

GitHub’s runner supplies Java and the Android SDK. The repository includes the Gradle wrapper.

## Status bar behavior

NetSpeed draws the rounded numeric reading into a small, monochrome notification icon. The icon uses `k` for kbit/s and `M` for Mbit/s. Android and One UI control the icon’s size and visibility and may hide it or show it differently. Swipe down to read the speed in full units in the notification. If you see a notification count or no icon, check **Settings → Notifications → Advanced settings → Show notification icons**. [Samsung notification settings](https://www.samsung.com/us/support/answer/ANS10002521/)
