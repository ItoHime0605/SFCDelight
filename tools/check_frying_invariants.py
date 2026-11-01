from pathlib import Path
root = Path(__file__).resolve().parents[1] / "src/main/java/tw/saltedfriedchicken"
basket = (root / "FryingBasketBlockEntity.java").read_text()
station = (root / "FryingStationBlockEntity.java").read_text()
block = (root / "FryingStationBlock.java").read_text()
assert "public static final int MAX_PIECES = 10" in basket
assert "setPose(true, false);" in basket
assert "if (state.getValue(FryingBasketBlock.LIFTING))" in basket
assert "if (state.getValue(FryingBasketBlock.RAISED))" in basket
assert "consumeFryingCharge()" in basket
assert "ActiveFrying" in station and "getUpdateTag" in station and "onDataPacket" in station
assert "!level.getBlockState(basketPos).canBeReplaced()" in block
assert "held.shrink(1)" in block
print("靜態流程檢查通過（不等於編譯或實機測試）")
