"""Phase ten design update: open, single inclined showcase; no pane or shelf."""
from pathlib import Path
import json, struct
root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
models=assets/"models"
def model(file):
    return json.loads((models/file).read_text("utf-8"))
def present(m,a,b,texture):
    return any(e["from"]==a and e["to"]==b and e["faces"]["north"]["texture"]=="#"+texture
               for e in m["elements"])
for side in ("left","right"):
    m=model(Path("block")/("fried_food_cart_upper_display_"+side+".json"))
    assert "glass" not in m["textures"]
    assert all(e["faces"]["up"]["texture"]!="#glass" for e in m["elements"])
    assert sum(e.get("rotation",{}).get("angle")==-45 and
               e["faces"]["up"]["texture"]=="#tray" for e in m["elements"])==1
    assert present(m,[1.05,7.61,0.7],[14.95,8.04,15.3],"tray")
    assert present(m,[1.05,7.95,0.7],[14.95,8.52,1.13],"steel")
    assert all(0<=lo<hi<=16 or
               (axis==2 and e["from"][1]>=13 and -8<=lo<hi<=16)
               for e in m["elements"]
               for axis,(lo,hi) in enumerate(zip(e["from"],e["to"])))
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
print("PASS: one open 45-degree cart slope, original item ergonomics and 128px metal textures")
