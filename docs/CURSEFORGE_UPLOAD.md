# CurseForge upload checklist

| Field | Value |
|---|---|
| Name | `Create: Stress Routes` |
| Summary | `Plot your Create kinetic network on Xaero's World Map: shafts, cogs, gearboxes and machines as a continuous green flow, plus a stress heatmap of the busiest zones.` |
| Category (primary) | `Map and Information` |
| Additional categories | `Technology` |
| License | `MIT License` |
| Game version | `1.21.1` |
| Mod loader | `NeoForge` |
| Java | `Java 21` |
| Environment | `Client` + `Server` (both) |
| Release type | `Release` |
| Project avatar | `src/main/resources/icon.png` (or a 400x400 render) |
| Source | `https://github.com/isma-is-dev/create-stress-routes` |
| Issues | `https://github.com/isma-is-dev/create-stress-routes/issues` |

Description: paste `docs/CURSEFORGE_DESCRIPTION.md` (BBCode).

Relations:

| Project | Relation |
|---|---|
| `Create` (328085) | Required dependency |
| `Xaero's World Map` (317780) | Optional dependency |

File upload:

| Field | Value |
|---|---|
| File | `build/libs/stressroutes-1.21.1-<version>.jar` |
| Release type | `Release` |
| Game versions | `1.21.1`, `NeoForge`, `Client`, `Server` |
| Changelog | matching section of `CHANGELOG.md` |

API upload (optional): `tools/upload-curseforge.ps1 -ProjectId <id> -Token <token>` (copied
from the Postal Routes project; the token is never written to disk).
