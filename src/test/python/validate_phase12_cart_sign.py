"""0.2.69-alpha roof plaque, scalloped canvas and illuminated unglazed food-display tests."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/"src/main/resources/assets/sfcdelight"
JAVA=ROOT/"src/main/java/tw/saltedfriedchicken/SaltedFriedChickenDelight.java"
def read(name):
    return json.loads((ASSETS/"models/block"/(name+".json")).read_text("utf-8"))
upper=read("fried_food_cart_upper_counter")
left=read("fried_food_cart_upper_display_left")
right=read("fried_food_cart_upper_display_right")
for obj in (upper,left,right):
    assert any(e["faces"]["up"]["texture"]=="#glow" and e.get("shade") is False
               for e in obj["elements"])
    assert sum(e["from"][2]<-7.9 and e["from"][1]<13 for e in obj["elements"])==6
assert not any(e["faces"]["north"]["texture"]=="#brand" for e in upper["elements"])
plaques=[]
for i,obj in enumerate((left,right)):
    p=[e for e in obj["elements"] if e["faces"]["north"]["texture"]=="#brand"]
    assert len(p)==1
    assert p[0]["faces"]["north"]["uv"]==([0,0,8,16] if i==0 else [8,0,16,16])
    assert 16<p[0]["from"][1] and p[0]["to"][1]<25
    assert obj["textures"]["brand"]=="sfcdelight:block/nightmarket_sign"
    assert sum(e.get("rotation",{}).get("angle")==-45 and
               e["faces"]["up"]["texture"]=="#tray" for e in obj["elements"])==1
    assert "glass" not in obj["textures"]
    plaques.append(p[0])
assert plaques[0]["to"][0]==16 and plaques[1]["from"][0]==0
item=json.loads((ASSETS/"models/item/fried_food_cart.json").read_text("utf-8"))
assert len([e for e in item["elements"] if e["faces"]["north"]["texture"]=="#brand"])==1
java=JAVA.read_text("utf-8")
assert "PART) >= 3 && state.getValue(FriedFoodCartBlock.PART) <= 5 ? 12 : 0" in java
print("PASS: central roof board, 12 empty click slots, joined sign UV, scalloped valance, triple illuminated tubes")
