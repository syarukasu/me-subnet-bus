# ME Subnet Bus

AE2-only cable part for power sharing and directional storage exposure between
two separate ME grids. No ACO dependency. Forge 1.20.1 and NeoForge 1.21.1.

## 0.1.0

Download the matching JAR from [GitHub Releases](https://github.com/syarukasu/me-subnet-bus/releases).
Install on BOTH client and server, together with Applied Energistics 2.

| Minecraft | Loader | Java | Tested AE2 |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.x | 17 | 15.4.10 |
| 1.21.1 | NeoForge 21.1.x | 21 | 19.2.17 |

MEサブネットバスは、2つのMEネットワークを統合せず、電力と在庫の公開経路を接続するAE2アドオンです。
電力共有・ストレージ共有・取出専用・挿入専用の4モード、方向反転、9枠の許可／除外フィルター、優先度設定に対応します。
AE2のクリエイティブタブに追加されます。必須MODはAE2だけで、ACOは不要です。
クライアントとサーバーの両方へ導入してください。

## Native AE2 implementation

- `InterfaceEndpoint` hosts AE2 `InterfaceLogic` on the interface-side node.
  It has zero stocking slots: this device exposes the network, never a local
  buffer. No local inventory or crafting jobs need to be saved.
- `SubnetStorage` extends AE2 `MEInventoryHandler`, the implementation used by
  `StorageBusPart`. Insertion, extraction, preferred-storage selection, access
  mode and partition filtering execute AE2 code, without operation overrides.
- `IStorageProvider` mounts the handler with the configured priority. AE2
  `StorageService` owns stock caching and change notifications, as with its
  interface/storage-bus connection.
- Power sharing uses the same `IEnergyOverlayGridConnection` service as AE2's
  `QuartzFiberPart`; the two grid nodes are never directly connected.

The addon retains the single-part lifecycle, mode/direction controls, settings,
models and GUI. `LinkTopology` / `ExposureGraph` remain for the specified
same-grid, cyclic and duplicate-path rejection and warnings. They do not move
or count items. A topology change disconnects the old delegate before remounting.

The former `SharedStorage` implementation, custom transfer/filter loops,
thread-local recursion guard and per-operation cache invalidation are removed.
AE2's network and storage handlers handle operation recursion themselves.

This reuses native logic classes; it does not place two hidden world blocks or
instantiate the full `StorageBusPart` with a fake adjacent block entity.

## Controls

Empty-hand right-click opens the settings screen. The AE2 network tool remains
supported. Select any of the four modes directly, swap host/adjacent cable roles,
and edit priority and filters in one screen. Green identifies the storage-bus
side; cyan identifies the interface side. Settings apply on the server, with the
same permission and distance checks as the existing menu. Wrench shortcuts remain.

## Source references

- AE2 Forge 15.4.10: https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/forge/v15.4.10
- AE2 NeoForge 19.2.17 (compiled dependency inspected for the same API).
- `appeng.helpers.InterfaceLogic`
- `appeng.parts.storagebus.StorageBusPart`
- `appeng.me.storage.MEInventoryHandler`
- `appeng.parts.networking.QuartzFiberPart`

## Verification

Use JDK 21 to launch Gradle; Forge compiles/runs with the Java 17 toolchain.

```powershell
.\gradlew.bat :core:test :forge-1201:runGameTestServer :neoforge-1211:runGameTestServer --no-daemon
.\gradlew.bat :forge-1201:build :neoforge-1211:build --no-daemon
```

GameTests place real AE2 grids. `native_storage_bus_parity` compares this part
against an actual AE2 interface block and storage bus using identical stock,
items/fluids, simulated/actual operations, filters, three storage modes and
amounts up to `Long.MAX_VALUE`. Other cases cover six faces, power-only mode,
grid separation, cyclic/same-grid rejection and saved/drop settings.

GameTests are not visual acceptance or third-party storage compatibility proof.
The dev.4 GUI polish uses native AE2 item icons, segmented mode/filter selectors,
colored role labels, inset slots and a status line. It does not alter part textures,
existing menu action IDs, permissions or storage behavior and has not been deployed to production.
Two additive menu actions select include/exclude explicitly, so repeated clicks do
not toggle the requested setting back.

`menu_controls` checks server-side GUI actions, preservation of inventory and
settings, grid separation, and rejection of unauthorized, distant or stale actions.
For isolated rendering checks (Japanese, all four modes and both directions):

```powershell
.\gradlew.bat :forge-1201:runClient -PvisualTest -PmenuSmoke --no-daemon
.\gradlew.bat :neoforge-1211:runClient -PvisualTest -PmenuSmoke --no-daemon
```

These commands require a development world named `SubnetQA` in that platform's
`run/saves`. Screenshots are stored under `run/screenshots`; the client exits when
finished. The render fixture does not send actions to a server or modify stock.

## Dedicated part artwork (dev.5)

Four dedicated 16x16 textures replace the temporary vanilla/AE2 texture references.
The plate has a white/gray frame, green storage face, cyan interface face and a
lavender power-only face. Extract/insert modes add opposite white pixel arrows on
the green face. Reversal swaps both faces, with the same design on placed parts
and inventory items. Selection bounds follow the new 8x8x3 plate geometry.

`tools/Build-Models.ps1` resamples the generated source atlas in `docs/art/` and
rebuilds all eight placed/item variants. Network logic and saved settings are unchanged.
Use `-PvisualTest -PartSmoke` with either `runClient` task to render all item variants
and a placed-cable scene. This fixture modifies only the isolated `SubnetQA` dev world.
