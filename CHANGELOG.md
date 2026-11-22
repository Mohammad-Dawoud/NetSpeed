# Changelog

NetSpeed uses [Semantic Versioning](https://semver.org/).

## [Unreleased]

- Prepare GitHub Release automation, reproducible release signing, and public documentation.

## [0.1.0] — Planned preview, not published

- Real-time Android status-bar speed icon, with dominant download/upload direction indicator.
- Enlarged, readable speed number with reserved space for unit and arrow.
- Notification shows individual download and upload rates: e.g. `Down: 6 Mb/s — Up: 0 b/s`.
- Uses Android `TrafficStats` sampled with actual elapsed time, a foreground service, and simple Start/Stop controls.
- Unit tests and APK builds through GitHub Actions.

A published release does not exist until its signed APK and tag are created.
