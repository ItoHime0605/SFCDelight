package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Oil is stored as 20 frying charges; each oil item contributes two charges. */
public class FryingStationBlockEntity extends BlockEntity {
    private int charges;
    private boolean frying;
    public FryingStationBlockEntity(BlockPos pos, BlockState state) { super(SaltedFriedChickenDelight.FRYER_ENTITY.get(), pos, state); }
    public int charges() { return charges; }
    public boolean isFrying() { return frying; }
    public void setFrying(boolean active) {
        if (frying == active) return;
        frying = active;
        update();
    }
    public boolean addOil() {
        if (charges > 18) return false;
        charges += 2;
        update();
        return true;
    }
    public boolean consumeFryingCharge() {
        if (charges <= 0) return false;
        charges--;
        update();
        return true;
    }
    private void update() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            int stage = FryingStationBlock.stageForCharges(charges);
            if (state.hasProperty(FryingStationBlock.INDICATOR) && state.getValue(FryingStationBlock.INDICATOR) != stage)
                level.setBlock(worldPosition, state.setValue(FryingStationBlock.INDICATOR, stage), 3);
            else level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("FryingCharges", charges);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charges = Math.max(0, Math.min(20, tag.getInt("FryingCharges")));
        frying = level != null && level.isClientSide && tag.getBoolean("ActiveFrying");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveWithoutMetadata(registries);
        tag.putBoolean("ActiveFrying", frying);
        return tag;
    }
    @Override public void onDataPacket(net.minecraft.network.Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        CompoundTag tag = packet.getTag();
        if (tag != null) frying = tag.getBoolean("ActiveFrying");
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
