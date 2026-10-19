# ME Subnet Bus 0.1.0

Initial public release of an independent Applied Energistics 2 addon.

Connect two separate ME grids with a thin, two-sided cable part. Share power and
expose storage through native AE2 interface/storage-bus logic without merging grids.

- Four modes: power sharing, storage sharing, extract only and insert only.
- Reverse host/adjacent cable roles.
- Nine allow/exclude filter slots and storage priority.
- Settings GUI, connection diagnostics and wrench controls.
- Dedicated 16x16 textures, eight item/placed states and Japanese/English translations.
- Registered in AE2's creative tab. AE2 is the only required mod; ACO is not required.

Install the matching runtime JAR on BOTH client and server with Applied Energistics 2.

| Minecraft | Loader | Java | Tested AE2 |
| --- | --- | --- | --- |
| 1.20.1 | Forge 47.4.x | 17 | 15.4.10 |
| 1.21.1 | NeoForge 21.1.x | 21 | 19.2.17 |

## Scope

AE2 owns item/fluid operations, inventory accounting and energy sharing. The
addon does not provide BigInteger storage, remote chunk loading or a new crafting
planner. Same-grid, cyclic and duplicate storage paths are rejected.

## Validation

- Clean build with --no-build-cache on both loaders.
- Three core unit tests and all 13 required GameTests on each loader passed.
- JAR metadata, four 16x16 textures, all eight placed/item model variants and
  test/source-atlas exclusion checked.
- Isolated GUI and eight-state rendering evidence from dev.5 is retained in docs.
  Release preparation changes version/metadata/build portability only; no Java
  logic or model geometry changed. Graphical tests were not repeated for 0.1.0.

Isolated development checks do not establish compatibility with every third-party
storage mod or complete modpack. No TPS or speedup claim is made.

## 日本語

MEサブネットバスの初回正式版です。ネットワークを分離したまま、電力共有と方向付きの在庫公開を1つのケーブルパーツで行えます。
4モード、方向反転、9枠の許可／除外フィルター、優先度、日本語GUIに対応します。
専用16x16テクスチャを同梱。実際のアイテム／液体の操作と会計はAE2本体の処理を使います。
Forge 1.20.1とNeoForge 1.21.1を用意しています。
