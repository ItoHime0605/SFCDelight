package tw.saltedfriedchicken;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A placed, single-use takeaway container. Contents survive world saves and chunk reloads. */
public class FriedPackagingBlockEntity extends BlockEntity {
    private ItemStack contents = ItemStack.EMPTY;
    public FriedPackagingBlockEntity(BlockPos pos, BlockState state) { super(SaltedFriedChickenDelight.PACKAGING_ENTITY.get(), pos, state); }
    public boolean isEmpty() { return contents.isEmpty(); }
    public ItemStack getContents() { return contents.copy(); }
    /** Transfers as many actual portions as will fit, leaving the remainder in the scoop. */
    public int putPortions(List<ItemStack> incoming, int capacity) {
        if (level == null || level.isClientSide || incoming == null || incoming.isEmpty()
                || (capacity != 5 && capacity != 10)
                || !FryingRecipes.isValidCookedBatch(incoming, 10)) return 0;
        // A saved or externally edited package may only contain our loaded scoop.
        // Never merge legitimate food into an arbitrary item disguised as contents.
        if (!contents.isEmpty() && !contents.is(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get())) return 0;
        List<ItemStack> existing = contents.isEmpty() ? List.of() : FriedBatchData.readAll(contents, level.registryAccess());
        // A damaged or externally edited package must not be used to exceed its capacity.
        if (!contents.isEmpty() && !FryingRecipes.isValidCookedBatch(existing, capacity)) return 0;
        int space = Math.max(0, capacity - FriedBatchData.countPortions(existing));
        if (space == 0) return 0;
        java.util.ArrayList<ItemStack> combined = new java.util.ArrayList<>();
        for (ItemStack food : existing) combined.add(food.copy());
        int transferred = 0;
        for (ItemStack food : incoming) {
            if (space == 0) break;
            int amount = Math.min(space, food.getCount());
            if (amount <= 0) continue;
            combined.add(food.copyWithCount(amount));
            space -= amount;
            transferred += amount;
        }
        // Validate the complete resulting package before replacing the saved item.
        // This protects against invalid existing data and ensures no partial writes.
        if (transferred > 0 && FryingRecipes.isValidCookedBatch(combined, capacity)
                && FriedBatchData.countPortions(combined)
                    == FriedBatchData.countPortions(existing) + transferred) {
            contents = FriedBatchData.wrapMany(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get(), combined, level.registryAccess());
            notifyChange();
            return transferred;
        }
        // Never report a transfer that was rejected: callers consume exactly
        // the number returned from this method.
        return 0;
    }
    public ItemStack take() {
        // The server alone owns inventory mutation; prevent accidental client-side removal.
        if (level == null || level.isClientSide) return ItemStack.EMPTY;
        ItemStack result = contents.copy();
        contents = ItemStack.EMPTY;
        notifyChange();
        return result;
    }
    private void notifyChange() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!contents.isEmpty()) tag.put("Contents", contents.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // Do not treat a malformed non-compound payload as an empty valid package.
        // Preserve the block entity as empty rather than attempting to parse an
        // unrelated NBT type (which can be introduced by external editors).
        contents = tag.contains("Contents", 10)
                ? ItemStack.parseOptional(registries, tag.getCompound("Contents"))
                : ItemStack.EMPTY;
    }
}
