# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: SemVer.

## [1.1.1] - 2026-11-25

### Fixed

- Heatmap values were far too low: cells used the base stress impact instead of Create's
  actual `impact x |speed|`, so a machine at speed looked almost free. Values now match the
  network stress shown by the stress gauge.
- The Heat button could not be turned off: it sat on top of the map area, and GuiMap ate
  those clicks. All buttons now read raw mouse input and the Heat button moved below the
  Stress button.
- Wider buttons: the label and the counter are no longer cramped.
- Spanish tooltip wording: the heatmap now says "Cada celda cubre 8x8 bloques" instead of
  the cryptic "(8x8 bloques)".
## [1.1.0] - 2026-11-25

### Added

- **Stress heatmap**, a separate view toggled by its own **Heat** button next to the Stress
  button. It aggregates consumed SU into configurable cells (8x8 blocks by default) and
  paints them green -> yellow -> orange -> red, so you can see which areas of a base are
  pulling the most stress without rendering a single machine. Hover a cell for its SU and
  machine count. Server command: `/stressroutes heat`.

### Notes

- The heatmap is computed only while a viewer asks for it, cached per tick, and the client
  only rebuilds its texture when the distribution changes by ~1% of the busiest cell.
## [1.0.1] - 2026-11-25

### Changed

- Only the drivetrain is drawn now: shafts, cogs, gearboxes and similar transmission
  blocks. Machines, generators and belts are excluded, which removes most of the markers
  and makes the network readable at a glance.
- Markers are a single map pixel with no frame, and lines use a thinner four-neighbour
  outline, so the overlay stops covering the base.

### Fixed

- Map colours were packed as RGB into an ABGR buffer, so the green appeared with swapped
  red/blue channels. Colours now convert explicitly.
## [1.0.0] - 2026-11-25

### Added

- Stress overlay for Xaero's World Map:
  - Kinetic network edges validated with `RotationPropagator.isConnected`.
  - Node markers for sources, consumers, transmission blocks and overstressed networks.
  - Tooltips with the network's used/capacity SU and load percentage.
  - On-map **Stress** toggle with a live network counter.
- Server-side kinetic index (chunk load/unload + block change rescans + self-healing sweep).
- Demand-driven sync, geometry/stats hash split so textures only rebuild on shape changes.
- Client and server config, `/stressroutes stats` and `/stressroutes rescan`.
- English and Spanish translations.
