"""Static checks for newly introduced foods, craftables and 16-color baskets."""
from pathlib import Path
import json, struct, zlib
from sfcdelight_png import validate_icon
ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT/"src/main/java/tw/saltedfriedchicken"
A = ROOT/"src/main/resources/assets/sfcdelight"
REC = ROOT/"src/main/resources/data/sfcdelight/recipe"
reg=(SRC/"SaltedFriedChickenDelight.java").read_text(encoding="utf-8")
fry=(SRC/"FryingRecipes.java").read_text(encoding="utf-8")
cart=(SRC/"FriedFoodCartBlockEntity.java").read_text(encoding="utf-8")
lang=json.loads((A/"lang/zh_tw.json").read_text(encoding="utf-8"))
tag=json.loads((ROOT/"src/main/resources/data/sfcdelight/tags/item/display_ingredients.json").read_text(encoding="utf-8"))
foods={
"raw_chicken_skin":"生雞皮","fried_chicken_skin":"炸雞皮",
"fried_mushroom":"炸香菇","fried_basil":"酥炸九層塔",
"battered_squid_rings":"裹粉魷魚圈","fried_squid_rings":"炸魷魚圈"
}
for food,translated in foods.items():
    assert '"'+food+'"' in reg,food
    assert lang["item.sfcdelight."+food]==translated
    model=json.loads((A/"models/item"/(food+".json")).read_text(encoding="utf-8"))
    item=json.loads((A/"items"/(food+".json")).read_text(encoding="utf-8"))
    assert model["textures"]["layer0"]=="sfcdelight:item/"+food
    assert item["model"]["model"]=="sfcdelight:item/"+food
    validate_icon(A/"textures/item"/(food+".png"))

for inp,out in [
("RAW_CHICKEN_SKIN","FRIED_CHICKEN_SKIN"),("Items.BROWN_MUSHROOM","FRIED_MUSHROOM"),
("BASIL","FRIED_BASIL"),("BATTERED_SQUID_RINGS","FRIED_SQUID_RINGS")]:
    assert inp in fry and out in fry
for id in ("sfcdelight:raw_chicken_skin","minecraft:brown_mushroom",
           "sfcdelight:basil","sfcdelight:battered_squid_rings"):
    assert id in tag["values"]
assert "DISPLAY_SLOTS = 12" in cart
assert "MAX_PIECES = 10" in (SRC/"FryingBasketBlockEntity.java").read_text(encoding="utf-8")

names=["frying_station","de_oiler","frying_basket","steel_scoop",
       "fried_food_cart","paper_bag","paper_box","plastic_basket",
       "basil_seeds","garlic_clove_from_allium","cooking_oil_from_seeds",
       "chicken_cutting_knife","battered_squid_rings"]
colors="white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split()
names += ["plastic_basket_"+c for c in colors]
assert len(names)==29
for name in names:
    r=json.loads((REC/(name+".json")).read_text(encoding="utf-8"))
    assert r["type"] in ("minecraft:crafting_shaped","minecraft:crafting_shapeless")
    assert r["result"]["id"].startswith("sfcdelight:")
    assert r["result"]["count"]>=1
    if "pattern" in r:
        assert 1<=len(r["pattern"])<=3
        assert 1<=len(r["pattern"][0])<=3
        assert all(len(row)==len(r["pattern"][0]) for row in r["pattern"])
        assert set("".join(r["pattern"]))-{" "} == set(r["key"])
    else:
        assert 1<=len(r["ingredients"])<=9
    if name.startswith("plastic_basket_"):
        assert r["result"]["id"]=="sfcdelight:"+name
        assert r["key"]["D"]=={"item":"minecraft:"+name[len("plastic_basket_"):]+"_dye"}
        assert {"item":"sfcdelight:plastic_basket"} not in r["key"].values()
    if name=="battered_squid_rings":
        assert r["neoforge:conditions"]==[{"type":"neoforge:mod_loaded","modid":"oceansdelight"}]
print("PASS: 6 32px sprites, 4 cooked varieties, 29 crafting recipes, 16 safe dye colors, 12-slot cart")
