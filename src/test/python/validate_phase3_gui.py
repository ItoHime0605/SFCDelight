"""Third-batch GUI regression: full texture integrity and read-only server-owned views."""
from pathlib import Path
import json, struct, zlib
root=Path(__file__).resolve().parents[3]
assets=root/"src/main/resources/assets/sfcdelight"
src=root/"src/main/java/tw/saltedfriedchicken"
def read(name):
    return (src/name).read_text(encoding="utf-8")
def png(name):
    data=(assets/"textures/gui"/name).read_bytes()
    assert data[:8]==b"\x89PNG\r\n\x1a\n", name
    pos=8;chunks={}
    while pos<len(data):
        count=struct.unpack_from(">I",data,pos)[0]
        typ=data[pos+4:pos+8]
        chunk=data[pos+8:pos+8+count]
        assert zlib.crc32(typ+chunk)==struct.unpack_from(">I",data,pos+8+count)[0],name
        chunks[typ]=chunks.get(typ,b"")+chunk
        pos+=12+count
        if typ==b"IEND":break
    assert pos==len(data),name
    assert struct.unpack_from(">II",chunks[b"IHDR"])==(256,256),name
    assert zlib.decompress(chunks[b"IDAT"]),name
png("equipment_status.png")
png("preparation_sink.png")

registry=read("SaltedFriedChickenDelight.java")
menu=read("EquipmentStatusMenu.java")
screen=read("client/EquipmentStatusScreen.java")
sink=read("client/PreparationSinkScreen.java")
client=read("client/ClientPreparationSinkScreens.java")
assert 'MENU_TYPES.register("equipment_status"' in registry
assert 'EquipmentStatusMenu::new' in registry
assert "EQUIPMENT_STATUS_MENU.get(), EquipmentStatusScreen::new" in client
assert "PREPARATION_SINK_MENU.get(), PreparationSinkScreen::new" in client
assert "public static final int DATA_SLOTS = 17" in menu
assert "addDataSlots(data)" in menu
assert "new SimpleContainerData(DATA_SLOTS)" in menu
assert "ContainerLevelAccess.create" in menu
assert "EquipmentStatusMenu.open" not in menu
assert "public static void open(ServerPlayer player" in menu
assert "quickMoveStack" in menu and "return ItemStack.EMPTY" in menu
assert "addSlot(" not in menu and "broadcastChanges" not in screen
assert "new SimpleContainerData" not in screen
assert "EquipmentStatusMenu.CART" not in menu or "FriedFoodCartBlockEntity" in menu
for string in ("cart.getDisplayStack", "cart.hasFrame", "cart.basketCount",
               "cart.getPepperJar", "cart.getChiliJar", "fryer.charges()",
               "fryer.isFrying()", "deoiler.elapsed()", "deoiler.isComplete()",
               "prep.marinationSeconds()", "prep.washes()"):
    assert string in menu,string
for name in ("cart","fryer","deoiler","preparation"):
    assert f"gui.sfcdelight.title_{name}" in screen
assert 'textures/gui/equipment_status.png' in screen
assert 'textures/gui/preparation_sink.png' in sink
block_names=["FriedFoodCartBlock.java","FryingStationBlock.java","DeOilerBlock.java","PreparationCounterBlock.java"]
for name in block_names:
    block=read(name)
    assert "EquipmentStatusMenu.open(" in block,name
    assert "player.isShiftKeyDown()" in block,name
    assert "level.isClientSide" in block,name
# Previously agreed interactions remain available.
assert "ItemStack legacy = cart.reclaimLegacy();" in read(block_names[0])
assert "cart.popBasket();" in read(block_names[0])
assert "cart.takeChiliJar()" in read(block_names[0])
assert "fryer.charges()" in read(block_names[1])
assert "machine.extractCompleted()" in read(block_names[2])
assert "bench.takeEmptyBowl()" in read(block_names[3])
assert "bench.mixBatter()" in read(block_names[3])
assert "new PreparationSinkMenu" in read(block_names[3])
lang=json.loads((assets/"lang/zh_tw.json").read_text(encoding="utf-8"))
for key in ("menu.sfcdelight.equipment_status","gui.sfcdelight.status_readonly",
            "gui.sfcdelight.status_oil","gui.sfcdelight.status_deoiling",
            "gui.sfcdelight.status_marination","gui.sfcdelight.status_showcase"):
    assert key in lang,key
print("PASS: two pixel-art textures, four live read-only dashboards, sink redesign, existing interactions preserved")
