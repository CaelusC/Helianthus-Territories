# Helianthus Territories — Working Agreement

## Platform
- Minecraft 1.20.1, Forge 47.x (`forge_version` in `gradle.properties`).
- Build plugin: ModDevGradle Legacy (`net.neoforged.moddev.legacyforge`).
- Java 17 only (`java.toolchain.languageVersion = 17` in `build.gradle`). Do not use language features or APIs newer than Java 17.
- Mappings: official Mojang names + Parchment (`parchment_*` in `gradle.properties`).

## Package isolation
- All mod code lives under the base package declared by `mod_group_id` in `gradle.properties`.
- FTB integration code (Teams, Library, Chunks) lives only in a dedicated `integration.ftb` sub-package. No FTB imports anywhere else.
- Client-only code (anything touching `net.minecraft.client`) lives only in a `client` sub-package and is never class-loaded on a dedicated server.
- Mixins live only in a `mixin` sub-package.
- Only call FTB methods whose signatures are recorded in `docs/ftb-api-notes.md`. Verify a signature against the actual dependency jar before using it, then record it there.

## Logging
- Use one SLF4J logger per class via `com.mojang.logging.LogUtils.getLogger()`.
- Never use `System.out` / `System.err` or `printStackTrace()`.
- `info` for one-off lifecycle events, `debug` for diagnostics, `warn`/`error` for recoverable/unrecoverable problems.
- Never log inside per-tick or per-packet paths at `info` or above.
- Use `{}` placeholders, not string concatenation.

## Small slices
- One ticket = one branch = one focused change. Do not touch anything outside the ticket's scope.
- Research the relevant code before every change; no guessing at APIs or behaviour.
- `./gradlew build` must pass after every change.
- Split commits logically; commit format: `{type}: one sentence describing what was done`.
- No new dependencies or schema/data format changes unless the ticket requires it; document why when added.
- If something is unclear or missing, stop and flag it instead of inventing a solution.
