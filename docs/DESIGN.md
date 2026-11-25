# Design notes — Stress Routes

What Create exposes for kinetic networks and why the overlay is built this way. Verified
against `create-1.21.1-6.0.10.jar`.

## What Create exposes

```
com.simibubi.create.content.kinetics.base.KineticBlockEntity
    public @Nullable Long network;          // id of the stress network this block belongs to
    public boolean hasNetwork();
    public KineticNetwork getOrCreateNetwork();
    public float calculateStressApplied();          // SU consumed by this block
    public float calculateAddedStressCapacity();    // SU provided by this block

com.simibubi.create.content.kinetics.KineticNetwork
    public Map<KineticBlockEntity, Float> members;
    public Map<KineticBlockEntity, Float> sources;
    public float calculateStress();
    public float calculateCapacity();

com.simibubi.create.content.kinetics.RotationPropagator
    public static boolean isConnected(KineticBlockEntity from, KineticBlockEntity to);
```

`RotationPropagator.isConnected` is the authoritative answer to "does rotation pass between
these two blocks". It handles axis alignment, cased shafts, gearboxes, cog meshing, chain
drives and custom connections. Using it means we never draw a line Create would not rotate.

## Data flow

```
server                                          client
PostalTracker-like chunk index                  StressClientState (edges + nodes)
  └─ every loaded KineticBlockEntity              └─ StressTextureCache (128x128 tiles)
       ├─ edges: neighbours where isConnected()        └─ StressMapOverlay (mixin render)
       ├─ nodes: role + block label
       └─ per-network stress/capacity (cached)
```

Only loaded chunks are indexed. Edges are emitted once per pair (positive directions only),
so the graph stays half of the naive size.

## Two hashes

* `topologyHash` includes stress/capacity **quantised to 32 SU**. It decides when to resend
  the snapshot, so tooltips update as the load moves without a packet per tick.
* `geometryHash` excludes stress entirely. Only this one invalidates the rasterised map
  textures, so an oscillating factory does not rebuild its tiles constantly.

## Heatmap

The heatmap is a second, opt-in view. Instead of per-block data it aggregates the stress
impact of consumers into cells (8x8 blocks by default) and sends `StressCell(cellX, cellZ,
stress, machines)`. It is computed only while a viewer asks for it (`/stressroutes heat`
prints the same summary), cached per tick, and the client rebuilds its texture only when a
cell changes by about 1% of the busiest cell. That keeps a 500-machine factory at a few
hundred cells instead of thousands of entries.

## Rendering

The transform copies Xaero's own camera maths (`scale = mapScale / Window#getGuiScale()`),
and lines are rasterised 1 pixel per block with a 1 px halo in two passes (all halos first,
then all lines) so crossings stay clean. Nodes are drawn as small markers on top.

## Limits

1. **Loaded chunks only.** Block entity state exists nowhere else.
2. **Belts are not part of kinetic networks** in the same way as shafts; they are drawn
   only through the shafts that drive them.
3. **Large cog ↔ speed controller** and other exotic connections rely on
   `isConnected`, so they appear only if Create reports them.
4. **Quantised stress**: the tooltip can lag a real change by up to 32 SU (about 0.5 s of
   typical load) — deliberate, to avoid resending the graph every tick.
5. **Xaero internals** are accessed through an accessor mixin with `require = 0`; a future
   Xaero update makes the overlay disappear, not crash.
