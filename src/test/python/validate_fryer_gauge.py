"""Five distinct oil stages plus zero-fuel red warning; 20 precise frying charges."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[3]
src=(R/"src/main/java/tw/saltedfriedchicken/FryingStationBlock.java").read_text(encoding="utf-8")
entity=(R/"src/main/java/tw/saltedfriedchicken/FryingStationBlockEntity.java").read_text(encoding="utf-8")
basket=(R/"src/main/java/tw/saltedfriedchicken/FryingBasketBlockEntity.java").read_text(encoding="utf-8")
assert "charges <= 4" in src and "charges <= 8" in src
assert "charges <= 12" in src and "charges <= 16" in src
assert "return 5;" in src
assert "fryer.charges()" in src and "20 次油炸" in src
assert "charges += 2" in entity and "charges--" in entity and "charges > 18" in entity
assert "if (frying && random.nextInt(60) == 0)" in src
assert "level.playLocalSound(" in src and "SoundEvents.CAMPFIRE_CRACKLE" in src
assert "if (fryer.consumeFryingCharge())" in basket
assert "level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN" in basket
states=json.loads((R/"src/main/resources/assets/sfcdelight/blockstates/frying_station.json").read_text())["variants"]
assert set(states)=={f"indicator={i}" for i in range(7)}
assert states["indicator=6"]["model"]=="sfcdelight:block/frying_station_5"
signatures=set()
for n in range(6):
    p=R/f"src/main/resources/assets/sfcdelight/models/block/frying_station_{n}.json"
    model=json.loads(p.read_text())
    # Gauge lenses remain the final 6 (empty) or 5 (charged) cubes;
    # the preceding elements are purely decorative stainless bodywork.
    lamps=[e["faces"]["north"]["texture"] for e in model["elements"][-(6 if n==0 else 5):]]
    assert len(lamps)==(6 if n==0 else 5)
    signatures.add(tuple(lamps))
assert len(signatures)==6, signatures
print("PASS: five oil stages, 20 charges, client-only active-oil sizzle, once-per-batch basket lift")
