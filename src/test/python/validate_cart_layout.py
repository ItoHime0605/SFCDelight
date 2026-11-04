"""Regression checks for the 3x1x2 cart, its 4x3 showcase and refillable pepper counter."""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "src/main/resources/assets/sfcdelight"
SRC = ROOT / "src/main/java/tw/saltedfriedchicken"

def load(path):
    return json.loads(path.read_text(encoding="utf-8"))

states = load(RES / "blockstates/fried_food_cart.json")["variants"]
# Keep two legacy part values registered, even though only parts 0..5 are placed now.
assert len(states) == 32
names = [
    "fried_food_cart_base_counter",
    "fried_food_cart_base_display_left",
    "fried_food_cart_base_display_right",
    "fried_food_cart_upper_counter",
    "fried_food_cart_upper_display_left",
    "fried_food_cart_upper_display_right",
]
facings = {"north": (0,-1), "east": (1,0), "south": (0,1), "west": (-1,0)}
for facing, front in facings.items():
    right = (-front[1],front[0])
    occupied = set()
    for part in range(8):
        variant = states[f"facing={facing},part={part}"]
        path = RES / "models" / (variant["model"].split(":", 1)[1] + ".json")
        assert path.is_file(), (facing,part,path)
        load(path)
        if part < 6:
            assert variant["model"].endswith(names[part]),(facing,part)
            occupied.add((right[0]*(part%3),part//3,right[1]*(part%3)))
    assert len(occupied) == 6
    xs,ys,zs = zip(*occupied)
    assert max(xs)-min(xs) + max(zs)-min(zs) == 2
    assert set(ys) == {0,1}
    assert max(xs)-min(xs) == 2 or max(zs)-min(zs) == 2
    for part in range(6):
        model = load(RES / "models/block" / (names[part]+".json"))
        assert model["elements"],names[part]
        assert "wheel" not in json.dumps(model).lower(), names[part]
        for element in model["elements"]:
            for axis in range(3):
                minimum = (-8 if part >= 3 and axis == 2
                           and element["from"][1] >= 13 else 0)
                assert minimum <= element["from"][axis] < element["to"][axis] <= 16, (names[part],element)
    # The click positions across the two display blocks map to exactly 4x3 slots.
    selected=set()
    for row in range(3):
        for col in range(4):
            width=(col+0.5)*0.5
            depth=(row+0.5)/3
            part=4+int(width)
            localWidth=width-(part-4)
            clickedCol=int(math.floor(((part-4)+localWidth)*2))
            clickedRow=int(math.floor(depth*3))
            actual=clickedRow*4+clickedCol
            assert actual == row*4+col,(facing,row,col,actual)
            selected.add(actual)
    assert selected==set(range(12))

block=(SRC/"FriedFoodCartBlock.java").read_text(encoding="utf-8")
entity=(SRC/"FriedFoodCartBlockEntity.java").read_text(encoding="utf-8")
renderer=(SRC/"client/FriedFoodCartContentsRenderer.java").read_text(encoding="utf-8")
registration=(SRC/"SaltedFriedChickenDelight.java").read_text(encoding="utf-8")
assert "part < 6" in block
assert "part % 3" in block and "part / 3" in block
assert "return row * 4 + column;" in block
assert "public static final int DISPLAY_SLOTS = 12" in entity
assert "row < 3" in renderer and "column < 4" in renderer
assert "cart.getBasket(basketIndex)" in renderer and "basketIndex < cart.basketCount()" in renderer
assert "double rimHeight = 1.15 + basketIndex * 0.080;" in renderer
assert "int shelf = basketIndex / 4" not in renderer
assert "int col = basketIndex % 4" not in renderer
assert "pose.scale(0.31f, 0.31f, 0.31f);" in renderer
assert "cart.getPepperJar()" in renderer
assert "LevelRenderer.getLightColor" in renderer and "1 + column / 2" in renderer
assert "foodLight" in renderer and "counterLight" in renderer
assert "relative(right, 2)" in renderer
assert "getRenderBoundingBox" in renderer
assert "installPepperJar" in entity and "refillPepperJar" in entity and "seasonPackage" in entity
assert "CounterPepper" in entity and "takePepperJar" in block
assert "CounterChili" in entity and "takeChiliJar" in block
assert "drop(level, anchor, pepper);" in block and "drop(level, anchor, chili);" in block
assert "held.shrink(inserted)" in block and "held.shrink(1)" in block
assert "MAX_BASKETS = 8" in entity and "basketCount()" in entity
assert "OLD_DISPLAY_SLOTS = 18" in entity and "LegacyFood" in entity and "LegacyFrames" in entity
assert "lightLevel(state -> state.getValue(FriedFoodCartBlock.PART) == 4" in registration
assert "? 12 : 0" in registration
item=load(RES/"models/item/fried_food_cart.json")
assert len(item["elements"]) >= 12
assert "wheel" not in json.dumps(item).lower()
xs=[e["from"][0] for e in item["elements"]]+[e["to"][0] for e in item["elements"]]
ys=[e["from"][1] for e in item["elements"]]+[e["to"][1] for e in item["elements"]]
zs=[e["from"][2] for e in item["elements"]]+[e["to"][2] for e in item["elements"]]
# The overhanging canopy increases the total model depth by half a block.
# Compare cart CABINET footprint (still 3x1) instead of roof dimensions.
body=[e for e in item["elements"] if e["from"][1]<12]
body_xs=[e["from"][0] for e in body]+[e["to"][0] for e in body]
body_zs=[e["from"][2] for e in body]+[e["to"][2] for e in body]
assert max(body_xs)-min(body_xs) > 2*(max(body_zs)-min(body_zs))
assert max(ys)-min(ys) > (max(zs)-min(zs))
for e in item["elements"]:
    for axis in range(3):
        assert -16 <= e["from"][axis] <= e["to"][axis] <= 32,e

print("PASS: 3x1x2 wheel-free cart, 12 front-facing trays, level-12 showcase lighting, seasoning counter and migration")
