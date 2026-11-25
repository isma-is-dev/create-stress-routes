# Compatibility

## Target environment

| Component | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.228 |
| Create | 1.21.1-6.0.10 |
| Xaero's World Map | 1.40.11 |
| Mod count | ~190 (Create-focused pack) |

## Why it is safe

The mod never mixins into Create. It reads public API only:

* `KineticBlockEntity` state (network id, stress/capacity helpers).
* `RotationPropagator.isConnected` for connectivity.
* No registry entries, no world data, no recipes.

Safe alongside the Create addons tested with Postal Routes (Garnished, Dreams n' Desires,
Big Cannons, Aeronautics, Diesel Generators, Power Grid, Central Kitchen, Tradeworks,
Railways Navigator, etc.): if a block is a `KineticBlockEntity` it is drawn, otherwise it is
ignored.

## Known interactions

| Situation | Behaviour |
|---|---|
| Xaero's World Map absent | Mod loads, does nothing; the mixin is skipped. |
| Xaero update moves the render hook | Overlay stops drawing, no crash (`require = 0`). |
| Create 5.x | Mod refuses to load (declared dependency). |
| Server without the mod | Client shows nothing, no errors. |
| Very large factory (> 16384 blocks) | Capped by `server.maxNodes`; raise it if needed. |
| Stress oscillating quickly | Tooltips refresh in 32 SU steps; textures do not rebuild. |

## With the sibling mods

Postal Routes (button at y=48), Stress Routes (y=64) and Fluid Routes (y=80) stack on the
map. Each has its own toggle and config, so they can be used independently.
