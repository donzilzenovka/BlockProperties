# BlockProperties
Allows advanced customization options for blocks. Based on Mob Properties.

Changelog is located at src/main/resources/changelog.txt

## Metadata-specific brightness (experimental)

`stats_by_meta` adds world-aware brightness overrides without changing the existing
`stats` format. Brightness must be from 0 through 15. `_meta` accepts a number, an
array, a comma-separated string, or an inclusive range. Later duplicate metadata
selectors are rejected so configuration mistakes cannot silently win.

Example for Immersive Cavegen's cave plant (metadata 0-1 emits no light and 2-7
emits light level 7):

```json
{
  "_name": "immersivecavegen:cavePlant",
  "stats_by_meta": [
    { "_meta": "0-1", "brightness": 0 },
    { "_meta": "2-7", "brightness": 7 }
  ]
}
```

The metadata lookup table is cleared and rebuilt by `/bpreload`. Existing global
`stats.brightness` entries remain supported and are used whenever no metadata
override matches.
