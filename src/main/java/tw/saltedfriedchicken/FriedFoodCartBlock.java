package tw.saltedfriedchicken;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A single cart item places a 1-wide, 4-deep, 2-tall multiblock.
 * Four top segments hold three tilted plastic frames apiece (4x3 = 12).
 * Only the bottom-front master (part 0) owns the inventory block entity.
 */
public class FriedFoodCartBlock extends BaseEntityBlock {
    public static final MapCodec<FriedFoodCartBlock> CODEC = simpleCodec(FriedFoodCartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    private static final ThreadLocal<Boolean> DISMANTLING = ThreadLocal.withInitial(() -> false);
    private static final VoxelShape LOWER_SHAPE = box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape UPPER_SHAPE = box(0, 0, 0, 16, 12, 16);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public FriedFoodCartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    private static BlockPos root(BlockPos segment, BlockState state) {
        int index = state.getValue(PART);
        Direction forward = state.getValue(FACING);
        return segment.relative(forward, index % 4).below(index / 4);
    }

    private static BlockPos segmentPos(BlockPos anchor, BlockState anchorState, int part) {
        return anchor.relative(anchorState.getValue(FACING).getOpposite(), part % 4).above(part / 4);
    }

    private static FriedFoodCartBlockEntity cartAt(Level level, BlockPos segment, BlockState segmentState) {
        BlockPos anchor = root(segment, segmentState);
        BlockState rootState = level.getBlockState(anchor);
        if (!(rootState.getBlock() instanceof FriedFoodCartBlock) || rootState.getValue(PART) != 0
                || rootState.getValue(FACING) != segmentState.getValue(FACING)) return null;
        return level.getBlockEntity(anchor) instanceof FriedFoodCartBlockEntity cart ? cart : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState anchorState = defaultBlockState().setValue(FACING, facing);
        BlockPos origin = context.getClickedPos();
        for (int part = 1; part < 8; part++) {
            BlockPos at = segmentPos(origin, anchorState, part);
            if (!context.getLevel().getWorldBorder().isWithinBounds(at)
                    || !context.getLevel().getBlockState(at).canBeReplaced()) return null;
        }
        return anchorState;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(PART) != 0) return;
        for (int part = 1; part < 8; part++) {
            level.setBlock(segmentPos(pos, state, part), state.setValue(PART, part), 3);
        }
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new FriedFoodCartBlockEntity(pos, state) : null;
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos, CollisionContext context) {
        return state.getValue(PART) >= 4 ? UPPER_SHAPE : LOWER_SHAPE;
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                      BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** The three columns run across the short edge; four rows run front-to-back. */
    private static int displaySlot(BlockPos pos, BlockState state, BlockHitResult hit) {
        if (state.getValue(PART) < 4 || hit.getDirection() != Direction.UP) return -1;
        double dx = hit.getLocation().x - (pos.getX() + 0.5);
        double dz = hit.getLocation().z - (pos.getZ() + 0.5);
        Direction right = state.getValue(FACING).getClockWise();
        double width = 0.5 + dx * right.getStepX() + dz * right.getStepZ();
        if (width < 0.05 || width >= 0.95) return -1;
        int column = Math.min(2, (int) Math.floor(width * 3));
        return (state.getValue(PART) - 4) * 3 + column;
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
        FriedFoodCartBlockEntity cart = cartAt(level, pos, state);
        if (cart == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Basket rack is reachable from the front of the bottom-front segment.
        if (state.getValue(PART) == 0 && hit.getDirection() == state.getValue(FACING)
                && held.getItem() instanceof PlasticFoodBasketItem) {
            if (!held.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                    .stream().allMatch(ItemStack::isEmpty)) return ItemInteractionResult.FAIL;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!cart.pushBasket(held)) return ItemInteractionResult.FAIL;
            held.shrink(1); // Prevent creative-mode physical-basket duplication.
            return ItemInteractionResult.SUCCESS;
        }

        int slot = displaySlot(pos, state, hit);
        if (slot < 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(SaltedFriedChickenDelight.PLASTIC_FRAME.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!cart.installFrame(slot, held)) return ItemInteractionResult.FAIL;
            if (!player.getAbilities().instabuild) held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.IRON_TONGS.get())) {
            ItemStack plastic = player.getOffhandItem();
            if (plastic.getCount() != 1 || !(plastic.getItem() instanceof PlasticFoodBasketItem))
                return ItemInteractionResult.FAIL;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            ItemStack food = cart.getDisplayStack(slot);
            if (food.isEmpty()) return ItemInteractionResult.FAIL;
            ItemStack updated = plastic.copy();
            if (!addToBasket(updated, food.copyWithCount(1))) return ItemInteractionResult.FAIL;
            ItemStack removed = cart.extractDisplay(slot, 1);
            if (removed.isEmpty() || !ItemStack.isSameItemSameComponents(removed, food)) {
                if (!removed.isEmpty()) cart.insertDisplay(slot, removed, removed.getCount());
                return ItemInteractionResult.FAIL;
            }
            plastic.set(DataComponents.CONTAINER,
                    updated.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            player.getInventory().setChanged();
            return ItemInteractionResult.SUCCESS;
        }
        if (held.isEmpty() || held.getItem() instanceof PlasticFoodBasketItem)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!cart.hasFrame(slot)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.sfcdelight.display_frame_required"), true);
            return ItemInteractionResult.FAIL;
        }
        int inserted = cart.insertDisplay(slot, held, 1);
        if (inserted == 0) return ItemInteractionResult.FAIL;
        if (!player.getAbilities().instabuild) held.shrink(inserted);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty())
            return InteractionResult.PASS;
        FriedFoodCartBlockEntity cart = cartAt(level, pos, state);
        if (cart == null) return InteractionResult.PASS;
        int slot = displaySlot(pos, state, hit);
        if (slot >= 0) {
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            if (!cart.getDisplayStack(slot).isEmpty()) {
                if (!level.isClientSide) player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable("message.sfcdelight.display_frame_has_food"), true);
                return InteractionResult.FAIL;
            }
            if (!cart.hasFrame(slot)) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            ItemStack frame = cart.removeFrame(slot);
            if (frame.isEmpty()) return InteractionResult.PASS;
            if (!player.getInventory().add(frame)) player.drop(frame, false);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(PART) != 0 || hit.getDirection() != state.getValue(FACING))
            return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            // Recover previously stocked items from six obsolete 6x3 slots.
            ItemStack legacy = cart.reclaimLegacy();
            if (legacy.isEmpty()) return InteractionResult.PASS;
            if (!player.getInventory().add(legacy)) player.drop(legacy, false);
            return InteractionResult.SUCCESS;
        }
        ItemStack basket = cart.popBasket();
        if (basket.isEmpty()) return InteractionResult.PASS;
        player.setItemInHand(InteractionHand.OFF_HAND, basket);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild) {
            FriedFoodCartBlockEntity cart = cartAt(level, pos, state);
            if (cart != null) cart.suppressCartDrop();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Break any of the eight physical sections to dismantle the cart once, without cloning contents. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean moved) {
        if (!state.is(nextState.getBlock()) && !level.isClientSide && !DISMANTLING.get()) {
            DISMANTLING.set(true);
            try {
                BlockPos anchor = root(pos, state);
                BlockState anchorState = level.getBlockState(anchor);
                // For the master currently being removed, its block entity still exists here.
                FriedFoodCartBlockEntity cart = level.getBlockEntity(anchor) instanceof FriedFoodCartBlockEntity c ? c : null;
                if (cart != null) {
                    for (ItemStack item : cart.drainDisplay()) drop(level, anchor, item);
                    for (ItemStack item : cart.drainFrames()) drop(level, anchor, item);
                    ItemStack basket;
                    while (!(basket = cart.popBasket()).isEmpty()) drop(level, anchor, basket);
                    if (!moved && cart.shouldDropCart()
                            && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS))
                        drop(level, anchor, new ItemStack(SaltedFriedChickenDelight.CART_ITEM.get()));
                }
                BlockState template = state.setValue(PART, 0);
                for (int part = 0; part < 8; part++) {
                    BlockPos other = segmentPos(anchor, template, part);
                    if (other.equals(pos)) continue;
                    BlockState otherState = level.getBlockState(other);
                    if (otherState.getBlock() == this
                            && otherState.getValue(PART) == part
                            && otherState.getValue(FACING) == state.getValue(FACING))
                        level.removeBlock(other, false);
                }
            } finally {
                DISMANTLING.set(false);
            }
        }
        super.onRemove(state, level, pos, nextState, moved);
    }

    private static void drop(Level level, BlockPos at, ItemStack stack) {
        if (!stack.isEmpty()) net.minecraft.world.Containers.dropItemStack(
                level, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack);
    }
}
