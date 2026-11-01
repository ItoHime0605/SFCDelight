from pathlib import Path
root = Path(__file__).resolve().parents[1] / 'src/main/java/tw/saltedfriedchicken'
recipes = (root / 'FryingRecipes.java').read_text()
station = (root / 'FryingStationBlock.java').read_text()
basket = (root / 'FryingBasketBlockEntity.java').read_text()
assert 'boolean isCookedOutput(ItemStack food)' in recipes
assert 'boolean isValidCookedBatch(List<ItemStack> foods, int maxPortions)' in recipes
assert 'foods.stream().allMatch(food -> isCookedOutput(food)' in recipes
assert 'isValidCookedBatch(foods, FryingBasketBlockEntity.MAX_PIECES)' in station
assert 'isValidCookedBatch(cooked, MAX_PIECES)' in basket
assert 'setPose(true, false);' in basket
print('熟炸籃防護靜態檢查通過（非編譯或實機測試）')
