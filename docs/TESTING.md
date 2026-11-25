# Testing checklist

## 1. Loads

`logs/latest.log` should contain `Create: Stress Routes 1.0.0 (stressroutes)`.

## 2. Hook wired

Open the world map once:

```
Xaero's World Map found; postal map integration enabled
Stress Routes: Xaero world map hook is active
```

No second line means the mixin config is not loaded: check the `[[mixins]]` block in
`neoforge.mods.toml`.

## 3. Widget

A **Stress** button appears below the Postal one. The number is the amount of stress
networks in the viewed dimension.

## 4. Data

Build a minimal network in creative:

```
/give @p create:creative_motor
/give @p create:creative_motor 5
```

Place the motor facing a shaft, then a line of shafts. Run `/stressroutes stats` as an op:
it should report blocks, links and one network. `/stressroutes rescan` re-indexes chunks
manually.

## 5. Alignment

One map pixel is one block, drawn with Xaero's camera transform; a marker must sit exactly
on its block. If everything is scaled by 8x, the dimension divisor is wrong.

## 6. Multiplayer

Server without the mod → client logs one line and stops asking. Server with it → only loaded
chunks are shown.
