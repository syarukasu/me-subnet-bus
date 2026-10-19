# dev.3 settings GUI verification

## Scope

- Extend the existing menu, rather than adding a second settings protocol.
- Empty-hand right-click opens settings. Network tool and wrench shortcuts remain.
- Direct selection of all four modes; swap host/adjacent cable roles.
- Green storage-bus and cyan interface labels, live status, priority and filters.
- All mutation uses the existing server-side configure and permission checks.
- No change to transfer/storage algorithms, grid topology rules or part textures.
- No deployment to production, publication or changes to production options/worlds.

## Server behavior

Both Forge 47.4.20 / AE2 15.4.10 and NeoForge 21.1.247 / AE2 19.2.17
passed all 13 GameTests, including `menu_controls`.
Reports: `forge-1201/build/gametest/results.xml` and
`neoforge-1211/build/gametest/results.xml`.

The added test exercises the real menu button handler on live independent grids:

- Direct mode selection, not repeated cycling.
- Local 32 + remote 64 iron appears as 96 in shared mode, 32 in insert-only mode.
- Reversal removes the old exposure, exposes the new target exactly once,
  and keeps the menu valid through node recreation.
- Power-only mode retains power sharing and grid separation.
- All mode/direction operations leave both backing storage counts at 32 and 64.
- Filters and priority survive mode/direction changes.
- Unknown button, permission denial, excessive distance and removed part reject mutation.

The mock-player factory differs by Minecraft version; each platform constructs its
own player and passes it to the shared scenario. Assertions were not relaxed.

## Actual client rendering

The separate development test mod creates client-side menu fixtures after entering
an isolated `SubnetQA` world. It captures the actual Minecraft framebuffer and
exits the client. No test classes are packaged in the deliverable JARs.
This checks rendering; it is not a manual mouse-input or network round-trip test.

Final run completed successfully on both versions, Japanese language, GUI scale 2,
854x480 framebuffer, all four modes and both directions (8 captures per version).
Priority 2147483647, filter slot, include/exclude labels and direction labels fit.
Selected mode is disabled and underlined. Inspected captures include shared,
power-only, reversed extraction and reversed insertion.
The late NeoForge captures contain a vanilla movement tutorial toast over the
upper-right area; the retained shared-mode example is unobstructed.

- Forge capture set: `forge-1201/run/screenshots/subnet-menu-1792343052254-*.png`
- NeoForge capture set: `neoforge-1211/run/screenshots/subnet-menu-1792343093027-*.png`
- Retained examples: `screenshots/dev3-forge-gui.png`, `screenshots/dev3-neoforge-gui.png`

The first NeoForge launch stopped at first-run accessibility onboarding; the test
process was terminated before loading a world. Only its new development options
file was initialized with onboarding disabled and fullscreen disabled. The next
run completed and exited normally. This is not a fix or acceptance claim for the
earlier GLFW crash investigation.

## Artifacts

Both builds succeeded. Metadata is 0.1.0-dev.3 and neither JAR includes test classes.

| Platform | Bytes | SHA-256 |
| --- | ---: | --- |
| Forge 1.20.1 | 59617 | 1469FBCD75CD94E7DD1980AF1866401B3CEC4590A8652CFEDCE4D4876C1577DD |
| NeoForge 1.21.1 | 59332 | 468FCF480D103BF04A4D9946895F84AAA727F21133118E3A0AECFB77AA38DD37 |

## Remaining boundaries

- Live multiplayer GUI clicking and third-party storage compatibility are not
  established by these fixtures.
- The supplied texture concept image has not been substituted for game textures.
- Long-running client stability and the previous native GLFW failure remain
  outside this GUI change.
