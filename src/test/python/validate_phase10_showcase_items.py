"""Phase 10: visible showcase joinery and improved movable item models.

This pass preserves the food viewport, original tray geometry and all blockstates.
It does not alter right-click hit tests, gameplay registration, storage or recipes.
"""
from pathlib import Path
import json,struct
root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
modelroot=assets/"models"
def model(path):
    return json.loads((modelroot/path).read_text(encoding="utf-8"))
def present(m,a,b,texture):
    return any(e["from"]==a and e["to"]==b and e["faces"]["north"]["texture"]=="#"+texture
               for e in m["elements"])
for side in ("left","right"):
    m=model(Path("block")/("fried_food_cart_upper_display_"+side+".json"))
    assert m["render_type"]=="minecraft:cutout"
    assert m["textures"]["glass"]=="sfcdelight:block/cart_glass"
    assert len(m["elements"])==59
    assert present(m,[7.87,1.2,0.07],[8.13,12.78,0.26],"steel")
    assert present(m,[0.52,12.78,0.09],[15.48,13.04,0.27],"brass")
    for y in (2.48,6.68,10.88):
        assert present(m,[1.15,y,1.83],[7.45,y+0.16,2.05],"brass")
        assert present(m,[8.5,y,1.83],[14.8,y+0.16,2.05],"brass")
    # Glass remains an unchanged, cutout-based viewing pane, not a solid panel.
    assert present(m,[0,0.8,0.3],[16,13.2,0.5],"glass")
    assert present(m,[0,0.5,14.5],[16,13.5,15.1],"glass")
    assert all(0<=low<high<=16 for e in m["elements"] for low,high in zip(e["from"],e["to"]))
frame=model(Path("item/plastic_frame.json"))
assert len(frame["elements"])==18
assert present(frame,[5.55,2.65,1.18],[10.45,3.05,1.78],"enamel")
assert present(frame,[5.55,2.65,14.22],[10.45,3.05,14.82],"enamel")
for context in ("firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand","gui","fixed"):
    assert context in frame["display"]
basket=model(Path("item/frying_basket.json"))
assert basket["parent"]=="sfcdelight:block/frying_basket"
for context in ("gui","ground","fixed","firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand"):
    assert context in basket["display"]
assert basket["display"]["firstperson_righthand"]["rotation"][1]>0
assert basket["display"]["firstperson_lefthand"]["rotation"][1]<0
assert basket["display"]["firstperson_righthand"]["scale"][0]<0.7
assert basket["display"]["firstperson_lefthand"]["scale"][0]<0.7
for name in ("nightmarket_brass","nightmarket_steel","nightmarket_enamel"):
    data=(assets/"textures/block"/(name+".png")).read_bytes()
    assert struct.unpack(">II",data[16:24])==(128,128)
assert len(json.loads((assets/"blockstates/fried_food_cart.json").read_text("utf-8"))["variants"])==32
print("PASS: upper display cutout and 3x2 shelf relief, practical grab rails, symmetric handheld items")
