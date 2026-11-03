"""Static regression: visual-only de-oiler rotation, real portions and one completion cue."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
SRC = ROOT / "src/main/java/tw/saltedfriedchicken"
render = (SRC / "client/DeOilerContentsRenderer.java").read_text(encoding="utf-8")
entity = (SRC / "DeOilerBlockEntity.java").read_text(encoding="utf-8")
reg = (SRC / "client/ClientBlockEntityRenderers.java").read_text(encoding="utf-8")
assert "DeOilerContentsRenderer::new" in reg and "DEOILER_ENTITY.get()" in reg
assert "FriedBatchData.readAll(stored, machine.getLevel().registryAccess())" in render
assert "machine.getLevel().getGameTime() + partialTick" in render
assert "boolean spinning = !machine.isComplete();" in render
assert "pieceIndex < 10" in render and "if (pieceIndex >= 10) break;" in render
assert "pose.pushPose()" in render and "pose.popPose()" in render
assert "CYCLE_TICKS = 160" in entity
assert "if (entity.elapsed == CYCLE_TICKS)" in entity
assert "level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP" in entity
assert "if (level.isClientSide || entity.food.isEmpty() || entity.elapsed >= CYCLE_TICKS) return;" in entity
assert "machine.contentsForDisplay()" in render
print("PASS: physical food in 8s de-oiler, render-time-only spin, single completion sound")
