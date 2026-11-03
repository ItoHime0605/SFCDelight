package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Eight independent basket records. Order is bottom (0) to top (count-1). */
public class FriedFoodCartBlockEntity extends BlockEntity {
    public static final int MAX_BASKETS = 8;
    private final NonNullList<ItemStack> baskets = NonNullList.withSize(MAX_BASKETS, ItemStack.EMPTY);
    private int count = 0;
    /** A real, refillable white-pepper shaker stored on the left serving counter. */
    private ItemStack counterPepper = ItemStack.EMPTY;
    /** Four display columns across the 2-block width, three rows along the 2-block depth. */
    /** Data-driven allowlist: other mods may add compatible ingredients via this tag. */
    public static final TagKey<Item> DISPLAY_INGREDIENTS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("sfcdelight", "display_ingredients"));
    public static final int DISPLAY_SLOTS = 12;
    private static final int OLD_DISPLAY_SLOTS = 18;
    private static final int LEGACY_SLOTS = OLD_DISPLAY_SLOTS - DISPLAY_SLOTS;
    public static final int MAX_DISPLAY_PORTIONS = 10;
    private final NonNullList<ItemStack> display = NonNullList.withSize(DISPLAY_SLOTS, ItemStack.EMPTY);
    /** One reusable plastic frame is installed beneath each stocked display slot. */
    private final NonNullList<ItemStack> displayFrames = NonNullList.withSize(DISPLAY_SLOTS, ItemStack.EMPTY);
    // Old 6x3 installations must not silently lose the six slots removed by resizing.
    private final NonNullList<ItemStack> legacyFood = NonNullList.withSize(LEGACY_SLOTS, ItemStack.EMPTY);
    private final NonNullList<ItemStack> legacyFrames = NonNullList.withSize(LEGACY_SLOTS, ItemStack.EMPTY);
    private boolean suppressCartDrop;

    public void suppressCartDrop() { suppressCartDrop = true; }
    public boolean shouldDropCart() { return !suppressCartDrop; }

    public ItemStack reclaimLegacy() {
        if (level == null || level.isClientSide()) return ItemStack.EMPTY;
        for (NonNullList<ItemStack> list : java.util.List.of(legacyFood, legacyFrames)) {
            for (int i = 0; i < list.size(); i++) {
                ItemStack old = list.get(i);
                if (!old.isEmpty()) {
                    list.set(i, ItemStack.EMPTY);
                    setChanged();
                    return old.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getPepperJar() { return counterPepper.copy(); }
    public boolean hasPepperJar() { return !counterPepper.isEmpty(); }

    /** Store exactly one physical shaker and preserve its current charge component. */
    public boolean installPepperJar(ItemStack held) {
        if (level == null || level.isClientSide() || hasPepperJar() || held == null
                || !held.is(SaltedFriedChickenDelight.PEPPER_SHAKER.get())
                || held.getCount() != 1) return false;
        counterPepper = held.copyWithCount(1);
        setChanged();
        return true;
    }

    /** Return the stored jar with its remaining pepper, never a newly generated refill. */
    public ItemStack takePepperJar() {
        if (level == null || level.isClientSide() || !hasPepperJar()) return ItemStack.EMPTY;
        ItemStack out = counterPepper.copy();
        counterPepper = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    public boolean refillPepperJar() {
        if (level == null || level.isClientSide() || !hasPepperJar()) return false;
        int charges = PepperShakerItem.charges(counterPepper);
        if (charges > PepperShakerItem.CAPACITY - 10) return false;
        PepperShakerItem.setCharges(counterPepper, charges + 10);
        setChanged();
        return true;
    }

    /** Click the counter with a packed fried-food bag/box to season it in place. */
    public boolean seasonPackage(ItemStack food, boolean creative) {
        if (level == null || level.isClientSide() || !hasPepperJar()
                || food == null || !WhitePepperItem.isPackage(food)
                || PepperShakerItem.charges(counterPepper) <= 0
                || WhitePepperItem.spiceLevel(food) >= 4) return false;
        int capacity = food.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(
                FriedBatchData.readAll(food, level.registryAccess()), capacity)) return false;
        WhitePepperItem.setSpiceLevel(food, WhitePepperItem.spiceLevel(food) + 1);
        if (!creative) PepperShakerItem.setCharges(counterPepper, PepperShakerItem.charges(counterPepper) - 1);
        setChanged();
        return true;
    }

    public boolean hasFrame(int slot) {
        return slot >= 0 && slot < DISPLAY_SLOTS && !displayFrames.get(slot).isEmpty();
    }

    public boolean installFrame(int slot, ItemStack held) {
        if (level == null || level.isClientSide() || slot < 0 || slot >= DISPLAY_SLOTS
                || held == null || !held.is(SaltedFriedChickenDelight.PLASTIC_FRAME.get())
                || held.isEmpty() || hasFrame(slot)) return false;
        displayFrames.set(slot, held.copyWithCount(1));
        setChanged();
        return true;
    }

    /** Only an empty display frame can be picked up; never detach stocked food. */
    public ItemStack removeFrame(int slot) {
        if (level == null || level.isClientSide() || !hasFrame(slot) || !display.get(slot).isEmpty())
            return ItemStack.EMPTY;
        ItemStack frame = displayFrames.get(slot).copy();
        displayFrames.set(slot, ItemStack.EMPTY);
        setChanged();
        return frame;
    }

    /** Return physical frames as well as their food when the cart is dismantled. */
    public java.util.List<ItemStack> drainFrames() {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        if (level == null || level.isClientSide()) return drops;
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            if (!displayFrames.get(slot).isEmpty()) drops.add(displayFrames.get(slot).copy());
            displayFrames.set(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            if (!legacyFrames.get(i).isEmpty()) drops.add(legacyFrames.get(i).copy());
            legacyFrames.set(i, ItemStack.EMPTY);
        }
        setChanged();
        return drops;
    }

    public ItemStack getDisplayStack(int slot) {
        if (slot < 0 || slot >= DISPLAY_SLOTS) return ItemStack.EMPTY;
        return display.get(slot).copy();
    }

    /** Server-side inventory operation; never merge different items or item components. */
    public int insertDisplay(int slot, ItemStack input, int requested) {
        if (level == null || level.isClientSide() || slot < 0 || slot >= DISPLAY_SLOTS || !hasFrame(slot) || input == null || input.isEmpty() || requested <= 0 || !input.is(DISPLAY_INGREDIENTS)) return 0;
        ItemStack current = display.get(slot);
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, input)) return 0;
        int inserted = Math.min(requested, Math.min(input.getCount(), Math.min(MAX_DISPLAY_PORTIONS, input.getMaxStackSize()) - current.getCount()));
        if (inserted <= 0) return 0;
        if (current.isEmpty()) display.set(slot, input.copyWithCount(inserted));
        else current.grow(inserted);
        setChanged();
        return inserted;
    }

    public ItemStack extractDisplay(int slot, int requested) {
        if (level == null || level.isClientSide() || slot < 0 || slot >= DISPLAY_SLOTS || requested <= 0) return ItemStack.EMPTY;
        ItemStack current = display.get(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        ItemStack taken = current.split(Math.min(requested, current.getCount()));
        if (current.isEmpty()) display.set(slot, ItemStack.EMPTY);
        setChanged();
        return taken;
    }

    /** Remove all display contents on block destruction, preserving item components. */
    public java.util.List<ItemStack> drainDisplay() {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        if (level == null || level.isClientSide()) return drops;
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            ItemStack stack = display.get(slot);
            if (!stack.isEmpty()) drops.add(stack.copy());
            display.set(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            if (!legacyFood.get(i).isEmpty()) drops.add(legacyFood.get(i).copy());
            legacyFood.set(i, ItemStack.EMPTY);
        }
        setChanged();
        return drops;
    }

    public FriedFoodCartBlockEntity(BlockPos pos, BlockState state) {
        super(SaltedFriedChickenDelight.CART_ENTITY.get(), pos, state);
    }

    /** Notify tracking clients whenever the physical display or basket stack changes. */
    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public int basketCount() { return count; }

    public boolean pushBasket(ItemStack candidate) {
        if (level == null || level.isClientSide() || candidate == null || candidate.isEmpty() || candidate.getCount() != 1 || count >= MAX_BASKETS || !candidate.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sfcdelight", "plastic_baskets")))) return false;
        if (!candidate.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .stream().allMatch(ItemStack::isEmpty)) return false;
        baskets.set(count++, candidate.copyWithCount(1));
        setChanged();
        return true;
    }

    public ItemStack getBasket(int index) {
        return index >= 0 && index < count ? baskets.get(index).copy() : ItemStack.EMPTY;
    }

    public ItemStack popBasket() {
        if (level == null || level.isClientSide() || count == 0) return ItemStack.EMPTY;
        ItemStack result = baskets.get(--count);
        baskets.set(count, ItemStack.EMPTY);
        setChanged();
        return result.copy();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BasketCount", count);
        if (!counterPepper.isEmpty()) tag.put("CounterPepper", counterPepper.save(registries));
        CompoundTag basketTag = new CompoundTag();
        ContainerHelper.saveAllItems(basketTag, baskets, registries);
        tag.put("Baskets", basketTag);
        CompoundTag displayTag = new CompoundTag();
        ContainerHelper.saveAllItems(displayTag, display, registries);
        tag.put("Display", displayTag);
        CompoundTag framesTag = new CompoundTag();
        ContainerHelper.saveAllItems(framesTag, displayFrames, registries);
        tag.put("DisplayFrames", framesTag);
        CompoundTag olderFood = new CompoundTag();
        ContainerHelper.saveAllItems(olderFood, legacyFood, registries);
        tag.put("LegacyFood", olderFood);
        CompoundTag olderFrames = new CompoundTag();
        ContainerHelper.saveAllItems(olderFrames, legacyFrames, registries);
        tag.put("LegacyFrames", olderFrames);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        counterPepper = tag.contains("CounterPepper", 10)
                ? ItemStack.parseOptional(registries, tag.getCompound("CounterPepper")) : ItemStack.EMPTY;
        if (!counterPepper.is(SaltedFriedChickenDelight.PEPPER_SHAKER.get())
                || counterPepper.getCount() != 1) counterPepper = ItemStack.EMPTY;
        for (int i = 0; i < MAX_BASKETS; i++) baskets.set(i, ItemStack.EMPTY);
        if (tag.contains("Baskets")) ContainerHelper.loadAllItems(tag.getCompound("Baskets"), baskets, registries);
        else ContainerHelper.loadAllItems(tag, baskets, registries); // migration from 0.1.5-alpha
        for (int i = 0; i < DISPLAY_SLOTS; i++) {
            display.set(i, ItemStack.EMPTY);
            displayFrames.set(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            legacyFood.set(i, ItemStack.EMPTY);
            legacyFrames.set(i, ItemStack.EMPTY);
        }
        // Read old 18-slot storage into an intermediate inventory. Keeping only
        // the first twelve would otherwise silently destroy the last six food stacks.
        NonNullList<ItemStack> savedFood = NonNullList.withSize(OLD_DISPLAY_SLOTS, ItemStack.EMPTY);
        NonNullList<ItemStack> savedFrames = NonNullList.withSize(OLD_DISPLAY_SLOTS, ItemStack.EMPTY);
        if (tag.contains("Display")) ContainerHelper.loadAllItems(tag.getCompound("Display"), savedFood, registries);
        if (tag.contains("DisplayFrames"))
            ContainerHelper.loadAllItems(tag.getCompound("DisplayFrames"), savedFrames, registries);
        else {
            for (int i = 0; i < OLD_DISPLAY_SLOTS; i++) {
                if (!savedFood.get(i).isEmpty())
                    savedFrames.set(i, new ItemStack(SaltedFriedChickenDelight.PLASTIC_FRAME.get()));
            }
        }
        for (int i = 0; i < DISPLAY_SLOTS; i++) {
            display.set(i, savedFood.get(i));
            displayFrames.set(i, savedFrames.get(i));
        }
        for (int i = 0; i < LEGACY_SLOTS; i++) {
            legacyFood.set(i, savedFood.get(DISPLAY_SLOTS + i));
            legacyFrames.set(i, savedFrames.get(DISPLAY_SLOTS + i));
        }
        if (tag.contains("LegacyFood")) ContainerHelper.loadAllItems(tag.getCompound("LegacyFood"), legacyFood, registries);
        if (tag.contains("LegacyFrames")) ContainerHelper.loadAllItems(tag.getCompound("LegacyFrames"), legacyFrames, registries);
        for (int i = 0; i < DISPLAY_SLOTS; i++) {
            ItemStack stack = display.get(i);
            if (stack.getCount() > MAX_DISPLAY_PORTIONS) stack.setCount(MAX_DISPLAY_PORTIONS);
        }
        // Rebuild a contiguous physical stack instead of trusting a possibly stale count.
        // Never silently delete a saved basket merely because BasketCount is wrong.
        NonNullList<ItemStack> restored = NonNullList.withSize(MAX_BASKETS, ItemStack.EMPTY);
        int restoredCount = 0;
        for (int i = 0; i < MAX_BASKETS; i++) {
            ItemStack stack = baskets.get(i);
            if (!stack.isEmpty() && stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sfcdelight", "plastic_baskets")))) {
                // Keep the full saved stack and components: do not silently erase legacy data.
                restored.set(restoredCount++, stack.copy());
            }
        }
        for (int i = 0; i < MAX_BASKETS; i++) baskets.set(i, restored.get(i));
        count = restoredCount;
    }
}
