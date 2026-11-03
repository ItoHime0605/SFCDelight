"""Phase-one art regression: all 12 flat icons and all 17 real 3D baskets.

PNG validation is pure Python so it runs on the normal GitHub Actions runner.
The rendered variants may be transparent in areas outside the icons, but
colors inside the visible area must not be grayscale-only or placeholder.
"""
from pathlib import Path
import json,struct,zlib
root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
icons=("chicken_cutting_knife","bloodletting_knife","pepper_shaker","chili_shaker",
"paper_bag","paper_box","raw_chicken_skin","fried_chicken_skin","fried_mushroom",
"fried_basil","battered_squid_rings","fried_squid_rings")
def load(path):return json.loads(path.read_text(encoding="utf-8"))
from sfcdelight_png import validate_icon
for id in icons:
    model=load(assets/"models/item"/(id+".json"))
    expected="minecraft:item/handheld" if id.endswith("knife") else "minecraft:item/generated"
    assert model["parent"]==expected,id
    assert model["textures"]["layer0"]=="sfcdelight:item/"+id,id
    validate_icon(assets/"textures/item"/(id+".png"))
colours="white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split()
models=["plastic_basket"]+["plastic_basket_"+colour for colour in colours]
shared=load(assets/"models/item/plastic_basket_base.json")
assert len(shared["elements"])>=30
assert shared["display"]["fixed"]["scale"][0]>0
for id in models:
    model=load(assets/"models/item"/(id+".json"))
    assert model["parent"]=="sfcdelight:item/plastic_basket_base",id
    assert model["textures"]["body"]=="sfcdelight:item/"+id,id
    assert (assets/"textures/item"/(id+".png")).exists(),id
# No basket item models are converted to flat sprites, preserving the real
# stack-of-8 3D baskets on the cart's rear rack.
renderer=(root/"src/main/java/tw/saltedfriedchicken/client/FriedFoodCartContentsRenderer.java").read_text(encoding="utf-8")
assert "ItemDisplayContext.FIXED" in renderer
assert "cart.getBasket(basketIndex)" in renderer
assert "cart.basketCount()" in renderer
print("PASS: 12 new 32x32 pixel icons + 17 detailed 3D basket item models, without gameplay changes")
