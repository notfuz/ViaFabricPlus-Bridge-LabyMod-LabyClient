# ViaFabricPlus Bridge (LabyMod 4 addon)

LabyMod replaces the multiplayer / edit-server screens, so the ViaFabricPlus buttons that normally live
there never show up. This addon puts everything into **LabyMod → Addons → ViaFabricPlus Bridge**.

## What you get

| Setting | What it does |
|---|---|
| **ViaFabricPlus menu** | Opens the real VFP version picker (Release / Beta / Alpha / Classic / April Fools / Bedrock / Favorites / search) |
| **ViaFabricPlus settings** | Opens VFP's settings screen (General, Bedrock, Authentication, Visual, Debug) |
| **ClassiCube / BetaCraft server list** | Opens the VFP server lists (not while connected) |
| **Report issues** | Opens VFP's report screen |
| **Target version** + **Apply** | Type `1.8.9`, `1.12.2`, `1.21.x`, `b1.7.3`, `c0.30`, `auto` or `native`. The field always shows the version VFP is using right now |
| **Newer / Older / Reset** | Step through every VFP version, or go back to your client's native version |
| Hotkey | Optional key that opens the VFP menu on any screen |
| Re-apply on startup | Re-applies your chosen version at launch |

Version changes are refused while you are connected to a server (same rule as ViaFabricPlus itself).

## How it works

ViaFabricPlus is a Fabric mod, so the addon never links against it. `ViaFabricPlusBridge` uses reflection
on VFP's public API (`ViaFabricPlus.api()`: `setTargetVersion`, `Screens.open*`, change listener). Because of that, the addon
keeps compiling when VFP updates. VFP 3.x/4.x (`getImpl()`) is supported for the version picker and settings screen.
"Report issues" is not public API and uses VFP internals, so it may stop working after a VFP update.

## Build

Needs JDK 21+ (the template's CI uses 25).

```
./gradlew build
./gradlew createReleaseJar     # jar ends up in build/libs/*-release.jar
```

Drop the jar into `.minecraft/labymod-neo/addons/` (or `.../labymod4/addons/`) next to ViaFabricPlus in `mods/`.

## Limitations

* The per-server version button (VFP's *Edit Server* screen) cannot be added to LabyMod's own server list.
* VFP's own settings are not mirrored into LabyMod's UI; use the *settings* button.

## Requirements
[ViaFabricPlus 26.1 - 26.3](https://modrinth.com/mod/viafabricplus)

## How it looks
![Menu Showcase](https://github.com/notfuz/ViaFabricPlus-Bridge-LabyMod-LabyClient/blob/main/Menu%20Showcase.webp?raw=true)
