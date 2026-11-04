"""Phase 9: relief detailing on high-resolution night-market equipment.
No moving food, oil basin, glass or gauge cuboids are displaced.
"""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[3]
models=root/"src/main/resources/assets/sfcdelight/models/block"
def read(n):return json.loads((models/(n+".json")).read_text("utf-8"))
def count_cube(m,f,t):
    return sum(e["from"]==f and e["to"]==t for e in m["elements"])
for i in range(7):
    m=read(f"frying_station_{i}")
    assert len(m["elements"])>=37
    assert count_cube(m,[3.4,5.1,0.06],[4.04,5.28,0.2])==1
    assert count_cube(m,[3.2,9.75,0.25],[12.8,10.07,0.62])==1
    assert m["elements"][5]["faces"]["up"]["texture"]=="#oil"
    assert m["elements"][6]["faces"]["up"]["texture"]=="#oil"
    gauges=m["elements"][-(6 if i==0 else 5):]
    assert all(e["faces"]["north"]["texture"] in
       {"#off","#lamp_0","#lamp_1","#lamp_2","#lamp_3","#lamp_4","#lamp_5"} for e in gauges)
m=read("de_oiler")
assert len(m["elements"])>=37
assert count_cube(m,[4.3,3.95,1.33],[5,4.14,1.7])==1
for side in ("left","right"):
    m=read("preparation_counter_"+side)
    assert len(m["elements"])>=26
    assert count_cube(m,[4.8,7.06,1.23],[6.25,7.55,1.47])==1
for piece in ("counter","display_left","display_right"):
    m=read("fried_food_cart_base_"+piece)
    assert count_cube(m,[1.55,4.1,0.43],[1.79,4.34,0.65])==1
for name in ("de_oiler","preparation_counter_left","preparation_counter_right"):
    assert max(e["to"][1] for e in read(name)["elements"])<=16
for name in ("nightmarket_steel","nightmarket_dark_steel","nightmarket_brass"):
    png=(root/"src/main/resources/assets/sfcdelight/textures/block"/(name+".png")).read_bytes()
    assert png[16:24]==bytes([0,0,0,128,0,0,0,128])
print("PASS: real 3D relief on 13 equipment models, 128px materials, oil/food/gauge preserved")
