# Modrinth description (copy/paste)

---

# Create: Stress Routes

**See your whole kinetic network on Xaero's World Map.**

Create's mechanical networks are invisible until something jams. This addon mirrors them
onto the map: every shaft, cog, gearbox, motor and machine as one continuous green flow.

## Features

- **Network lines** validated with Create's `RotationPropagator.isConnected` — gearboxes,
  cased shafts, cog meshing and chain drives included.
- **Node markers**: bright green sources, yellow-green consumers, dim green transmission,
  red when overstressed.
- **Live stress**: hover a drivetrain block for the network's used / capacity SU and load
  percentage.
- **Stress heatmap**: a separate Heat button paints an aggregated consumption map (green →
  red) so stressed zones stand out without rendering every machine.
- **On-map toggle** with a live network counter.
- **No blocks, no items, no worldgen** — a viewer you can add and remove freely.

## Performance

Nothing is collected while no map is open; chunk load/unload indexing; textures rebuild
only when the network shape changes; chunked, config-capped packets.

## Requirements

- Minecraft 1.21.1, NeoForge 21.1.x
- Create 6.0.10+ (required)
- Xaero's World Map 1.40.x (optional — the mod does nothing without it)

## License

MIT. Not affiliated with Mojang, Create or Xaero.
