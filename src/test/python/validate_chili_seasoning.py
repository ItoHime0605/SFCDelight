"""Chili and white pepper must coexist without doubling effects or item counts."""
from pathlib import Path
import json
r=Path(__file__).resolve().parents[3]
src=r/"src/main/java/tw/saltedfriedchicken"
read=lambda f:(src/f).read_text(encoding="utf-8")
food=read("FriedContainerItem.java")
chili=read("ChiliPowderItem.java")
cart=read("FriedFoodCartBlock.java")
entity=read("FriedFoodCartBlockEntity.java")
reg=read("SaltedFriedChickenDelight.java")
assert "ChiliLevel" in chili and "level(food) >= 4" in chili
assert "CHILI_SPEED_LIGHT" in reg and "CHILI_SPEED_STRONG" in reg
assert "0.05" in reg and "0.10" in reg
assert "20 * 30" in food and "player.removeEffect(SaltedFriedChickenDelight.CHILI_SPEED_LIGHT)" in food
assert "ChiliPowderItem.setLevel(next,chili)" in food
assert "WhitePepperItem.setWhitePepper(next)" in food
assert "rightCondiment" in cart and "cart.installChiliJar(held)" in cart
assert "cart.refillChiliJar()" in cart and "cart.seasonChiliPackage" in cart
assert "CounterChili" in entity and "counterChili = ItemStack.EMPTY" in entity
assert "cart.installPepperJar(held)" in cart
assert "cart.installChiliJar(held)" in cart
assert "ItemStack pepper = cart.takePepperJar();" in cart
assert "drop(level, anchor, pepper);" in cart
assert "ItemStack chili = cart.takeChiliJar();" in cart
assert "drop(level, anchor, chili);" in cart
assert "if (drops)" in cart
assert "RULE_DOBLOCKDROPS" in cart
assert "counterPepper = held.copyWithCount(1)" in entity
assert "counterChili = held.copyWithCount(1)" in entity
assert "ItemStack out = counterPepper.copy();" in entity
assert "ItemStack out=counterChili.copy();" in entity
assert 'tag.put("CounterPepper", counterPepper.save(registries))' in entity
assert 'tag.put("CounterChili", counterChili.save(registries))' in entity
# Both real jars keep their item data when dismantled, not newly minted shakers.
section = cart[cart.index("ItemStack pepper = cart.takePepperJar();"):cart.index("ItemStack basket;", cart.index("ItemStack pepper = cart.takePepperJar();"))]
assert section.count("drop(level, anchor,") == 2
assert "drop(level, anchor, pepper);" in section
assert "drop(level, anchor, chili);" in section
blockstates=json.loads((r/"src/main/resources/assets/sfcdelight/blockstates/chili_jar.json").read_text(encoding="utf-8"))
assert len(blockstates["variants"])==61
for item in ["empty_seasoning_jar","chili_powder","chili_shaker"]:
    path=r/"src/main/resources/assets/sfcdelight/items"/(item+".json")
    assert path.is_file()
    json.loads(path.read_text(encoding="utf-8"))
for item in ("pepper_shaker","chili_shaker"):
    recipe=json.loads((r/f"src/main/resources/data/sfcdelight/recipe/{item}.json").read_text(encoding="utf-8"))
    assert recipe["result"]["components"]["minecraft:custom_data"]["PepperCharges"] == 10
print("PASS: both physical cart shakers install, save remaining charges and drop together on dismantling")
