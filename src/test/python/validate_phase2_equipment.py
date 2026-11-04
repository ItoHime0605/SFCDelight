"""Phase II regression: attractive models without breaking existing data-driven state.

Static checks validate all five remodelled devices and original server mechanics.
Game-client rendering/interaction still requires an in-game test.
"""
import json
import math
import struct
import zlib
from pathlib import Path

root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
models=assets/"models"
block=models/"block"
source=root/"src/main/java/tw/saltedfriedchicken"

def read(path):
    return json.loads(path.read_text(encoding="utf-8"))

def check_model(path, allow_item_extent=False, animated_basket=False):
    obj=read(path)
    assert obj.get("elements"), path
    textures=obj.get("textures",{})
    assert textures, path
    for part in obj["elements"]:
        frm=part["from"];to=part["to"]
        assert len(frm)==3 and len(to)==3, path
        for axis,(a,b) in enumerate(zip(frm,to)):
            assert isinstance(a,(float,int)) and isinstance(b,(float,int)),path
            assert math.isfinite(a) and math.isfinite(b) and a<b,path
            if animated_basket and axis==1:
                # Animated basket crosses the block boundary on purpose; never clamp its lift.
                assert -10<=a<b<=22,path
            else:
                assert (-16 if allow_item_extent else 0)<=a<=b<=(32 if allow_item_extent else 16),path
        for face in part["faces"].values():
            token=face["texture"]
            assert token.startswith("#"),(path,token)
            key=token[1:]
            assert key in textures,(path,key)
    for key,ref in textures.items():
        if ref.startswith("sfcdelight:"):
            sub=ref.split(":",1)[1]
            assert (assets/"textures"/(sub+".png")).exists(),(path,key,ref)
        else:
            assert ref.startswith("minecraft:"),(path,key,ref)
    return obj

def check_png(p):
    data=p.read_bytes()
    assert data[:8]==bytes([137,80,78,71,13,10,26,10]),p
    parts={};pos=8
    while pos<len(data):
        count=struct.unpack_from(">I",data,pos)[0]
        kind=data[pos+4:pos+8]
        chunk=data[pos+8:pos+8+count]
        expected=struct.unpack_from(">I",data,pos+8+count)[0]
        assert zlib.crc32(kind+chunk)==expected,p
        parts[kind]=parts.get(kind,b"")+chunk
        pos+=count+12
        if kind==b"IEND":break
    assert pos==len(data),p
    assert struct.unpack(">IIBBBBB",parts[b"IHDR"])==(128,128,8,6,0,0,0),p
    return parts

materials="oak oak_dark steel dark_steel brass porcelain awning_red awning_cream enamel water plastic food_tray oil".split()
for name in materials:
    check_png(assets/"textures/block"/("nightmarket_"+name+".png"))
assert len(materials)==13

# Cart uses exactly six active block models and preserves the old variant map.
cart_states=read(assets/"blockstates/fried_food_cart.json")["variants"]
assert len(cart_states)==32
cart_models=[
"fried_food_cart_base_counter","fried_food_cart_base_display_left",
"fried_food_cart_base_display_right","fried_food_cart_upper_counter",
"fried_food_cart_upper_display_left","fried_food_cart_upper_display_right"
]
for part,id in enumerate(cart_models):
    assert all(cart_states[f"facing={f},part={part}"]["model"]=="sfcdelight:block/"+id
               for f in ("north","south","east","west")),id
    m=check_model(block/(id+".json"))
    assert len(m["elements"])>=15,id
    assert any(e["faces"]["north"]["texture"]=="#brass" for e in m["elements"]),id
    assert m["textures"]["brass"]=="sfcdelight:block/nightmarket_brass",id
    if part in (4,5):
        assert any(e.get("rotation",{}).get("angle")==-22.5 for e in m["elements"]),id
    if part<=2:
        assert any(e["faces"]["north"]["texture"]=="#wood" for e in m["elements"]),id

# No basket/toy graphics replace the real 3D item being rendered at runtime.
cart_item=check_model(models/"item/fried_food_cart.json",True)
assert len(cart_item["elements"])>=12
cart_renderer=(source/"client/FriedFoodCartContentsRenderer.java").read_text(encoding="utf-8")
assert "for (int row = 0; row < 3; row++)" in cart_renderer
assert "for (int column = 0; column < 4; column++)" in cart_renderer
assert "cart.getBasket(basketIndex)" in cart_renderer

# Fryer keeps precisely the original 5 fuel gauge stages, existing oil surfaces,
# and all LED states; new enclosure must never close the opening.
states=read(assets/"blockstates/frying_station.json")["variants"]
assert len(states)==7 and states["indicator=6"]["model"]=="sfcdelight:block/frying_station_5"
for n in range(6):
    m=check_model(block/(f"frying_station_{n}.json"))
    assert len(m["elements"])>=23,n
    assert m["elements"][5]["faces"]["up"]["texture"]=="#oil",n
    assert m["elements"][6]["faces"]["up"]["texture"]=="#oil",n
    assert len(m["elements"][-(6 if n==0 else 5):])==(6 if n==0 else 5)
for n in range(5):
    check_model(block/(f"fryer_oil_level_{n}.json"))
for mode in ("frying_basket","frying_basket_raised","frying_basket_lifting","frying_basket_submerged"):
    m=check_model(block/(mode+".json"),animated_basket=True)
    assert m["textures"]["metal"]=="sfcdelight:block/nightmarket_steel"

de=check_model(block/"de_oiler.json")
assert len(de["elements"])>=22
assert max(e["to"][1] for e in de["elements"])<=12.2, "Food visible at y=13.4"
de_renderer=(source/"client/DeOilerContentsRenderer.java").read_text(encoding="utf-8")
assert "0.84 + layer * 0.053" in de_renderer
assert "machine.isComplete()" in de_renderer

for part in ("left","right"):
    m=check_model(block/("preparation_counter_"+part+".json"))
    assert len(m["elements"])>=16
left=read(block/"preparation_counter_left.json")
right=read(block/"preparation_counter_right.json")
assert max(e["to"][1] for e in left["elements"])<15
assert any(e["faces"]["up"]["texture"]=="#water" for e in right["elements"])
prep_item=check_model(models/"item/preparation_counter.json",True)
x0=min(e["from"][0] for e in prep_item["elements"])
x1=max(e["to"][0] for e in prep_item["elements"])
assert (x0,x1)==(-8,24)
prep_renderer=(source/"client/PreparationCounterContentsRenderer.java").read_text(encoding="utf-8")
assert "0.5, 0.98, 0.5" in prep_renderer

frame=check_model(models/"item/plastic_frame.json")
assert len(frame["elements"])>=14
assert frame["textures"]["plastic"]=="sfcdelight:block/nightmarket_plastic"
assert "fixed" in frame["display"]
print("PASS: 13 enhanced 128x128 textures; cart (6), fryer (7), oil & basket states, open de-oiler, dual prep bench, 3D showcase tray")
