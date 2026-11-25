# Build & local tooling

These scripts are conveniences for the developer machine, not part of the mod.

## `fetch-libs.ps1`

Copies Xaero's World Map / Minimap jars into `libs/` so the project can compile.

```powershell
powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1
# or point it at another instance:
powershell -ExecutionPolicy Bypass -File tools/fetch-libs.ps1 -Instance "C:\path\to\instance"
```

## `build.ps1`

Builds the mod with the JDK 21 that ships with IntelliJ/Android Studio when no JDK 21
is on `PATH`, then reports the output jar.

```powershell
powershell -ExecutionPolicy Bypass -File tools/build.ps1
```

## `install-to-instance.ps1`

Copies the newest built jar into a CurseForge instance's `mods/` folder.

```powershell
powershell -ExecutionPolicy Bypass -File tools/install-to-instance.ps1
```
