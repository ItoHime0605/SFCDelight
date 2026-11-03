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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * One placed cart occupies three blocks across, one deep, two high.
 * Left: one-block seasoning counter; right: two-block 4x3 tilted food display.
 * Only the lower left master (part 0) owns the inventory block entity.
 * Part values 6 and 7 remain registered for alpha-world compatibility.
 */
public class FriedFoodCartBlock extends BaseEntityBlock {
    public static final MapCodec<FriedFoodCartBlock> CODEC = simpleCodec(FriedFoodCartBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 7);
    private static final ThreadLocal<Boolean> DISMANTLING = ThreadLocal.withInitial(() -> false);
    private static final VoxelShape LOWER_SHAPE = box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape UPPER_SHAPE = box(0, 0, 0, 16, 16, 16);

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    public FriedFoodCartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** Six new cart segments: bottom [0,1,2], top [3,4,5] from left to right. */
    private static BlockPos root(BlockPos segment, BlockState state) {
        int part = state.getValue(PART);
        Direction right = state.getValue(FACING).getClockWise();
        return segment.relative(right.getOpposite(), part % 3).below(part / 3);
    }

    private static BlockPos segmentPos(BlockPos anchor, BlockState anchorState, int part) {
        return anchor.relative(anchorState.getValue(FACING).getClockWise(), part % 3)
                .above(part / 3);
    }

    /** Previous 2x2x2 alpha layout, retained for dismantling installed old carts. */
    private static BlockPos oldSquareRoot(BlockPos segment, BlockState state) {
        int part = state.getValue(PART);
        return segment.relative(state.getValue(FACING).getClockWise().getOpposite(), part % 2)
                .relative(state.getValue(FACING), (part % 4) / 2).below(part / 4);
    }

    private static BlockPos oldSquareSegmentPos(BlockPos anchor, BlockState state, int part) {
        return anchor.relative(state.getValue(FACING).getClockWise(), part % 2)
                .relative(state.getValue(FACING).getOpposite(), (part % 4) / 2).above(part / 4);
    }

    /** Previous 1x4x2 alpha layout, retained for dismantling installed old carts. */
    private static BlockPos oldLongRoot(BlockPos segment, BlockState state) {
        int part = state.getValue(PART);
        return segment.relative(state.getValue(FACING), part % 4).below(part / 4);
    }

    private static BlockPos oldLongSegmentPos(BlockPos anchor, BlockState state, int part) {
        return anchor.relative(state.getValue(FACING).getOpposite(), part % 4).above(part / 4);
    }

    private static boolean isMaster(Level level, BlockPos pos, Direction facing) {
        BlockState check = level.getBlockState(pos);
        return check.getBlock() instanceof FriedFoodCartBlock
                && check.getValue(PART) == 0 && check.getValue(FACING) == facing;
    }

    private static BlockPos locateRoot(Level level, BlockPos segment, BlockState state) {
        BlockPos current = root(segment, state);
        if (isMaster(level, current, state.getValue(FACING))) return current;
        BlockPos oldSquare = oldSquareRoot(segment, state);
        if (isMaster(level, oldSquare, state.getValue(FACING))) return oldSquare;
        BlockPos oldLong = oldLongRoot(segment, state);
        if (isMaster(level, oldLong, state.getValue(FACING))) return oldLong;
        return current;
    }

    private static boolean matchingPart(Level level, BlockPos pos, Direction facing, int part) {
        BlockState check = level.getBlockState(pos);
        return check.getBlock() instanceof FriedFoodCartBlock
                && check.getValue(PART) == part && check.getValue(FACING) == facing;
    }

    /** All six new segments or eight old ones must be removed as one logical cart. */
    private static int layout(Level level, BlockPos anchor, BlockState anchorState) {
        Direction facing = anchorState.getValue(FACING);
        if (matchingPart(level, oldLongSegmentPos(anchor, anchorState, 7), facing, 7)) return 2;
        if (matchingPart(level, oldSquareSegmentPos(anchor, anchorState, 7), facing, 7)) return 1;
        return 0;
    }

