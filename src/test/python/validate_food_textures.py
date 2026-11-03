"""Validate the six original 32x32 RGBA fried-food sprites without external dependencies."""
import json
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "src/main/resources/assets/sfcdelight"
NAMES = (
    "fried_chicken_dice",
    "fried_cod_dice",
    "fried_dried_tofu",
    "fried_hundred_layer_tofu",
    "fried_rice_blood_cake",
    "fried_duck_blood",
)
SIGNATURE = b"\x89PNG\r\n\x1a\n"

for name in NAMES:
    png = (RES / "textures/item" / f"{name}.png").read_bytes()
    assert png[:8] == SIGNATURE, name
    chunks = {}
    offset = len(SIGNATURE)
    while offset < len(png):
        size = struct.unpack_from(">I", png, offset)[0]
        label = png[offset + 4:offset + 8]
        payload = png[offset + 8:offset + 8 + size]
        expected = struct.unpack_from(">I", png, offset + 8 + size)[0]
        assert zlib.crc32(label + payload) == expected, (name, label)
        chunks[label] = chunks.get(label, b"") + payload
        offset += 12 + size
        if label == b"IEND":
            break
    assert offset == len(png), name
    width, height, depth, kind, compression, filtering, interlace = struct.unpack(
        ">IIBBBBB", chunks[b"IHDR"])
    assert (width, height, depth, kind, compression, filtering, interlace) == (32,32,8,6,0,0,0), name
    raw = zlib.decompress(chunks[b"IDAT"])
    assert len(raw) == 32 * (1 + 32 * 4), name
    assert all(raw[row * 129] == 0 for row in range(32)), name
    alpha = [raw[row * 129 + 1 + pixel * 4 + 3] for row in range(32) for pixel in range(32)]
    assert 250 <= sum(a > 0 for a in alpha) <= 650, name
    assert sum(a == 0 for a in alpha) > 300, name
    model = json.loads((RES / "models/item" / f"{name}.json").read_text(encoding="utf-8"))
    assert model["textures"]["layer0"] == f"sfcdelight:item/{name}", name
    item_definition = RES / "items" / f"{name}.json"
    assert item_definition.exists(), item_definition
    print(f"PASS {name}: independent transparent RGBA sprite, validated PNG CRC, model reference")
