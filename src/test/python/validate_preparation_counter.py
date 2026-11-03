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
assert "displayedMarination()" in entity and "displayedBowl()" in entity
assert "displayedIngredient()" in entity and "displayedFlour()" in entity
assert "MAX_WASHES = 20" in entity and "WASHES_PER_BUCKET = 5" in entity
assert "waterUses > MAX_WASHES - WASHES_PER_BUCKET" in entity
assert "waterUses += WASHES_PER_BUCKET;" in entity and "waterUses--" in entity
assert "waterUses += 2" not in entity
assert "for (int i = 1; i < 4; i++)" in (src/"client/PreparationSinkScreen.java").read_text(encoding="utf-8")
assert "batterClicks < 3" in entity and "MARINATED_CHICKEN_DICE" in entity
assert "setChanged()" in entity and "saveAdditional" in entity and "loadAdditional" in entity
assert "onRemove" in block and "bench.drain()" in block
assert "playerWillDestroy" in block and "player.getAbilities().instabuild" in block
assert "bench.suppressCounterDrop()" in block and "bench.shouldDropCounter()" in block
assert "GameRules.RULE_DOBLOCKDROPS" in block and "!moved" in block
assert "suppressCounterDrop" in entity and "shouldDropCounter()" in entity
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
menu=(src/"PreparationSinkMenu.java").read_text(encoding="utf-8")
screen=(src/"client/PreparationSinkScreen.java").read_text(encoding="utf-8")
client=(src/"client/ClientPreparationSinkScreens.java").read_text(encoding="utf-8")
assert "serverPlayer.openMenu" in block and "player.isShiftKeyDown()" in block
assert block.index("player.isShiftKeyDown()",block.index("useWithoutItem")) < block.index("bench.washHands()",block.index("useWithoutItem"))
assert "DataSlots" in menu or "addDataSlots(data)" in menu
assert "bench.washes()" in menu and "SimpleContainerData(1)" in menu
assert "Math.max(0, Math.min(PreparationCounterBlockEntity.MAX_WASHES" in menu
assert "PREPARATION_SINK_MENU" in mod and "MENU_TYPES.register(modBus)" in mod
assert "RegisterMenuScreensEvent" in client and "PreparationSinkScreen::new" in client
assert "gui.sfcdelight.sink_washes" in screen and "water * 142 / 20" in screen
assert j("src/main/resources/assets/sfcdelight/lang/zh_tw.json")["menu.sfcdelight.preparation_sink"] == "不鏽鋼洗手台"
assert j("src/main/resources/assets/sfcdelight/lang/zh_tw.json")["gui.sfcdelight.sink_bucket"] == "每桶水補充 5 次，最多可儲存 20 次"
prep_renderer=(src/"client/PreparationCounterContentsRenderer.java").read_text(encoding="utf-8")
prep_reg=(src/"client/ClientBlockEntityRenderers.java").read_text(encoding="utf-8")
assert "PreparationCounterContentsRenderer::new" in prep_reg
assert "PREPARATION_COUNTER_ENTITY.get()" in prep_reg
assert "displayedMarination()" in prep_renderer and "displayedBowl()" in prep_renderer
assert "displayedIngredient()" in prep_renderer and "displayedFlour()" in prep_renderer
assert "pose.pushPose()" in prep_renderer and "pose.popPose()" in prep_renderer
steel_bowl = j("src/main/resources/assets/sfcdelight/models/item/steel_bowl.json")
assert len(steel_bowl["elements"]) >= 8
assert steel_bowl["textures"]["particle"] == "minecraft:block/iron_block"
print("PASS: live prep contents (real box/bowl/ingredient/flour), 3D bowl, 3-minute marinade, 4 buckets x 5 washes")
