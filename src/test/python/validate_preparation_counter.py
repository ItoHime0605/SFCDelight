"""Regression checks: 2-wide preparation bench, 3-minute marinade, 3-click batter, 20 hand washes."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[3]
j=lambda p:json.loads((R/p).read_text(encoding="utf-8"))
def source(n):(R/"src/main/java/tw/saltedfriedchicken"/n).read_text(encoding="utf-8")
src=(R/"src/main/java/tw/saltedfriedchicken")
block=(src/"PreparationCounterBlock.java").read_text(encoding="utf-8")
entity=(src/"PreparationCounterBlockEntity.java").read_text(encoding="utf-8")
mod=(src/"SaltedFriedChickenDelight.java").read_text(encoding="utf-8")
recipes=(src/"FryingRecipes.java").read_text(encoding="utf-8")
assert "MARINATE_TICKS = 20 * 180" in entity
assert "MAX_WASHES = 20" in entity and "waterUses += 2" in entity and "waterUses--" in entity
assert "batterClicks < 3" in entity and "MARINATED_CHICKEN_DICE" in entity
assert "setChanged()" in entity and "saveAdditional" in entity and "loadAdditional" in entity
assert "onRemove" in block and "bench.drain()" in block
assert "getClockWise()" in block and "getCounterClockWise()" in block
assert "SLIPPERY_HANDS" in mod and "-0.2" in mod and "BLOCK_BREAK_SPEED" in mod
assert "player.removeEffect(SaltedFriedChickenDelight.SLIPPERY_HANDS)" in block
assert "MobEffectInstance.INFINITE_DURATION" in block
assert "BATTERED_CHICKEN_DICE" in recipes and "BATTERED_COD_DICE" in recipes
states=j("src/main/resources/assets/sfcdelight/blockstates/preparation_counter.json")["variants"]
assert len(states)==8
for face in ("north","east","south","west"):
  for part in (0,1):
    model=states[f"facing={face},part={part}"]["model"].split(":")[1]
    data=j("src/main/resources/assets/sfcdelight/models/"+model+".json")
    assert data["elements"]
for name in ("preparation_counter","marination_box","marinating_chicken_box","marinating_cod_box","steel_bowl"):
  recipe=j("src/main/resources/data/sfcdelight/recipe/"+name+".json")
  assert recipe["result"]["id"]=="sfcdelight:"+name
assert "minecraft:sugar" in j("src/main/resources/data/sfcdelight/recipe/marinating_chicken_box.json")["ingredients"]
print("PASS: 2x1 marination/steel bowl workstation, 3-minute timer, 3 manual batter clicks, 20 washes, permanent slippery status until washing")
