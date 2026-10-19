# Dedicated textures - 0.1.0-dev.5

## Changes

- Four dedicated 16x16 PNG textures (3,321 bytes total), generated atlas and reproducible model/texture preparation script.
- White/gray frame, green storage face, cyan interface face, lavender power-only face.
- White pixel-arrow geometry distinguishes extract and insert modes on the green face.
- Eight item variants and eight placed variants use the same artwork and reversed roles.
- Inventory view faces the storage side by default. Item GUI lighting is explicitly front-lit.
- Geometry and selection bounds now describe an 8x8x3 thin plate. Network, storage and settings contracts are unchanged.

## Checks

- Forge 47.4.20 / AE2 15.4.10 build and all 13 GameTests passed.
- NeoForge 21.1.247 / AE2 19.2.17 build and all 13 GameTests passed.
- Both development clients rendered all eight saved item states and the actual placed-cable scene.
- The render fixture checks client item state and baked south-face texture against the expected variant.
- Screenshots are in `screenshots/dev5-{forge,neoforge}-{items,placed}.png`.
- No GameTest classes or source atlas are present in either release JAR.
- Production instances were not changed. This is visual/runtime evidence in isolated AE2 development environments, not a full modpack compatibility claim.

## Candidate JARs

| Platform | Bytes | SHA-256 |
| --- | ---: | --- |
| Forge 1.20.1 | 62480 | F81B85597C8A6B6895A8978CA63C9A2750E2E5E146260B6644028B1CB18DBAED |
| NeoForge 1.21.1 | 62097 | AEE4FE13CC07F766EB0CE0E28C71DADEDFF670D5AFA72EA017C11CC989552ACC |

See `art/README.md` for generated-art provenance and the generation brief.
