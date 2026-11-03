package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.List;

/** Left: timed marination and three-click bowl battering. Right: 20 hand washes.
 * Entire two-block bench has ONE master block entity; no per-player server ticks.
 */
public class PreparationCounterBlockEntity extends BlockEntity {
    public static final int MARINATE_TICKS = 20 * 180;
    public static final int MAX_WASHES = 20;
    public static final int WASHES_PER_BUCKET = 5;
    private ItemStack marination = ItemStack.EMPTY;
    private int marinateTicks;
    private ItemStack bowl = ItemStack.EMPTY;
    private ItemStack ingredient = ItemStack.EMPTY;
    private int flour;
    private int batterClicks;
    private int waterUses;
    /** Transient creative-break flag, never serialized to the world save. */
    private boolean suppressCounterDrop;

    public void suppressCounterDrop() { suppressCounterDrop = true; }
    public boolean shouldDropCounter() { return !suppressCounterDrop; }

    public PreparationCounterBlockEntity(BlockPos pos, BlockState state) {
        super(SaltedFriedChickenDelight.PREPARATION_COUNTER_ENTITY.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PreparationCounterBlockEntity bench) {
        if (level.isClientSide || bench.marination.isEmpty() || bench.marinateTicks >= MARINATE_TICKS) return;
        bench.marinateTicks++;
        if (bench.marinateTicks % 20 == 0 || bench.marinateTicks == MARINATE_TICKS) bench.sync();
        else bench.setChanged();
    }

    // Client renderers receive defensive copies and never mutate bench inventory.
    public ItemStack displayedMarination() { return marination.copy(); }
    public ItemStack displayedBowl() { return bowl.copy(); }
    public ItemStack displayedIngredient() { return ingredient.copy(); }
    public boolean displayedFlour() { return flour > 0; }

    public int washes() { return waterUses; }
    public int marinationSeconds() { return marinateTicks / 20; }
    public boolean marinating() { return !marination.isEmpty(); }
    public boolean marinationReady() { return marinating() && marinateTicks >= MARINATE_TICKS; }
    public boolean hasBowl() { return !bowl.isEmpty(); }
    public boolean hasIngredient() { return !ingredient.isEmpty(); }
    public boolean hasFlour() { return flour > 0; }
    public int batterClicks() { return batterClicks; }

    public boolean addWater() {
        if (level == null || level.isClientSide || waterUses > MAX_WASHES - WASHES_PER_BUCKET) return false;
        waterUses += WASHES_PER_BUCKET;
        sync();
        return true;
    }
    public boolean washHands() {
        if (level == null || level.isClientSide || waterUses <= 0) return false;
        waterUses--;
        sync();
        return true;
    }

    public boolean installMarination(ItemStack box) {
        if (level == null || level.isClientSide || box == null || box.getCount() != 1
                || marinating() || hasBowl()
                || (!box.is(SaltedFriedChickenDelight.MARINATING_CHICKEN_BOX.get())
                    && !box.is(SaltedFriedChickenDelight.MARINATING_COD_BOX.get()))) return false;
        marination = box.copyWithCount(1);
        marinateTicks = 0;
        sync();
        return true;
    }
    public ItemStack completeMarination() {
        if (level == null || level.isClientSide || !marinationReady()) return ItemStack.EMPTY;
        ItemStack output = marination.is(SaltedFriedChickenDelight.MARINATING_CHICKEN_BOX.get())
                ? new ItemStack(SaltedFriedChickenDelight.MARINATED_CHICKEN_DICE.get())
                : new ItemStack(SaltedFriedChickenDelight.MARINATED_COD_DICE.get());
        marination = ItemStack.EMPTY;
        marinateTicks = 0;
        sync();
        return output;
    }

    public boolean installBowl(ItemStack held) {
        if (level == null || level.isClientSide || marinating() || hasBowl()
                || held == null || !held.is(SaltedFriedChickenDelight.STEEL_BOWL.get())) return false;
        bowl = held.copyWithCount(1);
        sync();
        return true;
    }
    public ItemStack takeEmptyBowl() {
        if (level == null || level.isClientSide || !hasBowl()
                || hasIngredient() || hasFlour()) return ItemStack.EMPTY;
        ItemStack out = bowl.copy();
        bowl = ItemStack.EMPTY;
        batterClicks = 0;
        sync();
        return out;
    }
    public boolean putIngredient(ItemStack held) {
        if (level == null || level.isClientSide || !hasBowl() || hasIngredient() || hasFlour()
                || held == null || held.isEmpty()
                || !(held.is(SaltedFriedChickenDelight.MARINATED_CHICKEN_DICE.get())
                     || held.is(SaltedFriedChickenDelight.MARINATED_COD_DICE.get()))) return false;
        ingredient = held.copyWithCount(1);
        batterClicks = 0;
        sync();
        return true;
    }
    public boolean putFlour(ItemStack held) {
        if (level == null || level.isClientSide || !hasBowl() || !hasIngredient() || hasFlour()
                || held == null || !held.is(SaltedFriedChickenDelight.WHEAT_FLOUR.get())) return false;
        flour = 1;
        batterClicks = 0;
        sync();
        return true;
    }

    /** Three intentional empty-hand clicks coat one marinated portion. */
    public ItemStack mixBatter() {
        if (level == null || level.isClientSide || !hasBowl() || !hasIngredient() || !hasFlour())
            return ItemStack.EMPTY;
        batterClicks++;
        if (batterClicks < 3) {
            sync();
            return ItemStack.EMPTY;
        }
        ItemStack output = ingredient.is(SaltedFriedChickenDelight.MARINATED_CHICKEN_DICE.get())
                ? new ItemStack(SaltedFriedChickenDelight.BATTERED_CHICKEN_DICE.get())
                : new ItemStack(SaltedFriedChickenDelight.BATTERED_COD_DICE.get());
        ingredient = ItemStack.EMPTY;
        flour = 0;
        batterClicks = 0;
        sync();
        return output;
    }

    /** All physical ingredients/equipment survive breaking the bench. */
    public List<ItemStack> drain() {
        if (level == null || level.isClientSide) return List.of();
        List<ItemStack> drops = new ArrayList<>();
        if (!marination.isEmpty()) drops.add(marination.copy());
        if (!bowl.isEmpty()) drops.add(bowl.copy());
        if (!ingredient.isEmpty()) drops.add(ingredient.copy());
        if (flour > 0) drops.add(new ItemStack(SaltedFriedChickenDelight.WHEAT_FLOUR.get(), flour));
        marination = ItemStack.EMPTY;
        bowl = ItemStack.EMPTY;
        ingredient = ItemStack.EMPTY;
        marinateTicks = 0;
        flour = 0;
        batterClicks = 0;
        sync();
        return drops;
    }

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
        if (!marination.isEmpty()) tag.put("Marination", marination.save(registries));
        if (!bowl.isEmpty()) tag.put("Bowl", bowl.save(registries));
        if (!ingredient.isEmpty()) tag.put("Ingredient", ingredient.save(registries));
        tag.putInt("MarinateTicks", marinateTicks);
        tag.putInt("Flour", flour);
        tag.putInt("BatterClicks", batterClicks);
        tag.putInt("WaterUses", waterUses);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        marination = tag.contains("Marination",10)
                ? ItemStack.parseOptional(registries, tag.getCompound("Marination")) : ItemStack.EMPTY;
        if (!marination.is(SaltedFriedChickenDelight.MARINATING_CHICKEN_BOX.get())
                && !marination.is(SaltedFriedChickenDelight.MARINATING_COD_BOX.get()))
            marination = ItemStack.EMPTY;
        if (!marination.isEmpty()) marination.setCount(1);
        bowl = tag.contains("Bowl",10)
                ? ItemStack.parseOptional(registries, tag.getCompound("Bowl")) : ItemStack.EMPTY;
        if (!bowl.is(SaltedFriedChickenDelight.STEEL_BOWL.get())) bowl = ItemStack.EMPTY;
        if (!bowl.isEmpty()) bowl.setCount(1);
        ingredient = tag.contains("Ingredient",10)
                ? ItemStack.parseOptional(registries, tag.getCompound("Ingredient")) : ItemStack.EMPTY;
        if (!(ingredient.is(SaltedFriedChickenDelight.MARINATED_CHICKEN_DICE.get())
                || ingredient.is(SaltedFriedChickenDelight.MARINATED_COD_DICE.get())))
            ingredient = ItemStack.EMPTY;
        if (!ingredient.isEmpty()) ingredient.setCount(1);
        marinateTicks = marination.isEmpty() ? 0
                : Math.max(0, Math.min(MARINATE_TICKS, tag.getInt("MarinateTicks")));
        flour = bowl.isEmpty() || ingredient.isEmpty() ? 0 : Math.max(0, Math.min(1, tag.getInt("Flour")));
        batterClicks = flour == 0 ? 0 : Math.max(0, Math.min(2, tag.getInt("BatterClicks")));
        waterUses = Math.max(0, Math.min(MAX_WASHES, tag.getInt("WaterUses")));
    }
}
