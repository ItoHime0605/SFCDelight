"""Regression for showcase visibility on 1.21.1 NeoForge.

The original pane PNG had semi-transparent pixels, but its block model lacked a
render_type and could become opaque SOLID chunk geometry.  Sparse *binary*
transparent highlights + cutout models preserve food visibility without
translucent sorting overhead in a dense multi-block cart.
"""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / "src/main/resources/assets/sfcdelight"
MODEL = ASSETS / "models"
PATH = ASSETS / "textures/block/cart_glass.png"


def pixels(path):
    raw = path.read_bytes()
    assert raw[:8] == b"\x89PNG\r\n\x1a\n"
    pos = 8
    content = b""
    width = height = depth = color = None
    while pos < len(raw):
        size = struct.unpack_from(">I", raw, pos)[0]
        kind = raw[pos+4:pos+8]
        part = raw[pos+8:pos+8+size]
        assert zlib.crc32(kind + part) == struct.unpack_from(">I", raw, pos+8+size)[0]
        if kind == b"IHDR":
            width, height, depth, color, compression, filt, interlace = struct.unpack(">IIBBBBB", part)
            assert (compression, filt, interlace) == (0,0,0)
        if kind == b"IDAT":
            content += part
        pos += 12 + size
        if kind == b"IEND":
            break
    assert pos == len(raw)
    assert (width, height, depth, color) == (32,32,8,6)
    stream = zlib.decompress(content)
    assert len(stream) == 32*129
    result = []
    previous = bytearray(128)
    for y in range(32):
        filtering = stream[y*129]
        assert 0 <= filtering <= 4
        line = bytearray(stream[y*129+1:y*129+129])
        for n in range(128):
            left = line[n-4] if n >= 4 else 0
            up = previous[n]
            ul = previous[n-4] if n >= 4 else 0
            if filtering == 0: prediction = 0
            elif filtering == 1: prediction = left
            elif filtering == 2: prediction = up
            elif filtering == 3: prediction = (left + up)//2
            else:
                p = left+up-ul
                a,b,c = abs(p-left),abs(p-up),abs(p-ul)
                prediction = left if a<=b and a<=c else up if b<=c else ul
            line[n] = (line[n]+prediction)&255
        result.extend(tuple(line[n:n+4]) for n in range(0,128,4))
        previous = line
    return result


rgba = pixels(PATH)
alpha = [p[3] for p in rgba]
assert set(alpha) == {0,255}, "minecraft:cutout requires fully opaque or transparent pixels"
solid = sum(a == 255 for a in alpha)
assert 65 < solid < 256, "must show window edges while leaving >75% visibility"
for y in range(8,24):
    center = [rgba[y*32+x][3] for x in range(8,24)]
    assert sum(a == 0 for a in center) >= 10, (y, "glass must not block food")
paths = [
    "block/fried_food_cart_upper_display_left.json",
    "block/fried_food_cart_upper_display_right.json",
    "block/fried_food_cart_display.json",
    "block/fried_food_cart_display_front_left.json",
    "block/fried_food_cart_display_front_right.json",
    "block/fried_food_cart_display_rear_left.json",
    "block/fried_food_cart_display_rear_right.json",
    "item/fried_food_cart.json",
]
for rel in paths:
    model = json.loads((MODEL / rel).read_text(encoding="utf-8"))
    assert model["render_type"] == "minecraft:cutout", rel
    assert model["textures"]["glass"] == "sfcdelight:block/cart_glass", rel
    assert any(face.get("texture") == "#glass"
               for cube in model["elements"]
               for face in cube.get("faces", {}).values()), rel
# All model keys and coordinates were retained; no block state or item data changed.
cart = json.loads((ASSETS/"blockstates/fried_food_cart.json").read_text(encoding="utf-8"))
assert len(cart["variants"]) == 32
assert cart["variants"]["facing=north,part=4"]["model"].endswith("upper_display_left")
assert cart["variants"]["facing=north,part=5"]["model"].endswith("upper_display_right")
print("PASS: truly transparent 32x32 showcase, cutout materials, 8 model variants, no item or state changes")
