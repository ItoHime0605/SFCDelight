package tw.saltedfriedchicken;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

/** A single-batch basket. Cooking is server authoritative; no oil is spent on an aborted batch. */
public class FryingBasketBlockEntity extends BlockEntity {
    private final List<ItemStack> ingredients = new ArrayList<>();
    private int progress;
    private int liftTicks;
    private boolean batchReady; // Only a complete plastic-basket transfer can start the fryer.
    public static final int MAX_PIECES = 10;
    private static final int LIFT_TICKS = 20;
    private static final int COOK_TICKS = 300; // 15 seconds at 20 ticks per second
    public FryingBasketBlockEntity(BlockPos pos, BlockState state) { super(SaltedFriedChickenDelight.FRYING_BASKET_ENTITY.get(), pos, state); }
    public ItemStack ingredient() { return ingredients.isEmpty() ? ItemStack.EMPTY : ingredients.get(0).copy(); }
    public List<ItemStack> ingredients() { return ingredients.stream().map(ItemStack::copy).toList(); }
    public boolean isEmpty() { return ingredients.isEmpty(); }
    private boolean isRaw() {
        return !ingredients.isEmpty() && ingredients.stream().allMatch(s -> FryingRecipes.output(s) != null);
    }
    /** True only for an unfinished batch, not an already cooked product. */
    public boolean hasPendingFry() { return batchReady && isRaw(); }
    /** Accepts up to ten portions. Refuses changes after cooking begins. */
    public boolean insert(ItemStack held) {
        if (level == null || level.isClientSide || held == null || held.isEmpty() || FryingRecipes.output(held) == null
                || getBlockState().getValue(FryingBasketBlock.SUBMERGED)
                || ingredients.stream().mapToInt(ItemStack::getCount).sum() >= MAX_PIECES
                || progress > 0 || liftTicks > 0 || !isReadyForMore()) return false;
        if (FriedBatchData.countDistinctTypes(ingredients) >= 5
                && ingredients.stream().noneMatch(s -> ItemStack.isSameItemSameComponents(s, held))) return false;
        ingredients.add(held.copyWithCount(1));
        progress = 0;
        batchReady = false; // On a table this basket is for preparation, not auto-frying.
        sync();
        return true;
    }
    /** One atomic transfer of a complete plastic-basket selection (up to ten portions, five kinds). */
    public boolean transferFromPlastic(ItemStack plastic) {
        if (level == null || level.isClientSide || plastic == null || plastic.getCount() != 1
                || !(plastic.getItem() instanceof PlasticFoodBasketItem) || !ingredients.isEmpty()
                || progress > 0 || liftTicks > 0) return false;
        // The raised pose on an empty fryer is expected. Lifting means a completed batch.
        if (getBlockState().getValue(FryingBasketBlock.LIFTING)) return false;
        ItemContainerContents container = plastic.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        List<ItemStack> batch = container.stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList();
        if (batch.isEmpty() || batch.size() > 5
                || FriedBatchData.countPortions(batch) > MAX_PIECES
                || FriedBatchData.countDistinctTypes(batch) > 5
                || batch.stream().anyMatch(stack -> FryingRecipes.output(stack) == null)) return false;
        // All validations precede changes to either container. Never partially unload a basket.
        ingredients.addAll(batch);
        progress = 0;
        liftTicks = 0;
        boolean onFryer = getBlockState().getValue(FryingBasketBlock.SUBMERGED)
                && level.getBlockEntity(worldPosition.below()) instanceof FryingStationBlockEntity;
        batchReady = onFryer;
        plastic.set(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        if (onFryer) {
            FryingStationBlockEntity fryer = (FryingStationBlockEntity) level.getBlockEntity(worldPosition.below());
            setPose(false, fryer.charges() <= 0);
        }
        sync();
        return true;
    }
    private boolean isReadyForMore() {
        if (level == null) return ingredients.isEmpty() || isRaw();
        BlockState state = getBlockState();
        return !state.getValue(FryingBasketBlock.LIFTING) && !state.getValue(FryingBasketBlock.RAISED)
                && (ingredients.isEmpty() || isRaw());
    }
    /** Reinsert an already cooked hot basket: raise it again, without frying or oil usage. */
    public boolean restoreCooked(List<ItemStack> cooked) {
        if (!ingredients.isEmpty() || cooked == null || cooked.isEmpty() || level == null || level.isClientSide
                || !(level.getBlockEntity(worldPosition.below()) instanceof FryingStationBlockEntity)
                || !getBlockState().getValue(FryingBasketBlock.SUBMERGED)) return false;
        if (!FryingRecipes.isValidCookedBatch(cooked, MAX_PIECES)) return false;
        ingredients.addAll(cooked.stream().map(ItemStack::copy).toList());
        progress = 0;
        batchReady = false;
        liftTicks = 0;
        setPose(true, false);
        sync();
        return true;
    }
    public List<ItemStack> extractAll() {
        // Inventory is server-authoritative. Client rendering or prediction must
        // never empty a basket before the server confirms extraction.
        if (level == null || level.isClientSide) return List.of();
        List<ItemStack> out = ingredients();
        ingredients.clear();
        progress = 0;
        liftTicks = 0;
        batchReady = false;
        // No blockstate mutation here: extraction may run during block removal.
        sync();
        return out;
    }
    /** Extract one ingredient stack without discarding the remaining batch. */
    public ItemStack extract() {
        if (level == null || level.isClientSide || ingredients.isEmpty()) return ItemStack.EMPTY;
        ItemStack out = ingredients.remove(0).copy();
        progress = 0;
        liftTicks = 0;
        batchReady = false;
        sync();
        return out;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, FryingBasketBlockEntity basket) {
        // The basket can be placed on any block, but only fries when a station is directly below it.
        FryingStationBlockEntity fryer = level.getBlockEntity(pos.below()) instanceof FryingStationBlockEntity f ? f : null;
        if (fryer == null || !state.getValue(FryingBasketBlock.SUBMERGED)) {
            if (basket.progress != 0 || basket.batchReady || basket.liftTicks != 0) {
                basket.progress = 0;
                basket.batchReady = false;
                basket.liftTicks = 0;
                basket.sync(); // Clear stale client progress when the fryer disappears.
            }
            if (fryer != null) fryer.setFrying(false);
            // A basket can stand on an ordinary block. If its fryer is removed
            // during lifting, clear the stale pose so it cannot remain suspended.
            if (state.getValue(FryingBasketBlock.LIFTING) || state.getValue(FryingBasketBlock.RAISED)) {
                basket.setPose(false, false);
                basket.sync();
            }
            return;
        }
        // Recover cooked baskets from older saves or interrupted block-state updates.
        // A cooked batch must never re-enter the raw frying timer or spend another charge.
        // The regular one-second lift still applies, including after a chunk reload.
        if (!state.getValue(FryingBasketBlock.LIFTING)
                && !state.getValue(FryingBasketBlock.RAISED)
                && FryingRecipes.isValidCookedBatch(basket.ingredients, MAX_PIECES)) {
            basket.progress = 0;
            basket.batchReady = false;
            basket.liftTicks = 0;
            fryer.setFrying(false);
            basket.setPose(true, false);
            basket.sync();
            return;
        }
        // A completed basket rises for one second and stays hooked above the oil.
        if (state.getValue(FryingBasketBlock.LIFTING)) {
            fryer.setFrying(false);
            if (++basket.liftTicks >= LIFT_TICKS) {
                basket.liftTicks = 0;
                basket.setPose(false, true);
            } else basket.setChanged(); // Persist lift progress across chunk unloads.
            return;
        }
        if (state.getValue(FryingBasketBlock.RAISED)) {
            fryer.setFrying(false);
            if (basket.batchReady && basket.isRaw() && fryer.charges() > 0)
                basket.setPose(false, false); // Lower the full batch into the oil.
            return;
        }
        boolean active = basket.batchReady && basket.isRaw() && fryer.charges() > 0;
        if (!active) {
            if (basket.progress != 0) { basket.progress = 0; basket.setChanged(); }
            fryer.setFrying(false);
            // An empty or unpowered basket always hangs above the oil.
            basket.setPose(false, true);
            return;
        }
        fryer.setFrying(true);
        // Mark the block entity dirty while cooking. Without this, a chunk save
        // between the start and end of the 15-second cycle can lose progress.
        basket.progress++;
        basket.setChanged();
        if (basket.progress >= COOK_TICKS) {
            List<ItemStack> cooked = FryingRecipes.cook(basket.ingredients);
            // Validate the entire batch before charging oil or replacing any ingredients.
            if (FryingRecipes.isValidCookedBatch(cooked, MAX_PIECES)
                    && FriedBatchData.countPortions(cooked) == FriedBatchData.countPortions(basket.ingredients)) {
                if (fryer.consumeFryingCharge()) {
                    basket.ingredients.clear();
                    basket.ingredients.addAll(cooked);
                    basket.batchReady = false;
                    basket.setPose(true, false);
                    basket.sync();
                }
            }
            basket.progress = 0;
            basket.setChanged();
            fryer.setFrying(false);
        }
    }
    /** Model pose is authoritative and only changes on transitions, not every tick. */
    private void setPose(boolean lifting, boolean raised) {
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        if (state.getValue(FryingBasketBlock.LIFTING) != lifting || state.getValue(FryingBasketBlock.RAISED) != raised) {
            level.setBlock(worldPosition, state.setValue(FryingBasketBlock.LIFTING, lifting)
                .setValue(FryingBasketBlock.RAISED, raised), 3);
        }
        setChanged();
    }
    /** Notify clients when basket contents change; progress remains server-owned. */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack item : ingredients) list.add(item.save(registries));
        tag.put("Ingredients", list);
        tag.putInt("Progress", progress);
        tag.putInt("LiftTicks", liftTicks);
        tag.putBoolean("BatchReady", batchReady);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ingredients.clear();
        // Treat an invalid saved batch atomically. Truncating an overfilled list or
        // skipping a malformed entry would silently destroy the player's food.
        List<ItemStack> restored = new ArrayList<>();
        boolean valid = true;
        int portions = 0;
        if (tag.contains("Ingredients") && tag.contains("Ingredient")) valid = false;
        if (tag.contains("Ingredients")) {
            // A present but incorrectly typed Ingredients tag is corruption, not an empty basket.
            if (!tag.contains("Ingredients", 9)) valid = false;
            ListTag list = tag.getList("Ingredients", 10);
            if (list.isEmpty() || list.size() > MAX_PIECES) valid = false;
            // Reject non-compound elements instead of letting getList silently return empty.
            for (int i = 0; i < list.size() && valid; i++) {
                if (!(list.get(i) instanceof CompoundTag)) { valid = false; break; }
                ItemStack stack = ItemStack.parseOptional(registries, list.getCompound(i));
                if (stack.isEmpty() || stack.getCount() <= 0 || stack.getCount() > stack.getMaxStackSize()
                        || (FryingRecipes.output(stack) == null
                        && !FryingRecipes.isCookedOutput(stack))) {
                    valid = false;
                } else {
                    portions += stack.getCount();
                    if (portions > MAX_PIECES) valid = false;
                    else restored.add(stack);
                }
            }
        } else if (tag.contains("Ingredient", 10)) {
            ItemStack old = ItemStack.parseOptional(registries, tag.getCompound("Ingredient"));
            if (old.isEmpty() || old.getCount() <= 0 || old.getCount() > old.getMaxStackSize()
                    || old.getCount() > MAX_PIECES
                    || (FryingRecipes.output(old) == null && !FryingRecipes.isCookedOutput(old))) valid = false;
            else restored.add(old);
        }
        if (valid) ingredients.addAll(restored);
        // Corrupted or mixed raw/cooked saves must never start a fresh fry cycle.
        boolean rawOnly = !ingredients.isEmpty() && ingredients.stream().allMatch(i -> FryingRecipes.output(i) != null);
        boolean cookedOnly = FryingRecipes.isValidCookedBatch(ingredients, MAX_PIECES);
        if (!rawOnly && !cookedOnly) ingredients.clear();
        progress = rawOnly ? Math.max(0, Math.min(COOK_TICKS - 1, tag.getInt("Progress"))) : 0;
        liftTicks = cookedOnly ? Math.max(0, Math.min(LIFT_TICKS - 1, tag.getInt("LiftTicks"))) : 0;
        batchReady = rawOnly && tag.getBoolean("BatchReady");
        if (!batchReady) progress = 0; // Legacy manually loaded fryer saves may not auto-start.
    }
}
