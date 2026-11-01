package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Eight-second powered-by-itself de-oiling cycle. No redstone requirement. */
public class DeOilerBlockEntity extends BlockEntity {
    public static final int CYCLE_TICKS = 160;
    private ItemStack food = ItemStack.EMPTY;
    private int elapsed;
    public DeOilerBlockEntity(BlockPos pos, BlockState state) {
        super(SaltedFriedChickenDelight.DEOILER_ENTITY.get(), pos, state);
    }
    public boolean isEmpty() { return food.isEmpty(); }
    public ItemStack contentsForDisplay() { return food.copy(); }
    public boolean isComplete() { return !food.isEmpty() && elapsed >= CYCLE_TICKS; }
    public int elapsed() { return elapsed; }
    public boolean insert(ItemStack cooked) {
        if (level == null || level.isClientSide || !food.isEmpty() || cooked == null || cooked.isEmpty()
                || !cooked.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get())
                || cooked.getCount() != 1
                || !FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(cooked, level.registryAccess()), 10)) return false;
        food = cooked.copy();
        elapsed = 0;
        sync();
        return true;
    }
    public ItemStack extractCompleted() {
        if (level == null || level.isClientSide || !isComplete()) return ItemStack.EMPTY;
        ItemStack result = food.copy();
        food = ItemStack.EMPTY;
        elapsed = 0;
        sync();
        return result;
    }
    public static void tick(Level level, BlockPos pos, BlockState state, DeOilerBlockEntity entity) {
        if (level.isClientSide || entity.food.isEmpty() || entity.elapsed >= CYCLE_TICKS) return;
        entity.elapsed++;
        // Update the visible progress once a second, not every server tick.
        if (entity.elapsed % 20 == 0 || entity.elapsed == CYCLE_TICKS) entity.sync();
        else entity.setChanged();
    }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!food.isEmpty()) tag.put("Food", food.save(registries));
        tag.putInt("Elapsed", elapsed);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ItemStack saved = tag.contains("Food", 10)
                ? ItemStack.parseOptional(registries, tag.getCompound("Food")) : ItemStack.EMPTY;
        // Never resume an invalid or oversized batch from a damaged/legacy save.
        // Validation here also protects the client-side display after a chunk reload.
        if (saved.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get())
                && saved.getCount() == 1
                && FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(saved, registries), 10)) {
            food = saved;
            elapsed = Math.max(0, Math.min(CYCLE_TICKS, tag.getInt("Elapsed")));
        } else {
            food = ItemStack.EMPTY;
            elapsed = 0;
        }
    }
    public ItemStack dropContents() {
        if (level == null || level.isClientSide) return ItemStack.EMPTY;
        ItemStack result = food.copy();
        food = ItemStack.EMPTY;
        elapsed = 0;
        setChanged();
        return result;
    }
}
