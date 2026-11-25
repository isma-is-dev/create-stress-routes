# Create: Stress Routes

**See your whole kinetic network on Xaero's World Map.** Shafts, cogs, gearboxes, motors
and machines drawn as one continuous green flow, with live stress load in the tooltip.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-2f8d46)](https://minecraft.net)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-f16436)](https://neoforged.net)
[![Create](https://img.shields.io/badge/Create-6.0.10+-f0b23c)](https://createmod.com)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

---

## What it does

Create's kinetic networks are invisible until something breaks. This addon reads them on
the server and mirrors them onto Xaero's World Map:

* **Network lines** — every block rotation actually propagates between, as a solid green
  line with a dark outline.
* **Nodes** — sources (bright green), consumers (yellow-green), pure transmission (dim
  green) and overstressed networks (red).
* **Live stress** — hover any block for the network's `used / capacity SU` and load
  percentage.
* **No blocks, no items, no worldgen.** A pure viewer: safe to add to an existing world and
  safe to remove.

Connections are validated with Create's own `RotationPropagator.isConnected`, so gearboxes,
cased shafts, cog meshing and chain drives are drawn exactly as they transmit.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Create | 6.0.10 or newer (6.0.x) |
| Xaero's World Map | 1.40.x (optional at runtime) |

## Installation

1. NeoForge 21.1.x for Minecraft 1.21.1.
2. Install Create and Xaero's World Map.
3. Drop `stressroutes-1.21.1-1.0.0.jar` into `mods/`.
4. Open the map (`M`) and press the **Stress** button on the left.

Multiplayer works: install on server and clients. Only chunks loaded on the server can be
observed.

## Reading the map

| Element | Meaning |
|---|---|
| Solid green line, dark outline | Rotation propagates between these two blocks |
| Bright green marker | Stress source (motor, water wheel, windmill...) |
| Yellow-green marker | Stress consumer (machines) |
| Dim green marker | Transmission only (shaft, cog, gearbox) |
| Red marker | Network is over capacity |

Tooltips show the block name, its role and the network's stress (`used / capacity SU`).

## Configuration

`config/stressroutes-client.toml`

| Option | Default | Description |
|---|---|---|
| `enabled` | `true` | Master switch (also the map button) |
| `showRoutes` | `true` | Draw the network lines |
| `showNodes` | `true` | Draw block markers |
| `lineTheme` | `GREEN` | `GREEN`, `AMBER` or `TEAL` |
| `routeOpacity` | `100` | Line opacity, 0-100 |
| `dashedRoutes` | `false` | Solid lines by default |
| `maxMapSections` | `512` | 128×128 block texture cache limit |
| `widgetY` | `64` | Button position (Postal uses 48, Fluid 80) |

`config/stressroutes-server.toml`

| Option | Default | Description |
|---|---|---|
| `syncIntervalTicks` | `10` | Rebuild rate while a map is open |
| `maxNodes` | `16384` | Kinetic blocks reported per dimension |
| `maxDirtyChunksPerTick` | `4` | Chunk rescans per tick after block changes |

Commands: `/stressroutes stats` and `/stressroutes rescan` (permission level 2).

## Performance

* Nothing is collected while no map is open.
* Chunk load/unload indexing, no world scans.
* Textures rebuild only when the network *shape* changes; stress numbers update separately.
* Packets are chunked (192 entries each) and hard-capped by config.

## Compatibility

Tested with NeoForge 21.1.228, Create 6.0.10 and Xaero's World Map 1.40.11 alongside a
~190-mod Create pack. No Create mixins: only public block entity API plus Create's own
connectivity helper. The only mixin targets Xaero's map and is skipped when Xaero is absent.

## Building

```powershell
powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1
powershell -ExecutionPolicy Bypass -File tools/build.ps1
powershell -ExecutionPolicy Bypass -File tools/install-to-instance.ps1
```

## License

MIT — see [LICENSE](LICENSE). Not affiliated with Mojang, Create or Xaero.
