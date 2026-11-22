# Releasing NetSpeed

The goal is a **signed release APK in GitHub Releases** and ordinary debug APKs in GitHub Actions. Android Studio is not necessary on the developer's computer.

## Why release signing is required

Android requires compatible signing certificates to update an installed app. The existing debug builds may have runner-generated keys that change between workflows. They are only test builds; **do not publish a debug APK as a public release**.

Keep one permanent release keystore and a secure offline backup; use it for every future update. Existing debug users may need to uninstall the debug app once to switch to this signed release.

## One-time signing setup

Generate a long-lived keystore using Java `keytool` in a trusted environment with JDK installed (it does **not** require Android Studio):

```sh
keytool -genkeypair -v -keystore netspeed-release.jks -alias netspeed -keyalg RSA -keysize 3072 -validity 10000
```

Use strong passwords, keep a backup outside the repository, and convert the keystore into **one-line Base64** in that trusted environment (Linux example: `base64 -w 0 netspeed-release.jks`). The Base64 value **is sensitive private key material**; never put it into code, issues, pull requests, or chat.

Under **GitHub → Settings → Secrets and variables → Actions**, add repository secrets:

| Name | Value |
| --- | --- |
| `NETSPEED_RELEASE_KEYSTORE_B64` | Base64 content of the release keystore |
| `NETSPEED_RELEASE_STORE_PASSWORD` | Keystore password |
| `NETSPEED_RELEASE_KEY_ALIAS` | Alias (e.g. `netspeed`) |
| `NETSPEED_RELEASE_KEY_PASSWORD` | Private-key password |

The release workflow exits without publishing if a required secret is missing. Never store an unencrypted keystore in the repository or as a public Actions artifact.

## Releasing a version

1. Merge tested changes into `main`.
2. Increase Android `versionCode` for every release and set `versionName` to the version's semantic number in `app/build.gradle.kts`. For the first preview: `versionCode = 2` and `versionName = "0.1.0"`.
3. Update `CHANGELOG.md` and wait for the debug build workflow to pass.
4. **After all signing secrets are configured**, create tag `v0.1.0` on the intended `main` commit and push the tag. The tag triggers **Publish signed release APK**.
5. The workflow validates tag/version, runs tests, creates a signed APK, verifies its signature, and uploads `NetSpeed-v0.1.0.apk` plus its SHA-256 checksum to **GitHub Releases**, marked as a preview.
6. Install the release APK and verify it on a real Android 12+ device.

Normal pushes and pull requests do not publish a release. A failing signing step does not publish an unsigned APK. Planned v0.x.y tags are automatically marked pre-release.

## Before making the repository public

- Review the repo for sensitive values.
- Choose an open-source license (e.g. MIT or Apache-2.0) and add the approved `LICENSE` text.
- Test the first signed APK and download link.
- Only then deliberately change GitHub repository visibility to Public.

## Troubleshooting

- **Wrong tag:** check `vX.Y.Z` matches `versionName = "X.Y.Z"`.
- **Missing secrets:** complete the four signing secret settings.
- **Install/update conflict:** a prior debug installation may use another signing certificate. Back up any data and uninstall debug before installing the signed release.
- **Invalid signature:** do not distribute the APK until verified.
- **Private release URL:** GitHub Releases of a private repository are not public downloads.
