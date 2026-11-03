"""Food package portions: exact 0.5 + 0.1 saturation, without legacy fire buffs."""
from pathlib import Path
root=Path(__file__).resolve().parents[3]/"src/main/java/tw/saltedfriedchicken"
def java(name):return (root/name).read_text(encoding="utf-8")
food=java("FriedContainerItem.java")
white=java("WhitePepperItem.java")
cart=java("FriedFoodCartBlockEntity.java")
assert "player.getFoodData().eat(1, whitePepper ? 0.30f : 0.25f)" in food
assert "WhitePepperItem.hasWhitePepper(packageStack)" in food
assert "WhitePepperItem.setWhitePepper(next)" in food
assert "player.igniteForSeconds" not in food and "FIRE_RESISTANCE" not in food
assert "hasWhitePepper" in white and "tag.putBoolean(\"WhitePepper\", true)" in white
assert 'tag.getInt("PepperLevel") > 0' in white
assert "WhitePepperItem.hasWhitePepper(food)" in cart
assert "WhitePepperItem.setWhitePepper(food)" in cart
for f in ("PepperJarBlockItem.java", "PepperShakerItem.java"):
    assert "WhitePepperItem.hasWhitePepper(other)" in java(f)
    assert "WhitePepperItem.setWhitePepper(other)" in java(f)
print("PASS: one eaten portion restores 1 hunger, 0.5 saturation (+0.1 white pepper), no stacking or burning")
