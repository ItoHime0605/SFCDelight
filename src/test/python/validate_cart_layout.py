"""Validate the 2x2x2 cart and its 4x3 plastic-frame display grid."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "src/main/resources/assets/sfcdelight"
SOURCE = ROOT / "src/main/java/tw/saltedfriedchicken"

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

states = read(RES / "blockstates/fried_food_cart.json")["variants"]
assert len(states) == 32, f"Expected four facings times eight parts, got {len(states)}"
forward = {"north": (0, -1), "east": (1, 0), "south": (0, 1), "west": (-1, 0)}

for facing, front in forward.items():
    right = (-front[1], front[0])
    back = (-front[0], -front[1])
    occupied = set()
    for part in range(8):
        key = f"facing={facing},part={part}"
        variant = states[key]
        assert variant["model"].startswith("sfcdelight:block/fried_food_cart_")
        name = variant["model"].split(":", 1)[1].split("/", 1)[1]
        d, c, h = (part % 4) // 2, part % 2, part // 4
        expected = f"fried_food_cart_{'display' if h else 'base'}_{'front' if d == 0 else 'rear'}_{'left' if c == 0 else 'right'}"
        assert name == expected, (key, name, expected)
        assert (RES / "models/block" / (name + ".json")).is_file()
        x, z = right[0] * c + back[0] * d, right[1] * c + back[1] * d
        occupied.add((x, h, z))
    assert len(occupied) == 8, f"Duplicate position for facing={facing}"
    # Every facing occupies exactly a 2 x 2 footprint and is 2 blocks high.
    xs, ys, zs = zip(*occupied)
    assert max(xs) - min(xs) == 1
    assert max(zs) - min(zs) == 1
    assert set(ys) == {0, 1}

for name in (
    "fried_food_cart_base_front_left",
    "fried_food_cart_base_front_right",
    "fried_food_cart_base_rear_left",
    "fried_food_cart_base_rear_right",
    "fried_food_cart_display_front_left",
    "fried_food_cart_display_front_right",
    "fried_food_cart_display_rear_left",
    "fried_food_cart_display_rear_right",
):
    model = read(RES / "models/block" / (name + ".json"))
    assert model["elements"], name
    for element in model["elements"]:
        for axis in range(3):
            assert 0 <= element["from"][axis] <= element["to"][axis] <= 16, (name, element)

block = (SOURCE / "FriedFoodCartBlock.java").read_text(encoding="utf-8")
entity = (SOURCE / "FriedFoodCartBlockEntity.java").read_text(encoding="utf-8")
renderer = (SOURCE / "client/FriedFoodCartContentsRenderer.java").read_text(encoding="utf-8")
assert 'IntegerProperty.create("part", 0, 7)' in block
assert "part < 8" in block
assert "part % 2" in block and "(part % 4) / 2" in block
assert "return row * 4 + column;" in block
assert "public static final int DISPLAY_SLOTS = 12" in entity
assert "depth < 3" in renderer and "column < 4" in renderer
assert "relative(right).relative(back)" in renderer
assert "getRenderBoundingBox" in renderer
assert "OLD_DISPLAY_SLOTS = 18" in entity
assert "LegacyFood" in entity and "LegacyFrames" in entity

# The display grid has four columns across two blocks and three rows across two.
# Sample the actual frame centers and verify no slot maps to another index.
slots = set()
for row in range(3):
    for column in range(4):
        width = (column + 0.5) * 0.5
        depth = (row + 0.5) * (2.0 / 3.0)
        part = 4 + int(depth) * 2 + int(width)
        dx = width - int(width) - 0.5
        dz = depth - int(depth) - 0.5
        actual_width = (part % 2) + 0.5 + dx
        actual_depth = ((part - 4) // 2) + 0.5 + dz
        selected = int(math.floor(actual_depth * 1.5)) * 4 + int(math.floor(actual_width * 2))
        assert selected == row * 4 + column, (row, column, selected)
        slots.add(selected)
assert slots == set(range(12))

item = read(RES / "models/item/fried_food_cart.json")
assert len(item["elements"]) >= 12
assert min(e["from"][0] for e in item["elements"]) < 0
assert max(e["to"][0] for e in item["elements"]) > 16
assert min(e["from"][2] for e in item["elements"]) < 0
assert max(e["to"][2] for e in item["elements"]) > 16
assert max(e["to"][1] for e in item["elements"]) > 16

print("PASS: cart is 2x2x2, twelve display slots align with clicks, models and saved inventory")
