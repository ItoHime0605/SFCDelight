"""Regression for user's OPEN food showcase (NOT a glass display cabinet).

No cutout window or shelf-height fake geometry may remain in any placed
showcase segment. The two-piece glass seasoning bottles on the separate
counter, if any, are permitted: they are not a showcase pane.
"""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[3]
asset=root/"src/main/resources/assets/sfcdelight"
models=asset/"models/block"
active=("fried_food_cart_upper_display_left","fried_food_cart_upper_display_right")
legacy=("fried_food_cart_display","fried_food_cart_display_front_left",
        "fried_food_cart_display_front_right","fried_food_cart_display_rear_left",
        "fried_food_cart_display_rear_right")
for name in (*active,*legacy):
    m=json.loads((models/(name+".json")).read_text("utf-8"))
    assert "glass" not in m.get("textures",{}), name
    assert not any(f["texture"]=="#glass" for e in m["elements"]
                   for f in e["faces"].values()), name
for name in active:
    m=json.loads((models/(name+".json")).read_text("utf-8"))
    assert sum(e.get("rotation",{}).get("axis")=="x" and
               e.get("rotation",{}).get("angle")==-45 and
               e["faces"]["up"]["texture"]=="#tray" for e in m["elements"])==1
    assert all(e["from"][2]>=-8 for e in m["elements"])
    assert not any(e.get("rotation",{}).get("angle")==-22.5 for e in m["elements"])
states=json.loads((asset/"blockstates/fried_food_cart.json").read_text("utf-8"))["variants"]
assert len(states)==32
assert states["facing=north,part=4"]["model"].endswith("upper_display_left")
assert states["facing=north,part=5"]["model"].endswith("upper_display_right")
print("PASS: open inclined showcase, no transparent case panes, all 32 block states preserved")
