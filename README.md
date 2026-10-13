# Helianthus-Territories

A mod made for the Helianthus Minecraft community. It closely relies on the mod FTB Chunks.

## Prerequisites

- Java 17 (JDK). The Gradle toolchain is pinned to 17; newer JDKs are not supported for compiling.
- No separate Gradle install is needed — use the bundled wrapper (`./gradlew`, or `gradlew.bat` on Windows).

Target platform: Minecraft 1.20.1, Forge 47.x, built with [ModDevGradle Legacy](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md).

## Building

```sh
./gradlew build
```

The mod jar is written to `build/libs/`.

## Testing

```sh
./gradlew test
```

## Development commands

| Command | What it does |
| --- | --- |
| `./gradlew runClient` | Launches a dev Minecraft client with the mod loaded |
| `./gradlew runServer` | Launches a dev dedicated server (`--nogui`) |
| `./gradlew runGameTestServer` | Runs all registered game tests, then exits |
| `./gradlew runData` | Runs data generators into `src/generated/resources/` |
| `./gradlew clean` | Deletes build output |
| `./gradlew --refresh-dependencies` | Refreshes the local dependency cache |

Run directories (`run/`, `runs/`, `run-data/`), build output and IDE metadata are git-ignored.

## Mapping names

The project uses official Mojang mapping names with Parchment parameter names. Mojang's names are covered by a
specific license; see https://github.com/NeoForged/NeoForm/blob/main/Mojang.md.
