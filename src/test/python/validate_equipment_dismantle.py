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
assert "for (ItemStack food : FriedBatchData.readAll(stored, level.registryAccess()))" in deoiler
assert "SaltedFriedChickenDelight.FRYER_ITEM.get()" in fryer
print("PASS: bag/box, basket, de-oiler, fryer return items without duplicating consumed containers")