    private static FriedFoodCartBlockEntity cartAt(Level level, BlockPos segment, BlockState segmentState) {
        BlockPos anchor = locateRoot(level, segment, segmentState);
        if (!isMaster(level, anchor, segmentState.getValue(FACING))) return null;
        return level.getBlockEntity(anchor) instanceof FriedFoodCartBlockEntity cart ? cart : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState anchorState = defaultBlockState().setValue(FACING, facing);
        BlockPos origin = context.getClickedPos();
        for (int part = 1; part < 6; part++) {
            BlockPos at = segmentPos(origin, anchorState, part);
            if (!context.getLevel().getWorldBorder().isWithinBounds(at)
                    || context.getLevel().isOutsideBuildHeight(at)
                    || !context.getLevel().getBlockState(at).canBeReplaced()) return null;
        }
        return anchorState;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || state.getValue(PART) != 0) return;
        for (int part = 1; part < 6; part++) {
            level.setBlock(segmentPos(pos, state, part), state.setValue(PART, part), 3);
        }
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new FriedFoodCartBlockEntity(pos, state) : null;
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                             BlockPos pos, CollisionContext context) {
        return state.getValue(PART) >= 3 ? UPPER_SHAPE : LOWER_SHAPE;
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                                      BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    /** Display is on the FRONT glass of the two rightmost upper cart blocks.
     * Four columns across the two-block width; three rows top to bottom.
     * The left upper segment is the condiment counter, never a food display.
     */
    private static int displaySlot(BlockPos pos, BlockState state, BlockHitResult hit) {
        int part = state.getValue(PART);
        if ((part != 4 && part != 5) || hit.getDirection() != state.getValue(FACING))
            return -1;
        Direction right = state.getValue(FACING).getClockWise();
        double dx = hit.getLocation().x - (pos.getX() + 0.5);
        double dz = hit.getLocation().z - (pos.getZ() + 0.5);
        double width = (part - 4) + 0.5 + dx * right.getStepX() + dz * right.getStepZ();
        double y = hit.getLocation().y - pos.getY();
        if (width < -0.001 || width > 2.001 || y < -0.001 || y > 1.001) return -1;
        width = Math.max(0, Math.min(1.999999, width));
        y = Math.max(0, Math.min(0.999999, y));
        int column = Math.min(3, (int) Math.floor(width * 2.0));
        int row = Math.min(2, (int) Math.floor((1.0 - y) * 3.0));
        return row * 4 + column;
    }

    /** Left/right halves of the service counter select white pepper/chili respectively. */
    private static boolean rightCondiment(BlockPos pos, BlockState state, BlockHitResult hit) {
        Direction right=state.getValue(FACING).getClockWise();
        return (hit.getLocation().x-pos.getX()-0.5)*right.getStepX()
                +(hit.getLocation().z-pos.getZ()-0.5)*right.getStepZ() >= 0;
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

        // Left half: white pepper. Right half: chili powder. Both are real items.
        if (state.getValue(PART) == 3 && hit.getDirection() == state.getValue(FACING)) {
            boolean chili=rightCondiment(pos,state,hit);
            if (held.is(chili ? SaltedFriedChickenDelight.CHILI_SHAKER.get()
                              : SaltedFriedChickenDelight.PEPPER_SHAKER.get())) {
                if (level.isClientSide) return ItemInteractionResult.SUCCESS;
                boolean installed=chili ? cart.installChiliJar(held) : cart.installPepperJar(held);
                if (!installed) return ItemInteractionResult.FAIL;
                held.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            if (held.is(chili ? SaltedFriedChickenDelight.CHILI_POWDER.get()
                              : SaltedFriedChickenDelight.WHITE_PEPPER_POWDER.get())) {
                if (level.isClientSide) return ItemInteractionResult.SUCCESS;
                boolean refilled=chili ? cart.refillChiliJar() : cart.refillPepperJar();
                if (!refilled) return ItemInteractionResult.FAIL;
                if (!player.getAbilities().instabuild) held.shrink(1);
                return ItemInteractionResult.SUCCESS;
            }
            if (WhitePepperItem.isPackage(held)) {
                if (level.isClientSide) return ItemInteractionResult.SUCCESS;
                boolean seasoned=chili ? cart.seasonChiliPackage(held,player.getAbilities().instabuild)
                                      : cart.seasonPackage(held,player.getAbilities().instabuild);
                return seasoned ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
            }
        }

        // The basket rack is accessible at the left lower front of the cart.
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
            // A stocked frame is a physical item: consume the source even in creative,
            // matching the cart's basket rack and avoiding duplicate return drops.
            held.shrink(1);
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
        // Food is now stored in the cart; never retain the original stack as well.
        held.shrink(inserted);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        FriedFoodCartBlockEntity cart = cartAt(level, pos, state);
        if (cart == null) return InteractionResult.PASS;
        if (state.getValue(PART) == 3 && hit.getDirection() == state.getValue(FACING)) {
            boolean chili=rightCondiment(pos,state,hit);
            ItemStack other=player.getOffhandItem();
            if (WhitePepperItem.isPackage(other)) {
                if (level.isClientSide) return InteractionResult.SUCCESS;
                boolean seasoned=chili ? cart.seasonChiliPackage(other,player.getAbilities().instabuild)
                                      : cart.seasonPackage(other,player.getAbilities().instabuild);
                return seasoned ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            if (!other.isEmpty()) return InteractionResult.PASS;
            if (chili ? !cart.hasChiliJar() : !cart.hasPepperJar()) return InteractionResult.PASS;
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) {
                ItemStack jar=chili ? cart.takeChiliJar() : cart.takePepperJar();
                if (jar.isEmpty()) return InteractionResult.PASS;
                if (!player.getInventory().add(jar)) player.drop(jar,false);
            } else {
                ItemStack jar=chili ? cart.getChiliJar() : cart.getPepperJar();
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        (chili ? "辣椒粉" : "白胡椒粉") + "剩餘次數："
                        + PepperShakerItem.charges(jar) + " / 60"),true);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
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
            if (legacy.isEmpty()) {
                // Legacy slot recovery takes precedence; once empty, open the
                // read-only dashboard without disturbing baskets or seasonings.
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    EquipmentStatusMenu.open(serverPlayer, cart, cart.getBlockPos());
                return InteractionResult.SUCCESS;
            }
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

    /** Break any section to dismantle the entire structure once, preserving all inventories. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean moved) {
        if (!state.is(nextState.getBlock()) && !level.isClientSide && !DISMANTLING.get()) {
            DISMANTLING.set(true);
            try {
                BlockPos anchor = locateRoot(level, pos, state);
                BlockState masterState = level.getBlockState(anchor);
                int structure = layout(level, anchor, masterState.getBlock() == this
                        ? masterState : state.setValue(PART, 0));
                // For the master currently being removed, its block entity still exists here.
                FriedFoodCartBlockEntity cart = level.getBlockEntity(anchor) instanceof FriedFoodCartBlockEntity c ? c : null;
                if (cart != null) {
                    boolean drops = !moved && level.getGameRules().getBoolean(
                            net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
                    // All real display food, frames, stored baskets and spices
                    // are returned with the cart. Never invent extra decorations.
                    for (ItemStack item : cart.drainDisplay()) if (drops) drop(level, anchor, item);
                    for (ItemStack item : cart.drainFrames()) if (drops) drop(level, anchor, item);
                    ItemStack pepper = cart.takePepperJar();
                    ItemStack chili = cart.takeChiliJar();
                    if (drops) {
                        drop(level, anchor, pepper);
                        drop(level, anchor, chili);
                    }
                    ItemStack basket;
                    while (!(basket = cart.popBasket()).isEmpty()) if (drops) drop(level, anchor, basket);
                    if (drops && cart.shouldDropCart())
                        drop(level, anchor, new ItemStack(SaltedFriedChickenDelight.CART_ITEM.get()));
                }
                BlockState template = state.setValue(PART, 0);
                for (int part = 0; part < (structure == 0 ? 6 : 8); part++) {
                    BlockPos other = structure == 2 ? oldLongSegmentPos(anchor, template, part)
                            : structure == 1 ? oldSquareSegmentPos(anchor, template, part)
                            : segmentPos(anchor, template, part);
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
