# Sim Bike Park 🚲💥

A bright, blocky Android bike-park toy built with Kotlin and Jetpack Compose. Snap sequential isometric trail blocks together, then release three computer-controlled riders at once and discover which trail feature turns confidence into cartwheels.

## Features

- Isometric 2.5D, cel-shaded park rendered with Compose Canvas.
- 14 buildable features: table tops, gap jumps, dirt jumps, wooden skinnies, launcher logs, drops, teeter-totters, saloon doors, toilet bowls, rock rolls, crocodile pits, rocks, roots, and berms.
- Rotate the heading of the next block to create winding trails.
- Three simultaneous simulated riders selected from a roster with different air, balance, nerve, and technical abilities.
- Feature-specific crash susceptibility and animated, deliberately silly crash outcomes.
- Local named trail saves and loads using Android SharedPreferences/JSON; no account or network access required.
- Emoji used as visual punctuation for riders and comedy while the terrain itself is drawn as solid-color isometric geometry.

## Building and installing

GitHub Actions is the supported build environment. The `Build Android APK` workflow installs Java 17 and Android SDK 35, runs unit tests, builds the release variant, zip-aligns it, signs it, verifies the resulting signature with `apksigner`, and uploads `SimBikePark-release.apk` as the `SimBikePark-release-apk` artifact.

The repository contains a stable **development-only** signing key for CI. This is deliberate: CI is the project's supported APK build path, and a stable certificate allows APKs from later workflow runs to update an existing SimBikePark installation instead of failing because the signing certificate changed between ephemeral runners. The key is public and must never be used for a Play Store or production release.

Development certificate SHA-256 fingerprint:

`4F:44:F3:D8:04:41:07:B8:DF:A0:D1:7A:53:F2:A6:C4:85:B0:C6:57:15:65:45:2C:1F:83:98:34:3C:67:51:AA`

Download the `SimBikePark-release-apk` artifact from a successful GitHub Actions run, unzip it, and install `SimBikePark-release.apk` on the Android device. If a device already has a SimBikePark APK signed with a different development key, uninstall that older build once before installing the CI-signed build.
