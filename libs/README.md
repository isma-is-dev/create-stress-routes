# Build-time libraries (not committed)

This addon compiles against Create and Xaero's World Map:

* **Create** and **Ponder/Catnip** are resolved from `https://maven.createmod.net/`
  automatically by Gradle.
* **Xaero's World Map / Minimap** are not published to a public maven, so their jars
  must sit in this folder. **XaeroLib** ships nested inside the world map jar and is
  extracted by the script. They are compile-only: the built mod does not bundle them.

## Get the jars

```powershell
powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1
```

The script first looks in your CurseForge instance and only falls back to downloading
from Modrinth. You can also copy them manually:

```
libs/xaeroworldmap-neoforge-1.21.1-1.40.11.jar
libs/xaerominimap-neoforge-1.21.1-25.3.10.jar
```

Both jars are git-ignored on purpose (they are third-party, all-rights-reserved
artifacts).
