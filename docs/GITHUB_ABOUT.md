# GitHub repository About — suggested metadata

The GitHub repository's **About** description/topics are repository settings, not fields automatically filled from README. In the repository's Code page, click the small **gear** next to **About**, then enter:

**Description**

`A lightweight Android network speed indicator with live status-bar readings and separate download/upload notifications.`

**Website (optional)**

Leave blank until there is a dedicated project website or landing page. Do not use an unrelated website.

**Topics**

`android`, `kotlin`, `network-speed`, `internet-speed`, `status-bar`, `notification`, `network-monitor`, `open-source`

The repository may remain **Private** until the signed preview and download link have been validated.

## Releases versus Packages

- **Releases:** where the signed APK `NetSpeed-v0.1.0.apk` should be published after release signing is configured.
- **Packages:** can stay empty. GitHub Packages is a package registry (such as Maven and containers), not the intended channel for standalone Android APK downloads.
- Do **not** publish a dummy package to make the Packages box appear populated.

See [RELEASING.md](RELEASING.md) for the signing and tag steps.
