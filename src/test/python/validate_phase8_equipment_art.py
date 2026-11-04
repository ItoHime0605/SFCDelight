"""Phase 8: ensure the equipment's high-resolution artwork is used in *real*
block models and that 3D details do not obstruct animated cooking areas.

Tests focus on unmodified item/block states and true mesh relief instead of
merely testing whether an image was produced by a standalone art generator.
"""
from pathlib import Path
import json,struct,zlib,hashlib

ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/"src/main/resources/assets/sfcdelight"
MODELS=ASSETS/"models/block"
materials=("oak","oak_dark","steel","dark_steel","brass",
           "awning_red","awning_cream","oil","enamel",
           "food_tray","porcelain","plastic","water")
checksums=set()
for material in materials:
    path=ASSETS/"textures/block"/("nightmarket_"+material+".png")
    png=path.read_bytes()
    assert png[:8]==b"\x89PNG\r\n\x1a\n",path
    offset=8;ihdr=None;data=b""
    while offset<len(png):
        count=struct.unpack_from(">I",png,offset)[0]
        key=png[offset+4:offset+8]
        chunk=png[offset+8:offset+8+count]
        crc=struct.unpack_from(">I",png,offset+8+count)[0]
        assert zlib.crc32(key+chunk)==crc,(path,key)
        if key==b"IHDR":ihdr=struct.unpack(">IIBBBBB",chunk)
        if key==b"IDAT":data+=chunk
        offset+=count+12
        if key==b"IEND":break
    assert offset==len(png),path
    assert ihdr==(128,128,8,6,0,0,0),(path,ihdr)
    assert zlib.decompress(data),path
    checksum=hashlib.sha256(png).hexdigest()
    assert checksum not in checksums,(material,"duplicate texture")
    checksums.add(checksum)

def model(name):
    value=json.loads((MODELS/(name+".json")).read_text(encoding="utf-8"))
    for i,cube in enumerate(value["elements"]):
        for axis,(lo,hi) in enumerate(zip(cube["from"],cube["to"])):
            awning = name.startswith("fried_food_cart_upper_") and axis == 2 and cube["from"][1] >= 13
            allowed_min = -8 if awning else 0
            assert allowed_min <= lo < hi <= 16,(name,i,lo,hi)
        for face in cube["faces"].values():
            assert face["texture"][1:] in value["textures"],(name,face)
    return value

cart=model("fried_food_cart_base_counter")
assert len(cart["elements"])>=26
assert sum(c["faces"]["north"]["texture"]=="#brass" for c in cart["elements"])>=6
brand=model("fried_food_cart_upper_counter")
assert len(brand["elements"])>=32
assert brand["textures"]["brand"]=="sfcdelight:block/nightmarket_sign"
for name in ("fried_food_cart_base_display_left","fried_food_cart_base_display_right"):
    block=model(name)
    assert len(block["elements"])>=24
for stage in range(7):
    fryer=model(f"frying_station_{stage}")
    assert len(fryer["elements"])>=31,(stage,len(fryer["elements"]))
    assert fryer["elements"][5]["faces"]["up"]["texture"]=="#oil"
    assert fryer["elements"][6]["faces"]["up"]["texture"]=="#oil"
    assert any(e["from"]==[2.2,12.15,0.35] for e in fryer["elements"]),(stage,"missing raised front lip")
    # Decorative brackets are strictly outside the open basin; gauges remain last.
    lip=next(e for e in fryer["elements"] if e["from"]==[2.2,12.15,0.35])
    assert lip["to"][2]<=0.65 and lip["to"][1]<=12.6,(stage,"open oil basin")
    tail=6 if stage==0 else 5
    assert all(e["faces"]["north"]["texture"] in
               {"#off","#lamp_0","#lamp_1","#lamp_2","#lamp_3","#lamp_4","#lamp_5"}
               for e in fryer["elements"][-tail:]),(stage,"original gauge order")
oiler=model("de_oiler")
assert len(oiler["elements"])>=30
assert max(cube["to"][1] for cube in oiler["elements"])<=12.2
for kind in ("left","right"):
    bench=model("preparation_counter_"+kind)
    assert any(cube["from"]==[1.5,1.65,1.7] for cube in bench["elements"])
    assert len(bench["elements"])>=21

states=json.loads((ASSETS/"blockstates/fried_food_cart.json").read_text(encoding="utf-8"))
assert len(states["variants"])==32
fryer_states=json.loads((ASSETS/"blockstates/frying_station.json").read_text(encoding="utf-8"))
assert len(fryer_states["variants"])==7
# The user explicitly chose an OPEN inclined display with NO glass canopy.
showcase=model("fried_food_cart_upper_display_left")
assert "glass" not in showcase["textures"]
assert len([e for e in showcase["elements"] if e.get("rotation",{}).get("angle")==-45
            and e["faces"]["up"]["texture"]=="#tray"])==1
print("PASS: 13 distinct 128x128 equipment materials, one open cart incline, equipment states intact")
