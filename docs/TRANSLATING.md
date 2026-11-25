# Translating Create: Stress Routes

The mod ships **English** (`en_us`, complete) and **Spanish** (`es_es`, complete). Adding a
language is a single JSON file and does not require Java changes.

## Add a language

1. Copy `src/main/resources/assets/stressroutes/lang/en_us.json` to
   `src/main/resources/assets/stressroutes/lang/<locale>.json` (for example `de_de.json`,
   `fr_fr.json`, `pt_br.json`, `ru_ru.json`, `zh_cn.json`).
2. Translate the values, never the keys. Keep the `%s` placeholders in the same order.
3. Save as **UTF-8** (no BOM). ANSI/Windows-1252 breaks accents in game.
4. Open a pull request. Include your name/handle if you want to be credited.

## Rules

- `%s` are substituted in order. Do not translate or reorder them.
- `stressroutes.widget.label` is intentionally short: it must fit a 58x14 button next to the
  icon. Keep it under ~9 characters if possible.
- Block names come from Create's own translations; do not translate the `label` sent by the server.
- The status line and tooltips are read at render time, so language changes apply
  immediately when the player switches language in game.

## Verify locally

```powershell
powershell -ExecutionPolicy Bypass -File tools/build.ps1
# copy build/libs/stressroutes-*.jar into mods/, launch, switch language, open the map
```

Check the mod list entry, the Stress button tooltip, a port tooltip and
`/stressroutes stats`.
