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
    /** Six columns by three rows, each slot limited to ten portions. */
    /** Data-driven allowlist: other mods may add compatible ingredients via this tag. */
    public static final TagKey<Item> DISPLAY_INGREDIENTS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("sfcdelight", "display_ingredients"));
    public static final int DISPLAY_SLOTS = 18;
    public static final int MAX_DISPLAY_PORTIONS = 10;
    private final NonNullList<ItemStack> display = NonNullList.withSize(DISPLAY_SLOTS, ItemStack.EMPTY);
    /** One reusable plastic frame is installed beneath each stocked display slot. */
    private final NonNullList<ItemStack> displayFrames = NonNullList.withSize(DISPLAY_SLOTS, ItemStack.EMPTY);

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

    /** Return physical frames as well as their food when the cart is dismantled. */
    public java.util.List<ItemStack> drainFrames() {
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        if (level == null || level.isClientSide()) return drops;
        for (int slot = 0; slot < DISPLAY_SLOTS; slot++) {
            if (!displayFrames.get(slot).isEmpty()) drops.add(displayFrames.get(slot).copy());
            displayFrames.set(slot, ItemStack.EMPTY);
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
        CompoundTag basketTag = new CompoundTag();
        ContainerHelper.saveAllItems(basketTag, baskets, registries);
        tag.put("Baskets", basketTag);
        CompoundTag displayTag = new CompoundTag();
        ContainerHelper.saveAllItems(displayTag, display, registries);
        tag.put("Display", displayTag);
        CompoundTag framesTag = new CompoundTag();
        ContainerHelper.saveAllItems(framesTag, displayFrames, registries);
        tag.put("DisplayFrames", framesTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < MAX_BASKETS; i++) baskets.set(i, ItemStack.EMPTY);
        if (tag.contains("Baskets")) ContainerHelper.loadAllItems(tag.getCompound("Baskets"), baskets, registries);
        else ContainerHelper.loadAllItems(tag, baskets, registries); // migration from 0.1.5-alpha
        for (int i = 0; i < DISPLAY_SLOTS; i++) {
            display.set(i, ItemStack.EMPTY);
            displayFrames.set(i, ItemStack.EMPTY);
        }
        if (tag.contains("Display")) ContainerHelper.loadAllItems(tag.getCompound("Display"), display, registries);
        if (tag.contains("DisplayFrames")) {
            ContainerHelper.loadAllItems(tag.getCompound("DisplayFrames"), displayFrames, registries);
        } else {
            // Legacy stocked carts had implicit built-in frames. Preserve their stored food
            // instead of making the existing display inaccessible after upgrading.
            for (int i = 0; i < DISPLAY_SLOTS; i++) {
                if (!display.get(i).isEmpty())
                    displayFrames.set(i, new ItemStack(SaltedFriedChickenDelight.PLASTIC_FRAME.get()));
            }
        }
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
