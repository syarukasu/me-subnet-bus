# Subnet verification, 2026-10-19 (JST)

Target: 0.1.0-dev.2. Production worlds and installed mods were not changed.
Only development test sources were extended in this verification run.

## Server-side results

Both Forge 1.20.1 / AE2 15.4.10 and NeoForge 1.21.1 / AE2 19.2.17
passed all 12 runtime GameTests, including the new `physical_chained_subnet`.
Reports: `forge-1201/build/gametest/results.xml` and
`neoforge-1211/build/gametest/results.xml`.

The added test uses a real vanilla chest, native AE2 storage bus and terminal,
and two subnet parts forming three separate grids. It verifies:

- All three grids are different and powered by one creative energy cell.
- Each grid consumes exactly two local channels: terminal + endpoint,
  two endpoints, or endpoint + native storage bus respectively.
- The source sees the chest's 64 iron ingots exactly once.
- Simulating extraction leaves the chest at 64.
- Extracting 16 changes the chest to 48; inserting 8 changes it to 56.
- The source grid's cached inventory updates to 56.
- Switching the first link to power-only hides remote stock, consumes no
  channel on that link, still powers the far network, and preserves all 56 iron.

The initial channel assertion incorrectly treated IGridNode.getUsedChannels()
as device-only consumption. AE2's ad-hoc routing count includes other devices.
The test now checks IPathingService.getUsedChannels() against the exact expected
total for each independent grid. No production code or channel behavior was
changed to pass the test.

Existing tests also passed: native interface/storage-bus parity, all six faces,
access modes and filters, direction reversal, immediate detachment, cycle and
same-grid rejection, settings save/load and configured-item replacement.

## Interactive client

An isolated Forge development client loaded a copied GameTest world named
SubnetQA and placed four physical chest/network setups. The first run failed
in native GLFW on the render thread (`run/hs_err_pid40456.log`,
`EXCEPTION_ACCESS_VIOLATION`, `glfw.dll+0xf8ab`). This is not a successful client
acceptance and the root cause is not established by that frame alone.

On restart, before recreating the fixtures, `/subnetqa check` observed:

| Mode | Separate grids | Both powered | Grid channels | Visible iron |
| --- | --- | --- | --- | --- |
| POWER | true | true | 1 / 1 | 0 |
| SHARED | true | true | 2 / 2 | 64 |
| EXTRACT | true | true | 2 / 2 | 64 |
| INSERT | true | true | 2 / 2 | 0 |

The power-only grids each contain one separate native device (terminal or
storage bus); the subnet link itself uses zero channels. Insert-only hides
extractable stock rather than deleting it.

This demonstrates fixture recovery after reopening the saved test world, not
general crash recovery, chunk-unload acceptance or long-duration stability.
NeoForge GUI, third-party storage and general client stability remain unverified.

## Unchanged candidate artifacts

- Forge SHA-256: `45334476A6801A7F8115C692CD7816A977160C1E2662A7BC986D5C6EBD33EC19`
- NeoForge SHA-256: `FD0305BF0CA6BCC12B12A74E22CBAC33EA65F2406F7DE4E25BD11EB72A2E2646`
