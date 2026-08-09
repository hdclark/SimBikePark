# Sim Bike Park 🚲💥

A bright, blocky Android bike-park toy built with Kotlin and Jetpack Compose. Snap sequential isometric trail blocks together, then release three computer-controlled riders at once and discover which trail feature turns confidence into cartwheels.

## Features

- Isometric 2.5D, cel-shaded park rendered with Compose Canvas.
- 14 buildable features: table tops, gap jumps, dirt jumps, wooden skinnies, launcher logs, drops, teeter-totters, saloon doors, toilet bowls, rock rolls, crocodile pits, rocks, roots, and berms.
- Snap-aware placement: every tile has an explicit entry and exit connector, and the builder offers left, straight, or right placement relative to the current trail so consecutive pieces meet edge-to-edge without 180-degree foldbacks.
- Smooth connector geometry through turns; simulated riders follow the same route geometry that is drawn on screen.
- Three simultaneous simulated riders selected from a roster with different air, balance, nerve, technical, and speed abilities.
- Per-feature rider handling and crash susceptibility. Each of the 14 feature types has a distinct traversal motion and a distinct crash animation rather than a generic obstacle animation.
- Local named trail saves and loads using Android SharedPreferences/JSON; no account or network access required. Older saves containing impossible reverse connectors are repaired to a straight continuation when loaded.
- Emoji used as visual punctuation for riders and comedy while the terrain itself is drawn as solid-color isometric geometry.

## Building and installing

GitHub Actions is the supported build environment. The `Build Android APK` workflow installs Java 17 and Android SDK 35, runs unit tests, builds the release variant, zip-aligns it, signs it, verifies the resulting signature with `apksigner`, and uploads `SimBikePark-release.apk` as the `SimBikePark-release-apk` artifact.

The repository contains a stable **development-only** PEM signing key and certificate for CI. This is deliberate: CI is the project's supported APK build path, and a stable certificate allows APKs from later workflow runs to update an existing SimBikePark installation instead of failing because the signing certificate changed between ephemeral runners. The private key is public and must never be used for a Play Store or production release.

Development certificate SHA-256 fingerprint:

`1F:1D:73:2E:1D:8B:3A:4C:90:EF:1B:50:F4:CD:74:3A:35:A6:E8:08:50:26:34:5E:31:B0:99:D2:40:57:C4:95`

Download the `SimBikePark-release-apk` artifact from a successful GitHub Actions run, unzip it, and install `SimBikePark-release.apk` on the Android device. If a device already has a SimBikePark APK signed with a different development key, uninstall that older build once before installing the CI-signed build.
