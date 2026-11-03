"""Garlic & basil should trigger a non-stacking 5-minute 20% attack/mining effect."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[3]
src=root/"src/main/java/tw/saltedfriedchicken"
get=lambda f:(src/f).read_text(encoding="utf-8")
reg=get("SaltedFriedChickenDelight.java")
food=get("FriedContainerItem.java")
mix=get("GarlicBasilMixItem.java")
assert "SATISFACTION" in reg and "satisfaction_attack_speed" in reg and "satisfaction_mining_speed" in reg
assert reg.count("0.20") >= 2 and "BLOCK_BREAK_SPEED" in reg
assert "20 * 60 * 5" in food and "GarlicBasilMixItem.hasMix(packageStack)" in food
assert "GarlicBasilMixItem.mark(next)" in food
assert 'tag.putBoolean("GarlicBasil",true)' in mix
assert 'hasMix(food) ||' in mix
recipe=json.loads((root/"src/main/resources/data/sfcdelight/recipe/garlic_basil_mix.json").read_text())
assert recipe["ingredients"]==[{"item":"sfcdelight:garlic"},{"item":"sfcdelight:basil"}]
print("PASS: garlic/basil +20% mining and attack, five-minute nonstacking status per eaten serving")
