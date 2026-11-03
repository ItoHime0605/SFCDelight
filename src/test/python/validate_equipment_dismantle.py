"""Block lifecycle regression for all equipment, game rules and container transfers."""
from pathlib import Path
P=Path(__file__).resolve().parents[3]/"src/main/java/tw/saltedfriedchicken"
def read(n): return (P/n).read_text(encoding="utf-8")
def check_lifecycle(block, entity):
    s=read(block); e=read(entity)
    assert "playerWillDestroy" in s and "getAbilities().instabuild" in s, block
    assert "onRemove" in s and "RULE_DOBLOCKDROPS" in s, block
    assert "shouldDropEquipment()" in s and "suppressEquipmentDrop()" in s, block
    assert "private boolean suppressEquipmentDrop;" in e, entity
    assert "suppressEquipmentDrop = true;" in e, entity
    assert 'tag.putBoolean("suppressEquipmentDrop"' not in e, entity

for block,entity in [
    ("FriedPackagingBlock.java","FriedPackagingBlockEntity.java"),
    ("FryingBasketBlock.java","FryingBasketBlockEntity.java"),
    ("DeOilerBlock.java","DeOilerBlockEntity.java"),
    ("FryingStationBlock.java","FryingStationBlockEntity.java"),
]: check_lifecycle(block,entity)

package=read("FriedPackagingBlock.java")
basket=read("FryingBasketBlock.java")
deoiler=read("DeOilerBlock.java")
fryer=read("FryingStationBlock.java")
assert "container.suppressEquipmentDrop();" in package
assert "int moved = container.putPortions(foods, capacity);" in package
assert "FriedBatchData.countPortions(" in package
assert "player.isShiftKeyDown()" in package
assert "FoodContentsTooltip.summarize(stored, true)" in package
assert "if (container.isEmpty())" in package
assert "SaltedFriedChickenDelight.PAPER_BAG_ITEM.get()" in package
assert "SaltedFriedChickenDelight.PAPER_BOX_ITEM.get()" in package
assert "FriedBatchData.transfer(stored" in package
assert "FryingRecipes.isValidCookedBatch(" in package
assert basket.count("basket.suppressEquipmentDrop();") == 3  # 2 pick-ups + creative dismantle
assert "SaltedFriedChickenDelight.LOADED_RAW_BASKET.get()" in basket
assert "SaltedFriedChickenDelight.HOT_FOOD_BASKET.get()" in basket
assert "FryingRecipes.isValidRawBatch(foods" in basket
assert "FryingRecipes.isValidCookedBatch(foods" in basket
assert "SaltedFriedChickenDelight.DEOILER_ITEM.get()" in deoiler
assert "machine.dropContents();" in deoiler
assert "for (ItemStack food : FriedBatchData.readAll(stored, level.registryAccess()))" not in deoiler
assert "Only the physical machine is recovered" in deoiler
assert "FryingBasketBlock.dropPortableBasket(level, basketPos, basket);" in fryer
assert "basket.extractAll();" in fryer and "basket.suppressEquipmentDrop();" in fryer
assert "level.removeBlock(basketPos, false);" in fryer
assert "FryingBasketBlock.dropPortableBasket" in fryer
assert "public static void dropPortableBasket(" in basket
assert "FryingRecipes.isValidRawBatch(foods" in basket
assert "FryingRecipes.isValidCookedBatch(foods" in basket
cart = read("FriedFoodCartBlock.java")
assert "cart.drainDisplay()" in cart and "cart.drainFrames()" in cart
assert "cart.popBasket()" in cart and "cart.takePepperJar()" in cart
assert "RULE_DOBLOCKDROPS" in cart
assert "SaltedFriedChickenDelight.FRYER_ITEM.get()" in fryer
assert fryer.count("rollback.suppressEquipmentDrop();") == 2
assert "|| !basket.restoreRaw(raw))" in fryer
assert "|| !basket.restoreCooked(foods))" in fryer
print("PASS: cart returns real inventory; de-oiler discards food; fryer returns its basket by raw/cooked state")
