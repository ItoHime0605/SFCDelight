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

# Crouch to pick up a raw metal basket from a table or a raised empty-oil fryer.
assert "player.isShiftKeyDown()" in basket
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
assert lang["item.sfcdelight.loaded_raw_basket"] == "裝有生食材的金屬炸籃"
assert "未油炸｜可放上油炸爐加工" in container

print("PASS: portable raw metal basket, ten mixed portions, one fryer, atomic transfer, 15-second cook")
