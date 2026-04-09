<img src="ias.png" alt="In-Game Account Switcher Icon" width=128 height=128/>

# In-Game Account Switcher — Minecraft 26.1.1 Fork

Fork of [In-Game Account Switcher](https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher) with added support for **Minecraft 26.1.1** (Fabric).

## What's changed

- Added Minecraft **26.1.1** as a supported version (new Minecraft versioning scheme)
- Switched from Architectury Loom to **Fabric Loom 1.15-SNAPSHOT** (Architectury Loom does not support 26.1.1's unobfuscated jars)
- Ported all screen/rendering code to 26.1.1's renamed API (`GuiGraphics` -> `GuiGraphicsExtractor`, `render` -> `extractRenderState`, `PlayerFaceRenderer` -> `PlayerFaceExtractor`, etc.)
- Updated Fabric API screen events (`ScreenEvents.afterRender` -> `afterExtract`, `Screens.getButtons` -> `getWidgets`)
- Updated sound events (`PIG_AMBIENT` -> `PIG_AMBIENT_BABY`, `PIG_DEATH` -> `PIG_DEATH_BABY`)
- All changes are behind Stonecutter preprocessor conditionals (`//? if >=26.1`) to maintain compatibility with older versions

## Dependencies

**Fabric**: [Fabric API](https://modrinth.com/mod/fabric-api) (Required),
[Mod Menu](https://modrinth.com/mod/modmenu) (Optional)

## Building

```bash
./gradlew versions/26.1.1-fabric:build -x test
```

The output jar will be at `versions/26.1.1-fabric/build/libs/`.

## Original mod

Based on [In-Game Account Switcher](https://github.com/The-Fireplace-Minecraft-Mods/In-Game-Account-Switcher) by The_Fireplace & VidTu.
Licensed under [GNU LGPLv3](LICENSE).
