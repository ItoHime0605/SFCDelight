"""Static layout checks for the 1x4x2 cart. Run with python3 from the repository root."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "src/main/resources/assets/sfcdelight"
SOURCE = ROOT / "src/main/java/tw/saltedfriedchicken"

states = json.loads((RES / "blockstates/fried_food_cart.json").read_text(encoding="utf-8"))["variants"]
assert len(states) == 32, f"Four facings x eight segments required, got {len(states)}"
for facing in ("north", "east", "south", "west"):
    for part in range(8):
        variant = states[f"facing={facing},part={part}"]
        assert variant["model"].startswith("sfcdelight:block/fried_food_cart_")
        if part >= 4:
            assert variant["model"].endswith("fried_food_cart_display")
        model = RES / "models" / (variant["model"].split(":", 1)[1] + ".json")
        assert model.is_file(), model
        json.loads(model.read_text(encoding="utf-8"))

block = (SOURCE / "FriedFoodCartBlock.java").read_text(encoding="utf-8")
entity = (SOURCE / "FriedFoodCartBlockEntity.java").read_text(encoding="utf-8")
renderer = (SOURCE / "client/FriedFoodCartContentsRenderer.java").read_text(encoding="utf-8")
assert 'IntegerProperty.create("part", 0, 7)' in block
assert "part < 8" in block
assert "part < 4" not in block or "state.getValue(PART) < 4" in block
assert "public static final int DISPLAY_SLOTS = 12" in entity
assert "displaySlot" in block and " * 3 + column" in block
assert "depth < 4" in renderer and "column < 3" in renderer
assert "getRenderBoundingBox" in renderer
assert "OLD_DISPLAY_SLOTS = 18" in entity
assert "LegacyFood" in entity and "LegacyFrames" in entity

top = json.loads((RES / "models/block/fried_food_cart_display.json").read_text(encoding="utf-8"))
assert len(top["elements"]) >= 7
for model_name in (
    "fried_food_cart_base_front",
    "fried_food_cart_base_middle",
    "fried_food_cart_base_rear",
    "fried_food_cart_display",
):
    model = json.loads((RES / "models/block" / (model_name + ".json")).read_text(encoding="utf-8"))
    for element in model["elements"]:
        for axis in range(3):
            assert -16 <= element["from"][axis] <= element["to"][axis] <= 32, (model_name, element)
print("Cart 1x4x2: 8 segments, 12 display slots, 32 blockstates, migration and render bounds OK")
