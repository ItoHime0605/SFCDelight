package tw.saltedfriedchicken;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

/** Two blocks: left prep platform and right sink, sharing one server-owned inventory. */
public final class PreparationCounterBlock extends BaseEntityBlock {
    public static final MapCodec<PreparationCounterBlock> CODEC = simpleCodec(PreparationCounterBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
    private static final ThreadLocal<Boolean> DISMANTLING = ThreadLocal.withInitial(() -> false);

    public PreparationCounterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) {
        builder.add(FACING, PART);
    }
    private static BlockPos masterPos(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? pos : pos.relative(state.getValue(FACING).getCounterClockWise());
    }
    private static PreparationCounterBlockEntity getBench(Level level, BlockPos pos, BlockState state) {
        BlockPos master = masterPos(pos, state);
        BlockState root = level.getBlockState(master);
        if (!(root.getBlock() instanceof PreparationCounterBlock) || root.getValue(PART) != 0
                || root.getValue(FACING) != state.getValue(FACING)) return null;
        return level.getBlockEntity(master) instanceof PreparationCounterBlockEntity e ? e : null;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos right = context.getClickedPos().relative(facing.getClockWise());
        if (!context.getLevel().getWorldBorder().isWithinBounds(right)
                || context.getLevel().isOutsideBuildHeight(right)
                || !context.getLevel().getBlockState(right).canBeReplaced()) return null;
        return defaultBlockState().setValue(FACING, facing);
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(PART) == 0)
            level.setBlock(pos.relative(state.getValue(FACING).getClockWise()), state.setValue(PART,1),3);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new PreparationCounterBlockEntity(pos,state) : null;
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(PART) != 0) return null;
        return createTickerHelper(type, SaltedFriedChickenDelight.PREPARATION_COUNTER_ENTITY.get(), PreparationCounterBlockEntity::tick);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    private static void grant(Player player, ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        PreparationCounterBlockEntity bench = getBench(level,pos,state);
        if (bench == null) return ItemInteractionResult.FAIL;
        if (state.getValue(PART) == 1) {
            if (!held.is(Items.WATER_BUCKET)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!bench.addWater()) {
                player.displayClientMessage(Component.literal("洗手台水量已滿（20 / 20 次）"), true);
                return ItemInteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            level.playSound(null,pos,SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,0.55f,1.0f);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.MARINATING_CHICKEN_BOX.get())
                || held.is(SaltedFriedChickenDelight.MARINATING_COD_BOX.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!bench.installMarination(held)) return ItemInteractionResult.FAIL;
            level.playSound(null,pos,SoundEvents.BARREL_CLOSE,SoundSource.BLOCKS,0.55f,1.15f);
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.STEEL_BOWL.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!bench.installBowl(held)) return ItemInteractionResult.FAIL;
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.MARINATED_CHICKEN_DICE.get())
                || held.is(SaltedFriedChickenDelight.MARINATED_COD_DICE.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!bench.putIngredient(held)) return ItemInteractionResult.FAIL;
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.WHEAT_FLOUR.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!bench.putFlour(held)) return ItemInteractionResult.FAIL;
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        PreparationCounterBlockEntity bench = getBench(level,pos,state);
        if (bench == null) return InteractionResult.PASS;
        if (state.getValue(PART) == 1) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            // Agreed controls: crouch + empty-hand right click opens water GUI;
            // normal empty-hand right click washes only when hands are slippery.
            if (player.isShiftKeyDown()) {
                if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
                    serverPlayer.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, viewer) -> new PreparationSinkMenu(id, inventory, bench, pos),
                            Component.translatable("menu.sfcdelight.preparation_sink")));
                return InteractionResult.SUCCESS;
            }
            if (!player.hasEffect(SaltedFriedChickenDelight.SLIPPERY_HANDS)) {
                player.displayClientMessage(Component.translatable("message.sfcdelight.hands_clean"), true);
                return InteractionResult.SUCCESS;
            }
            if (bench.washHands()) {
                player.removeEffect(SaltedFriedChickenDelight.SLIPPERY_HANDS);
                level.playSound(null,pos,SoundEvents.BUCKET_EMPTY,SoundSource.BLOCKS,0.6f,1.35f);
                player.displayClientMessage(Component.translatable("message.sfcdelight.hands_washed"), true);
            } else {
                player.displayClientMessage(Component.translatable("message.sfcdelight.sink_empty"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (bench.marinationReady()) {
            ItemStack ready = bench.completeMarination();
            grant(player,ready);
            grant(player,new ItemStack(SaltedFriedChickenDelight.MARINATION_BOX.get()));
            level.playSound(null,pos,SoundEvents.BARREL_OPEN,SoundSource.BLOCKS,0.55f,1.1f);
            player.displayClientMessage(Component.literal("醃製完成，取得食材與空醃製箱。"), true);
            return InteractionResult.SUCCESS;
        }
        if (bench.marinating()) {
            player.displayClientMessage(Component.literal("醃製中：" + bench.marinationSeconds() + " / 180 秒"), true);
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && bench.hasBowl() && !bench.hasIngredient() && !bench.hasFlour()) {
            grant(player,bench.takeEmptyBowl());
            return InteractionResult.SUCCESS;
        }
        if (bench.hasBowl() && bench.hasIngredient() && bench.hasFlour()) {
            ItemStack result = bench.mixBatter();
            level.playSound(null,pos,SoundEvents.SLIME_BLOCK_HIT,SoundSource.BLOCKS,0.45f,1.3f);
            if (!result.isEmpty()) {
                grant(player,result);
                player.addEffect(new MobEffectInstance(
                        SaltedFriedChickenDelight.SLIPPERY_HANDS, MobEffectInstance.INFINITE_DURATION));
                player.displayClientMessage(Component.literal("裹粉完成！手上沾滿麵糊，請到洗手台洗手。"), true);
            } else player.displayClientMessage(Component.literal("裹粉操作：" + bench.batterClicks() + " / 3 次"), true);
            return InteractionResult.SUCCESS;
        }
        player.displayClientMessage(Component.literal(bench.hasBowl()
                ? "放入醃好的食材與小麥粉，空手右鍵 3 次完成裹粉。"
                : "左側可以放入醃製箱（3 分鐘），或放上鋼盆裹粉。"), true);
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Creative mode removes the whole 2x1 counter without minting a spare item.
        if (!level.isClientSide && player.getAbilities().instabuild) {
            PreparationCounterBlockEntity bench = getBench(level, pos, state);
            if (bench != null) bench.suppressCounterDrop();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !DISMANTLING.get()) {
            DISMANTLING.set(true);
            try {
                BlockPos master = masterPos(pos,state);
                BlockState root = level.getBlockState(master);
                boolean rooted = root.getBlock() instanceof PreparationCounterBlock
                        && root.getValue(PART) == 0 && root.getValue(FACING) == state.getValue(FACING);
                if (rooted) {
                    if (level.getBlockEntity(master) instanceof PreparationCounterBlockEntity bench)
                        for (ItemStack item: bench.drain()) drop(level,master,item);
                    if (!moved && level.getGameRules().getBoolean(
                            net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                            && (level.getBlockEntity(master) instanceof PreparationCounterBlockEntity bench
                                && bench.shouldDropCounter()))
                        drop(level,master,new ItemStack(SaltedFriedChickenDelight.PREPARATION_COUNTER_ITEM.get()));
                    BlockPos other=master.relative(state.getValue(FACING).getClockWise());
                    if (level.getBlockState(other).getBlock() == this) level.removeBlock(other,false);
                    if (!master.equals(pos)) level.removeBlock(master,false);
                }
            } finally { DISMANTLING.set(false); }
        }
        super.onRemove(state,level,pos,next,moved);
    }
    private static void drop(Level level, BlockPos pos, ItemStack item) {
        if (!item.isEmpty()) net.minecraft.world.Containers.dropItemStack(
                level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,item);
    }
}
