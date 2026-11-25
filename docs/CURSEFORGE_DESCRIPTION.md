# CurseForge description (copy/paste)

BBCode, ready for the project description editor.

---

[b]See your whole kinetic network on Xaero's World Map.[/b]

Create's mechanical networks are invisible until something jams. Stress Routes mirrors them onto Xaero's World Map so you can read a whole factory at a glance:

[list]
[*][b]Network lines[/b] - every connection rotation actually propagates between, drawn as a solid green line with a dark outline.
[*][b]Nodes[/b] - bright green sources (motors, water wheels), yellow-green consumers (machines), dim green transmission (shafts, cogs, gearboxes) and red when a network is overstressed.
[*][b]Live stress[/b] - hover any block for the network's used / capacity SU and load percentage.
[*][b]On-map toggle[/b] - a Stress button with a live network counter.
[/list]

Connectivity is validated with Create's own [i]RotationPropagator.isConnected[/i], so gearboxes, cased shafts, cog meshing and chain drives are drawn exactly as they transmit - no guessed "touching blocks" lines.

[h2]No blocks. No items. No worldgen.[/h2]

A pure viewer: it reads Create's public block entity data on the server and streams it to clients with the map open. Add it to an existing world, remove it whenever.

[h2]Performance first[/h2]

[list]
[*]Nothing is collected while no one has the map open.
[*]Chunks are indexed on load/unload; there is no world scan.
[*]Textures rebuild only when the network shape changes - stress numbers update separately.
[*]Packets are chunked and hard-capped by config.
[/list]

[h2]Requirements[/h2]

[list]
[*]Minecraft 1.21.1
[*]NeoForge 21.1.x
[*]Create 6.0.10+ (6.0.x)
[*]Xaero's World Map 1.40.x (optional - without it the mod does nothing)
[/list]

[h2]Stacks with the other route mods[/h2]

Postal Routes (train-map style logistics) and Fluid Routes (pipe network) use different button positions and colours, so all three can run at once.

[h2]Credits[/h2]

Made by isma-is-dev. Not affiliated with Mojang, Create or Xaero.
