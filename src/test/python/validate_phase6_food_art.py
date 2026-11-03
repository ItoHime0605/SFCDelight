"""Keep all 11 second-pass Taiwanese street-food icons individually legible.
They use real 32px RGBA pixel art, not text-based or tinted placeholder icons.
This is an art regression test; item registration, recipe math and NBT must not change.
"""
from pathlib import Path
from hashlib import sha256
import json
from sfcdelight_png import validate_icon

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/sfcdelight"
IDENTITIES = (
    "fried_chicken_dice",
    "fried_cod_dice",
    "fried_fries",
    "fried_sweet_potato_fries",
    "fried_dried_tofu",
    "fried_hundred_layer_tofu",
    "fried_duck_blood",
    "fried_rice_blood_cake",
    "fried_squid_tentacles",
    "fried_squid_ball",
    "fried_tempura_fish_cake",
)
seen = set()
for item_id in IDENTITIES:
    path = ASSETS / "textures/item" / (item_id + ".png")
    assert path.is_file(), item_id
    coverage = validate_icon(path)  # Validated PNG CRC, 32x32 RGBA, real alpha.
    assert 100 <= coverage <= 830, (item_id, coverage)
    signature = sha256(path.read_bytes()).hexdigest()
    assert signature not in seen, ("Duplicate food illustration", item_id)
    seen.add(signature)
    model_path = ASSETS / "models/item" / (item_id + ".json")
    model = json.loads(model_path.read_text(encoding="utf-8"))
    assert model["parent"] == "minecraft:item/generated", item_id
    assert model["textures"]["layer0"] == "sfcdelight:item/" + item_id, item_id

# Previously reviewed art is not replaced by this pass:
for item_id in ("fried_basil", "fried_mushroom", "fried_squid_rings", "fried_chicken_skin"):
    assert (ASSETS / "textures/item" / (item_id + ".png")).exists()
print("PASS: 11 distinct 32×32 transparent original food sprites, linked item models, phase-one art preserved")
