"""Verify the six original core fried foods without assuming one PNG scanline filter.
All standard PNG filter types 0..4 are valid; pixel occupancy must be checked
after unfiltering, not directly in compressed/decompressed filter-dependent bytes.
"""
import json
from pathlib import Path
from sfcdelight_png import validate_icon

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "src/main/resources/assets/sfcdelight"
NAMES = (
    "fried_chicken_dice",
    "fried_cod_dice",
    "fried_dried_tofu",
    "fried_hundred_layer_tofu",
    "fried_rice_blood_cake",
    "fried_duck_blood",
)
for name in NAMES:
    path = RES / "textures/item" / (name + ".png")
    assert path.is_file(), name
    coverage = validate_icon(path, expected_size=128)
    assert 5000 <= coverage <= 11000, (name, coverage)
    model = json.loads((RES / "models/item" / (name + ".json")).read_text(encoding="utf-8"))
    assert model["parent"] == "minecraft:item/generated", name
    assert model["textures"]["layer0"] == "sfcdelight:item/" + name, name
    item_definition = RES / "items" / (name + ".json")
    assert item_definition.exists(), item_definition
    print(f"PASS {name}: independent transparent 128px sprite, PNG CRC, model reference")
