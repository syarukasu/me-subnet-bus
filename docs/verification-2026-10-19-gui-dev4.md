# GUI polish - 0.1.0-dev.4

## Scope

- Compact mode segments with native AE2 item icons and a visible selected state.
- Separate endpoint roles, cable orientation, filter policy, priority and status.
- Explicit include/exclude menu actions 6/7 prevent repeated clicks from toggling back. Existing action IDs remain supported.
- No power, storage, topology, transfer or persistence behavior changes.

## Verification

- Both platform builds succeeded with the final explicit filter actions.
- Forge 47.4.20 / AE2 15.4.10: all 13 GameTests passed at 02:35:40.
- NeoForge 21.1.247 / AE2 19.2.17: all 13 GameTests passed at 02:35:59.
- Menu tests cover repeated explicit include/exclude requests against the server-side menu and part state.
- Both real development clients rendered eight fixture states at 854x480, GUI scale 2, Japanese language, and exited normally.
- Visually inspected Forge shared mode and NeoForge reversed insert-only mode with minimum priority and cycle warning; text and controls fit without overlap.
- Screenshots: `screenshots/dev4-forge-gui.png`, `screenshots/dev4-neoforge-gui.png`.
- Render fixtures predate the final explicit filter-action change; their layout is identical. They inject display state and do not prove manual input/network round trips.
- Release JARs contain no GameTest or render-fixture classes. Production instances were not modified.

## Artifacts

| Platform | Bytes | SHA-256 |
| --- | ---: | --- |
| Forge 1.20.1 | 62993 | 61AD7C00F6DDB9D6D18CAE4731218234E69E74CE34EBF2CF61F60F6E590010D8 |
| NeoForge 1.21.1 | 62742 | 9B4B86198239675600188A8B6A84C13EC7CA6CEF32508E8AC87EF7BA5B29BBBE |

Artifacts are in each platform's `build/libs/` directory with version `0.1.0-dev.4`.
