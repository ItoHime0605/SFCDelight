"""Static regression checks for table preparation -> physical fryer basket -> frying.

These checks complement (but do not replace) real in-game integration tests.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java/tw/saltedfriedchicken"
RES = ROOT / "src/main/resources/assets/sfcdelight"

def source(name):
    return (SRC / name).read_text(encoding="utf-8")

basket = source("FryingBasketBlock.java")
basket_be = source("FryingBasketBlockEntity.java")
fryer = source("FryingStationBlock.java")
recipes = source("FryingRecipes.java")
registry = source("SaltedFriedChickenDelight.java")
container = source("FriedContainerItem.java")

assert "LOADED_RAW_BASKET" in registry
assert "loaded_raw_basket" in registry
assert "isValidRawBatch" in recipes
assert "countPortions(foods)" in recipes
assert "portions <= maxPortions" in recipes

# Plastic basket still unloads all its contents atomically, without a five-type cap.
assert "transferFromPlastic(plastic)" in basket
assert "transferFromPlastic(ItemStack plastic)" in basket_be
assert "batch.size() > MAX_PIECES" in basket_be
assert "countDistinctTypes(batch) > MAX_PIECES" in basket_be
assert "plastic.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY)" in basket_be

# Empty-hand right click lifts a whole raw metal basket from a table,
# without sneaking. Fryer-mounted raised uncooked baskets retain sneak pickup.
assert "No sneak key required." in basket
assert "if ((!state.getValue(SUBMERGED)" in basket
assert "(player.isShiftKeyDown() && state.getValue(RAISED))" in basket
assert "FryingRecipes.isValidRawBatch(basket.ingredients()" in basket
assert "FriedBatchData.wrapMany(" in basket
assert "SaltedFriedChickenDelight.LOADED_RAW_BASKET.get()" in basket
assert "basket.extractAll();" in basket and "level.removeBlock(pos, false);" in basket

# A fryer permits exactly one physical metal basket over its surface.
assert "held.is(SaltedFriedChickenDelight.LOADED_RAW_BASKET.get())" in fryer
assert "!level.getBlockState(basketPos).canBeReplaced()" in fryer
assert "basket.restoreRaw(raw)" in fryer
assert "held.shrink(1)" in fryer
assert "batchReady = true" in basket_be
assert "private static final int COOK_TICKS = 300" in basket_be
assert "setPose(false, fryer.charges() <= 0)" in basket_be
assert "if (fryer.consumeFryingCharge())" in basket_be
assert "FryingRecipes.isValidCookedBatch(cooked, MAX_PIECES)" in basket_be
assert "getValue(FryingBasketBlock.RAISED)" in basket_be

# Item art is the real metal basket, but tooltips clearly say the raw food is not cooked.
model = json.loads((RES / "models/item/loaded_raw_basket.json").read_text(encoding="utf-8"))
definition = json.loads((RES / "items/loaded_raw_basket.json").read_text(encoding="utf-8"))
lang = json.loads((RES / "lang/zh_tw.json").read_text(encoding="utf-8"))
assert model["parent"] == "sfcdelight:item/frying_basket"
assert definition["model"]["model"] == "sfcdelight:item/loaded_raw_basket"
hot_definition = json.loads((RES / "items/hot_food_basket.json").read_text(encoding="utf-8"))
assert hot_definition["model"]["model"] == "sfcdelight:item/hot_food_basket"
assert lang["item.sfcdelight.loaded_raw_basket"] == "裝有生食材的金屬炸籃"
assert "appendMetalBasketContents" in container
assert "HOT_FOOD_BASKET.get()" in container
assert "LOADED_RAW_BASKET.get()" in container
details = source("FoodContentsTooltip.java")
assert "食材份數：" in details and "內含食材：" in details
assert "已油炸・待脫油" in details
assert "amounts.forEach" in details
assert "拿著金屬炸籃右鍵脫油機，整籃轉移炸物" in container

# Pick up the completed fryer basket as one hot container, then
# transfer it into the de-oiler as an intact batch.
assert "FryingRecipes.isValidCookedBatch(basket.ingredients()" in basket
assert "SaltedFriedChickenDelight.HOT_FOOD_BASKET.get()" in basket
assert "Never scatter individual contents" in basket
de_oiler = source("DeOilerBlock.java")
de_oiler_entity = source("DeOilerBlockEntity.java")
assert "held.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get())" in de_oiler
assert "machine.insert(held)" in de_oiler
assert "held.shrink(1)" in de_oiler
assert "new ItemStack(SaltedFriedChickenDelight.FRYING_BASKET_ITEM.get())" in de_oiler
assert "FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(cooked" in de_oiler_entity
assert "CYCLE_TICKS = 160" in de_oiler_entity
assert "machine.dropContents()" in de_oiler
assert "SaltedFriedChickenDelight.DEOILER_ITEM.get()" in de_oiler
assert "FryingBasketBlock.dropPortableBasket(level, basketPos, basket)" in fryer
assert "machine.dropContents();" in de_oiler
assert "FriedBatchData.readAll(stored, level.registryAccess())" not in de_oiler
assert "GameRules.RULE_DOBLOCKDROPS" in de_oiler
assert "machine.shouldDropEquipment()" in de_oiler

print("PASS: metal basket carries every fried portion, readable tooltip, atomic de-oiler transfer and no duplicate basket")
