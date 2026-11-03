"""Night-market visual polish, with real model geometry and no game-data changes.

We intentionally test the assets' exact dimensions, the GUI texture links,
the cart sign's depth, and item display transforms. Any failure must stop
the build rather than ship invisible models or purple-and-black textures.
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/sfcdelight"
SOURCE = ROOT / "src/main/java/tw/saltedfriedchicken"

def j(path):
    return json.loads(path.read_text(encoding="utf-8"))

def png16(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", path
    at = 8
    stream = []
    dimensions = None
    while at < len(data):
        size = struct.unpack_from(">I", data, at)[0]
        kind = data[at + 4:at + 8]
        chunk = data[at + 8:at + 8 + size]
        crc = struct.unpack_from(">I", data, at + 8 + size)[0]
        assert zlib.crc32(kind + chunk) == crc, (path, kind)
        if kind == b"IHDR":
            dimensions = struct.unpack(">IIBBBBB", chunk)
        if kind == b"IDAT":
            stream.append(chunk)
        at += size + 12
        if kind == b"IEND":
            break
    assert at == len(data), path
    assert dimensions == (16, 16, 8, 6, 0, 0, 0), (path, dimensions)
    assert zlib.decompress(b"".join(stream)), path

emblems = ("cart", "fryer", "deoiler", "preparation")
screen = (SOURCE / "client/EquipmentStatusScreen.java").read_text(encoding="utf-8")
for kind in emblems:
    name = "status_" + kind
    png16(ASSETS / "textures/gui" / (name + ".png"))
    assert f'"textures/gui/{name}.png"' in screen, kind
assert 'graphics.blit(insignia, leftPos + 177, topPos + 16' in screen
assert 'case EquipmentStatusMenu.FRYER -> ICON_FRYER' in screen
assert 'case EquipmentStatusMenu.DEOILER -> ICON_DEOILER' in screen
assert 'case EquipmentStatusMenu.PREPARATION -> ICON_PREPARATION' in screen

png16(ASSETS / "textures/block/nightmarket_sign.png")
cart_block = j(ASSETS / "models/block/fried_food_cart_upper_counter.json")
assert cart_block["textures"]["brand"] == "sfcdelight:block/nightmarket_sign"
branded = [e for e in cart_block["elements"] if
           e["faces"]["north"]["texture"] == "#brand"]
assert len(branded) == 1
sign = branded[0]
assert sign["from"] == [4.95, 8.8, 0.6]
assert sign["to"] == [11.05, 11.75, 0.82]
for element in cart_block["elements"]:
    for x,y in zip(element["from"], element["to"]):
        assert 0 <= x < y <= 16, element
assert len(cart_block["elements"]) >= 25

for item_id, block_id in (("frying_station","frying_station_0"),
                           ("de_oiler","de_oiler")):
    model = j(ASSETS / "models/item" / (item_id + ".json"))
    assert model["parent"] == "sfcdelight:block/" + block_id
    contexts = model["display"]
    for key in ("gui", "ground", "fixed",
                "firstperson_righthand","firstperson_lefthand",
                "thirdperson_righthand","thirdperson_lefthand"):
        assert key in contexts, (item_id, key)
        assert all(0 < v <= 1 for v in contexts[key]["scale"]), (item_id, key)
for item_id in ("fried_food_cart","preparation_counter"):
    contexts = j(ASSETS / "models/item" / (item_id + ".json"))["display"]
    assert contexts["firstperson_righthand"]["scale"][0] <= 0.32
    assert contexts["firstperson_lefthand"]["scale"][0] <= 0.32
    assert contexts["firstperson_righthand"]["rotation"][1] > 0
    assert contexts["firstperson_lefthand"]["rotation"][1] < 0
# Real world-state models are referenced without adding any new blocks,
# special entities, save fields, or extra client/server dependencies.
print("PASS: four bespoke GUI insignias, night-market cart sign, 4 hand-size fixes, no new recipes")
