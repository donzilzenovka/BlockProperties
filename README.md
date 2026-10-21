# BlockProperties
Allows advanced customization options for blocks. Based on Mob Properties.

Changelog is located at src/main/resources/changelog.txt

## Metadata-specific stats (unofficial extension)

`stats_by_meta` adds metadata-aware overrides without changing the existing `stats`
format. It accepts `brightness`, `opacity`, `hardness`, `resistance`, `sound`, and
`slipperiness`; `tool_data` remains in the original metadata-aware format. Brightness
must be 0-15 and opacity 0-255. `_meta` accepts a number, an array, a comma-separated
string, or an inclusive range. Duplicate assignments to the same stat and metadata
are rejected so configuration mistakes cannot silently win.

Example for Immersive Cavegen's cave plant (metadata 0-1 emits no light and 2-7
emits light level 7):

```json
{
  "_name": "immersivecavegen:cavePlant",
  "stats_by_meta": [
    { "_meta": "0-1", "brightness": 0, "opacity": 0 },
    {
      "_meta": "2-7",
      "brightness": 7,
      "opacity": 0,
      "hardness": 0.0,
      "resistance": 1.8000001,
      "sound": "grass",
      "slipperiness": 0.6
    }
  ]
}
```

The metadata lookup table is cleared and rebuilt by `/bpreload`. Existing global
`stats` entries remain supported and are used whenever no metadata override matches.
