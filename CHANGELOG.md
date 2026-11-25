# Changelog

All notable changes to this project are documented here.
Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versioning: SemVer.

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
