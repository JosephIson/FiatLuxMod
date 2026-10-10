"""Writes the Realm Forge dimension pool: data/fiatlux/dimension/planet_0..15.json."""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/data/fiatlux/dimension"
for k in range(16):
    (OUT / f"planet_{k}.json").write_text(json.dumps({
        "type": "fiatlux:planet",
        "generator": {"type": "fiatlux:planet", "slot": k,
                      "biome_source": {"type": "minecraft:fixed", "biome": "fiatlux:created_world"}},
    }, indent=2) + "\n")
print("wrote 16 pool dimensions")
