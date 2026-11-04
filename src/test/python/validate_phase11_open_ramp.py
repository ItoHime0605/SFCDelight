"""Single-object cart redesign regression: no glass, single tilted 4x3 surface.

The concept artwork's food is illustrative, not preinstalled inventory.
Awnings overhang customers by 1/2 block; rear light poles and front
lanterns in the concept are background props, NOT added block models.
"""
from pathlib import Path
import json, math, re, struct
root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
source=root/"src/main/java/tw/saltedfriedchicken"
def load(path): return json.loads(path.read_text(encoding="utf-8"))
def model(name): return load(assets/"models/block"/(name+".json"))
def coords(e,from_,to_):
    return e["from"]==from_ and e["to"]==to_
for name in ("fried_food_cart_upper_display_left",
             "fried_food_cart_upper_display_right"):
    m=model(name)
    assert "glass" not in m["textures"],name
    assert not any(f["texture"]=="#glass" for e in m["elements"] for f in e["faces"].values())
    pads=[e for e in m["elements"] if
          e.get("rotation",{}).get("angle")==-45
          and e["faces"]["up"]["texture"]=="#tray"]
    assert len(pads)==1,name
    assert coords(pads[0],[1.05,7.61,0.7],[14.95,8.04,15.3])
    assert sum(e.get("rotation",{}).get("angle")==-22.5 for e in m["elements"])==0
    for z in (5.33,10.67):
        assert any(coords(e,[1.12,7.97,z],[14.88,8.23,z+0.25])
                   for e in m["elements"]), (name,"row dividers",z)
    for k in range(4,16):
        assert m["elements"][k]["from"][2]==-8,(name,"awning overhang",k)
    assert any(e["faces"]["up"]["texture"]=="#glow" and
               coords(e,[1.45,13.12,-5.72],[14.55,13.41,-5.29])
               for e in m["elements"]),(name,"illuminated strip")
    # Deliberately empty until genuine display-frame inventory is installed.
    assert "fixed_food" not in m["textures"] and "sample_food" not in m["textures"]
    assert all(-8<=e["from"][2]<e["to"][2]<=16 for e in m["elements"])
counter=model("fried_food_cart_upper_counter")
assert all(counter["elements"][k]["from"][2]==-8 for k in range(4,16))
assert any(e["faces"]["up"]["texture"]=="#glow" and
           coords(e,[1.45,13.12,-5.72],[14.55,13.41,-5.29])
           for e in counter["elements"])
item=load(assets/"models/item/fried_food_cart.json")
assert sum(e.get("rotation",{}).get("angle")==-45 and
           e["faces"]["up"]["texture"]=="#tray" for e in item["elements"])==1
assert sum(e["faces"]["north"]["texture"]=="#lamp" for e in item["elements"])==2
# Item's small seasoning jars are retained, but its large showcase pane is gone.
assert sum(e["faces"]["north"]["texture"]=="#glass" for e in item["elements"])==3
assert not any(coords(e,[-10,12,12.5],[26,21,14]) for e in item["elements"])
assert all(-16<=e["from"][i]<e["to"][i]<=32 for e in item["elements"] for i in range(3))
block=(source/"FriedFoodCartBlock.java").read_text("utf-8")
render=(source/"client/FriedFoodCartContentsRenderer.java").read_text("utf-8")
registration=(source/"SaltedFriedChickenDelight.java").read_text("utf-8")
assert "Direction.UP" in block
assert "double depth = 0.5 - dx * front.getStepX() - dz * front.getStepZ();" in block
assert "Math.floor(depth * 3.0)" in block
assert "DISPLAY_NORTH" in block and "DISPLAY_EAST" in block
assert "DISPLAY_SOUTH" in block and "DISPLAY_WEST" in block
assert "case NORTH -> box(0, 0, near, 16, height, far);" in block
assert "double depth = (row + 0.5) / 3.0;" in render
assert "double y = 1.25 + depth * 0.65;" in render
assert "rotationDegrees(-45.0f)" in render
assert "rotationDegrees(-32.0f)" in render
assert "for (int row = 0; row < 3; row++)" in render
assert "for (int column = 0; column < 4; column++)" in render
assert "if (!cart.hasFrame(slot)) continue;" in render
assert "cart.getDisplayStack(slot)" in render
assert 'PART) >= 3 && state.getValue(FriedFoodCartBlock.PART) <= 5 ? 12 : 0' in registration
# Click target vs actual rendered centers: 12 slots in ALL four orientations.
cardinals=[(0,-1),(1,0),(0,1),(-1,0)]
for fx,fz in cardinals:
    rx,rz=-fz,fx
    visited=set()
    for row in range(3):
        for col in range(4):
            depth=(row+0.5)/3
            across=0.5+(col+0.5)*0.5
            front_offset=0.5-depth
            wx=0.5+rx*across+fx*front_offset
            wz=0.5+rz*across+fz*front_offset
            part=4+col//2
            sx,sz=rx*(part-3),rz*(part-3)
            dx=wx-(sx+0.5);dz=wz-(sz+0.5)
            hit_width=(part-4)+0.5+dx*rx+dz*rz
            hit_depth=0.5-dx*fx-dz*fz
            chosen_col=min(3,int(math.floor(hit_width*2)))
            chosen_row=min(2,int(math.floor(hit_depth*3)))
            assert chosen_col==col and chosen_row==row,(fx,fz,row,col,chosen_row,chosen_col)
            visited.add(chosen_row*4+chosen_col)
    assert visited==set(range(12))
states=load(assets/"blockstates/fried_food_cart.json")["variants"]
assert len(states)==32
print("PASS: 1 front-low/rear-high slope, 12 selectable frames all directions, open viewport, half-block awning and real light tube")
