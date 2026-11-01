#!/usr/bin/env python3
"""Offline integrity checks; not a substitute for a NeoForge compile or gameplay test."""
import json
from pathlib import Path
root = Path(__file__).resolve().parents[1]
resources = root / "src/main/resources"
files = sorted(resources.rglob("*.json"))
for path in files:
    json.loads(path.read_text(encoding="utf-8"))
assert len(files) >= 80, "Expected prior resource set is incomplete"
for color in ("white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black").split():
    assert (resources / f"assets/sfcdelight/models/item/plastic_basket_{color}.json").exists(), color
    assert (resources / f"assets/sfcdelight/textures/item/plastic_basket_{color}.png").exists(), color
print(f"OK: {len(files)} JSON assets parsed, 16 dyed basket models and textures present")
