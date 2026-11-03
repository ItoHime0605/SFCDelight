package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** First playable interaction slice: one physical stack of up to eight baskets. */
public class FriedFoodCartBlock extends BaseEntityBlock {
    public static final MapCodec<FriedFoodCartBlock> CODEC = simpleCodec(FriedFoodCartBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    public FriedFoodCartBlock(Properties properties) { super(properties); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FriedFoodCartBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** Pick the actual 6x3 plastic display frames on the TOP of the cart.
     * Their centers agree with the static model and the dynamic ingredient renderer.
     * Never map three rows to vertical front-face positions by accident.
     */
    private static int displaySlot(BlockPos pos, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) return -1;
        double x = (hit.getLocation().x - pos.getX()) * 16.0;
        double z = (hit.getLocation().z - pos.getZ()) * 16.0;
        int col = (int) Math.floor((x - 1.30) / 2.25);
        int row = (int) Math.floor((z - 1.40) / 4.35);
        if (col < 0 || col >= 6 || row < 0 || row >= 3) return -1;
        double dx = x - 1.30 - col * 2.25;
        double dz = z - 1.40 - row * 4.35;
        if (dx < 0 || dx > 2.02 || dz < 0 || dz > 3.95) return -1;
        return row * 6 + col;
    }

    /** Up to ten total portions, allowing ten different ingredients in the same plastic basket. */
    private static boolean addToBasket(ItemStack basket, ItemStack food) {
        ItemContainerContents contents = basket.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> slots = NonNullList.withSize(FryingBasketBlockEntity.MAX_PIECES, ItemStack.EMPTY);
        int index = 0;
        int portions = 0;
        for (ItemStack entry : contents.stream().toList()) {
            // Reject malformed or oversized containers instead of silently discarding food.
            if (entry == null || (!entry.isEmpty() && (entry.getCount() <= 0
                    || entry.getCount() > entry.getMaxStackSize()
                    || !entry.is(FriedFoodCartBlockEntity.DISPLAY_INGREDIENTS)))) return false;
            if (!entry.isEmpty()) portions += entry.getCount();
            if (index >= slots.size()) {
                if (!entry.isEmpty()) return false;
                continue;
            }
            slots.set(index++, entry.copy());
        }
        // Ten total portions per basket, not ten portions of every variety.
        if (portions >= FryingBasketBlockEntity.MAX_PIECES) return false;
        for (int i = 0; i < slots.size(); i++) {
            ItemStack current = slots.get(i);
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, food)
                    && current.getCount() < current.getMaxStackSize()) {
                current.grow(1);
                basket.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
                return true;
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isEmpty()) {
                slots.set(i, food.copyWithCount(1));
                basket.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
                return true;
            }
        }
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FriedFoodCartBlockEntity cart)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (hit.getDirection() == Direction.UP && held.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sfcdelight", "plastic_baskets")))) {
            if (!held.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().allMatch(ItemStack::isEmpty))
                return ItemInteractionResult.FAIL;
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            if (!cart.pushBasket(held)) return ItemInteractionResult.FAIL;
            // Creative mode must still move exactly one basket into the rack,
            // otherwise a single click duplicates a data-bearing container.
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        int slot = displaySlot(pos, hit);
        if (slot < 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(SaltedFriedChickenDelight.IRON_TONGS.get())) {
            ItemStack basket = player.getOffhandItem();
            if (basket.getCount() != 1 || !basket.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sfcdelight", "plastic_baskets")))) return ItemInteractionResult.FAIL;
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            ItemStack food = cart.getDisplayStack(slot);
            if (food.isEmpty()) return ItemInteractionResult.FAIL;
            // Stage the result on a copy first: a failed extraction must never duplicate food.
            ItemStack updatedBasket = basket.copy();
            if (!addToBasket(updatedBasket, food.copyWithCount(1))) return ItemInteractionResult.FAIL;
            ItemStack extracted = cart.extractDisplay(slot, 1);
            if (extracted.isEmpty() || !ItemStack.isSameItemSameComponents(extracted, food)) {
                // Never commit the basket update if the display changed.
                if (!extracted.isEmpty()) cart.insertDisplay(slot, extracted, extracted.getCount());
                return ItemInteractionResult.FAIL;
            }
            basket.set(DataComponents.CONTAINER, updatedBasket.getOrDefault(
                    DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            player.getInventory().setChanged();
            return ItemInteractionResult.SUCCESS;
        }
        // Staff may stock a rack with an ordinary food stack. Never insert tools or baskets.
        if (held.isEmpty() || held.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("sfcdelight", "plastic_baskets"))))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        int inserted = cart.insertDisplay(slot, held, 1);
        if (inserted == 0) return ItemInteractionResult.FAIL;
        if (!player.getAbilities().instabuild) held.shrink(inserted);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FriedFoodCartBlockEntity cart)) return InteractionResult.PASS;
        ItemStack basket = cart.popBasket();
        if (basket.isEmpty()) return InteractionResult.PASS;
        player.setItemInHand(InteractionHand.OFF_HAND, basket);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean moved) {
        if (!state.is(nextState.getBlock()) && !level.isClientSide() && level.getBlockEntity(pos) instanceof FriedFoodCartBlockEntity cart) {
            for (ItemStack stack : cart.drainDisplay()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack);
            }
            ItemStack basket;
            while (!(basket = cart.popBasket()).isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, basket);
            }
        }
        super.onRemove(state, level, pos, nextState, moved);
    }
}
