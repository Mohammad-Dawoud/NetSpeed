# Releasing NetSpeed

The intended distribution is a **signed release APK** attached to GitHub Releases. GitHub Packages is not required for distributing this standalone Android APK.

## Why release signing matters

Android requires compatible signing certificates for in-place app updates. Existing debug builds may use different temporary runner-generated certificates, so the first signed release may require uninstalling the debug app. From then on, **keep the same release keystore for every update** and safely back it up.

## One-time signing setup

Generate a long-lived keystore in a **trusted** environment with Java `keytool` (Android Studio is not required):

```sh
keytool -genkeypair -v -keystore netspeed-release.jks -alias netspeed -keyalg RSA -keysize 3072 -validity 10000
```

Store the keystore securely with an offline backup. Convert the file to single-line Base64 locally in that trusted environment (example: `base64 -w 0 netspeed-release.jks`).

**Never share the keystore file, the Base64 contents or the passwords in a GitHub issue, pull request, repository file, or chat.**

Under **GitHub → Settings → Secrets and variables → Actions** configure:

| Secret | Meaning |
| --- | --- |
| `NETSPEED_RELEASE_KEYSTORE_B64` | Base64 keystore bytes |
| `NETSPEED_RELEASE_STORE_PASSWORD` | Keystore password |
| `NETSPEED_RELEASE_KEY_ALIAS` | Key alias (e.g. `netspeed`) |
| `NETSPEED_RELEASE_KEY_PASSWORD` | Key password |

Without all four secrets, the release workflow fails safely and publishes nothing.

## Publish a signed preview

1. Confirm the target commit is in `main` and the build/test workflow passes.
2. Increment `versionCode` and set `versionName` in `app/build.gradle.kts`. The first preview is currently configured as `versionCode = 2` and `versionName = "0.1.0"`.
3. Update [CHANGELOG.md](../CHANGELOG.md).
4. After configuring signing secrets, create and push tag `v0.1.0` for the intended commit on `main`.
5. The **Publish signed release APK** workflow validates the tag/version and ancestry, runs tests, signs and verifies the release APK, and attaches `NetSpeed-v0.1.0.apk` and a checksum to a GitHub **pre-release**.
6. Download the resulting APK to a real Android 12+ phone and verify installation and future updates.

**Do not create a version tag before signing secrets are in place** unless you deliberately want the workflow to fail. Ordinary branch pushes and PRs do not publish releases.

## Before switching repository visibility

- Review source and commit history for accidentally exposed secrets.
- Verify the [MIT License](../LICENSE) is in the repository and README reflects it.
- Test the signed APK and its checksum, then verify the release's download instructions.
- Change the repository visibility from Private to Public **only when the owner decides to publish**.

## Troubleshooting

- **Tag mismatch:** tag must match `vX.Y.Z` and Android `versionName = "X.Y.Z"`.
- **Missing secrets:** all four signing secrets are required.
- **Signature verification fails:** do not distribute; inspect the keystore and configuration.
- **Cannot update an installed debug APK:** the debug signing key is likely different; uninstall debug (back up any important data first) and install signed APK.
- **Private release link:** it is visible only to users with repository access until made public.
