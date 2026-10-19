# ME Subnet Bus 0.1.0 release validation - 2026-10-19

The incoming dev.5 JAR hashes matched the handoff. Release preparation changes
only version metadata, project links/logo references, build portability and
publication documentation. Trailing whitespace/blank lines were normalized in
16 pre-existing files for the initial git diff check. No Java, model, storage,
power or GUI logic changed.

## Current checks

- JDK 21 Gradle launcher; Forge Java 17 / NeoForge Java 21 toolchains.
- clean :core:test :forge-1201:build :neoforge-1211:build
  :forge-1201:runGameTestServer :neoforge-1211:runGameTestServer
  --no-daemon --no-build-cache: BUILD SUCCESSFUL.
- Core: 3 tests; Forge: 13 required GameTests; NeoForge: 13 required GameTests.
  Zero failures, errors or skips in the XML reports.
- Both archives declare version 0.1.0, the storage-face icon and logoBlur=false.
- Four packaged PNGs are 16x16 and hash-identical to source resources.
- Eight item and eight placed model variants are present on both loaders.
- No runtime-test classes, docs or source atlas are packaged in either JAR.

## Artifacts

| Platform | SHA-256 |
| --- | --- |
| Forge 1.20.1 | 8EDCFD1453E2FFC60630933809D39C5F0FB71CA85AB82AABE0FE05901365D916 |
| NeoForge 1.21.1 | BB300A223C57B4AE4AC4E071FE21AF0F3C30BD9E8A43DD2058CAD456697E3682 |

## Boundaries

Prior isolated GUI/model evidence: verification-2026-10-19-art.md and
verification-2026-10-19-gui-dev4.md. No new graphical acceptance claim is made
for this packaging revision. Complete production-modpack compatibility remains
unverified. No live mods, server launchers or saved user worlds were modified.
