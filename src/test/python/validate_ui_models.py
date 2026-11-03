"""No fake condiment items and two-block countertop preview."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[3]
def load(p):return json.loads((R/p).read_text(encoding="utf-8"))
left=load("src/main/resources/assets/sfcdelight/models/block/fried_food_cart_upper_counter.json")
assert all(e["faces"]["north"]["texture"] not in ("#bottle","#yellow","#orange") for e in left["elements"])
r=R/"src/main/java/tw/saltedfriedchicken/client/FriedFoodCartContentsRenderer.java"
assert "cart.getPepperJar(),cart.getChiliJar()" in r.read_text(encoding="utf-8")
item=load("src/main/resources/assets/sfcdelight/models/item/preparation_counter.json")
assert len(item["elements"])>12
xx=[e["from"][0] for e in item["elements"]]+[e["to"][0] for e in item["elements"]]
assert max(xx)-min(xx)==32
assert min(xx)==-8 and max(xx)==24
defn=load("src/main/resources/assets/sfcdelight/items/preparation_counter.json")
assert defn["model"]["model"]=="sfcdelight:item/preparation_counter"
pepper=load("src/main/resources/assets/sfcdelight/items/pepper_shaker.json")
assert pepper["model"]["model"]=="sfcdelight:item/pepper_shaker"
print("PASS: two-wide prep model, functional real condiment jars and 1.21.1 white pepper item model")
