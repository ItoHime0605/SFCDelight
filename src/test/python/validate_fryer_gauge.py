"""Five distinct oil stages plus zero-fuel red warning; 20 precise frying charges."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[3]
src=(R/"src/main/java/tw/saltedfriedchicken/FryingStationBlock.java").read_text(encoding="utf-8")
entity=(R/"src/main/java/tw/saltedfriedchicken/FryingStationBlockEntity.java").read_text(encoding="utf-8")
assert "charges <= 4" in src and "charges <= 8" in src
assert "charges <= 12" in src and "charges <= 16" in src
assert "return 5;" in src
assert "fryer.charges()" in src and "20 次油炸" in src
assert "charges += 2" in entity and "charges--" in entity and "charges > 18" in entity
states=json.loads((R/"src/main/resources/assets/sfcdelight/blockstates/frying_station.json").read_text())["variants"]
assert set(states)=={f"indicator={i}" for i in range(7)}
assert states["indicator=6"]["model"]=="sfcdelight:block/frying_station_5"
signatures=set()
for n in range(6):
    p=R/f"src/main/resources/assets/sfcdelight/models/block/frying_station_{n}.json"
    model=json.loads(p.read_text())
    lamps=[e["faces"]["north"]["texture"] for e in model["elements"][7:]]
    assert len(lamps)==(6 if n==0 else 5)
    signatures.add(tuple(lamps))
assert len(signatures)==6, signatures
print("PASS: five oil stages, distinct empty indicator, 20 precise charges, old stage6 maps safely to stage5")
