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

## Building

GitHub Actions is the supported build environment. The `Build Android APK` workflow installs Java 17 and Android SDK 35, runs unit tests, builds the release variant, and uploads `app-release-unsigned.apk` as the `SimBikePark-release-apk` artifact.

The release APK is unsigned by design because this repository does not contain signing secrets. It is otherwise the complete, full-featured application; signing can be added later using GitHub Actions secrets without changing gameplay code.
