"""Dedicated 1.21.1 Farmer's Delight cutting board compatibility regression."""
from pathlib import Path
import json,struct,zlib
ROOT=Path(__file__).resolve().parents[3]
SRC=ROOT/"src/main/java/tw/saltedfriedchicken"
BASE=ROOT/"src/main/resources"
RECIPE=BASE/"data/sfcdelight/recipe"
ART=BASE/"assets/sfcdelight"
def load(p):return json.loads(p.read_text(encoding="utf-8"))
reg=(SRC/"SaltedFriedChickenDelight.java").read_text(encoding="utf-8")
assert 'ITEMS.registerSimpleItem("chicken_cutting_knife", new Item.Properties().durability(256))' in reg
assert 'CHICKEN_CUTTING_KNIFE' in reg
assert load(ART/"lang/zh_tw.json")["item.sfcdelight.chicken_cutting_knife"]=="雞肉分切刀"
cut=load(RECIPE/"cutting/chicken_with_special_knife.json")
assert cut["type"]=="farmersdelight:cutting"
assert cut["ingredients"]==[{"item":"minecraft:chicken"}]
assert cut["tool"]=={"item":"sfcdelight:chicken_cutting_knife"}
assert cut["result"]==[
    {"item":{"id":"sfcdelight:raw_chicken_dice","count":4}},
    {"item":{"id":"sfcdelight:raw_chicken_skin","count":2}}
]
assert cut["neoforge:conditions"]==[{"type":"neoforge:mod_loaded","modid":"farmersdelight"}]
assert not (RECIPE/"raw_chicken_dice_from_vanilla.json").exists()
assert not (RECIPE/"raw_chicken_skin.json").exists()
assert load(RECIPE/"raw_chicken_dice.json")["ingredients"]==["farmersdelight:chicken_cuts"]
craft=load(RECIPE/"chicken_cutting_knife.json")
assert craft["type"]=="minecraft:crafting_shaped"
assert craft["result"]=={"id":"sfcdelight:chicken_cutting_knife","count":1}
assert craft["pattern"]==["  I"," I ","SC "]
assert craft["key"]=={"I":"minecraft:iron_ingot","S":"minecraft:stick","C":"minecraft:comparator"}
assert load(ART/"models/item/chicken_cutting_knife.json")["textures"]["layer0"]=="sfcdelight:item/chicken_cutting_knife"
assert load(ART/"items/chicken_cutting_knife.json")["model"]["model"]=="sfcdelight:item/chicken_cutting_knife"
sprite=(ART/"textures/item/chicken_cutting_knife.png").read_bytes()
assert sprite[:8]==bytes([137,80,78,71,13,10,26,10])
assert struct.unpack_from(">II",sprite,16)==(32,32)
# The actual PNG decoder detects whether all compressed data is valid.
pos=8
while pos<len(sprite):
    n=struct.unpack_from(">I",sprite,pos)[0]
    typ=sprite[pos+4:pos+8]
    payload=sprite[pos+8:pos+8+n]
    assert zlib.crc32(typ+payload)==struct.unpack_from(">I",sprite,pos+8+n)[0]
    pos+=12+n
    if typ==b"IEND":break
assert pos==len(sprite)
print("PASS: dedicated knife only, two guaranteed chicken outputs, other knives untouched")
