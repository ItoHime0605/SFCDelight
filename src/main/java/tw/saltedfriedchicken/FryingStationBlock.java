package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class FryingStationBlock extends BaseEntityBlock {
    public static final MapCodec<FryingStationBlock> CODEC = simpleCodec(FryingStationBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    public static final IntegerProperty INDICATOR = IntegerProperty.create("indicator", 0, 6);
    public FryingStationBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(INDICATOR, 0));
    }
    /** Five colored oil levels, plus a separate empty-oil red warning.
     * Each oil item gives two frying charges, ten items = twenty batches.
     * The old indicator=6 state stays registered for alpha-world migration.
     */
    public static int stageForCharges(int charges) {
        if (charges <= 0) return 0;
        if (charges <= 4) return 1;
        if (charges <= 8) return 2;
        if (charges <= 12) return 3;
        if (charges <= 16) return 4;
        return 5;
    }
    /** Client-only visual particles: no server-side particle broadcast or per-tick inventory scan. */
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        // The oil surface remains visually full even when no frying charges remain.
        // The active flag is read from the synchronized block entity, not inferred from oil level.
        boolean frying = level.getBlockEntity(pos) instanceof FryingStationBlockEntity fryer && fryer.isFrying();
        if (!frying && random.nextInt(12) != 0) return;
        int count = frying ? 2 + random.nextInt(3) : 1;
        for (int i = 0; i < count; i++) {
            double x = pos.getX() + 0.30 + random.nextDouble() * 0.40;
            double z = pos.getZ() + 0.30 + random.nextDouble() * 0.40;
            double y = pos.getY() + 0.82;
            level.addParticle(frying ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.SMOKE,
                    x, y, z, 0.0, 0.015 + random.nextDouble() * 0.015, 0.0);
            if (frying) level.addParticle(ParticleTypes.BUBBLE_POP, x, y - 0.015, z, 0.0, 0.015, 0.0);
        }
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(INDICATOR);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FryingStationBlockEntity(pos, state); }
    /** Recover from an unloaded/removed basket without retaining smoke indefinitely. */
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SaltedFriedChickenDelight.FRYER_ENTITY.get(), FryingStationBlock::serverTick);
    }
    private static void serverTick(Level level, BlockPos pos, BlockState state, FryingStationBlockEntity fryer) {
        if (fryer.isFrying() && !(level.getBlockEntity(pos.above()) instanceof FryingBasketBlockEntity basket
                && basket.hasPendingFry())) {
            fryer.setFrying(false);
        }
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    /** Empty hand shows exact remaining batches, not just the coarse five-stage gauge. */
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
            Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FryingStationBlockEntity fryer))
            return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) player.displayClientMessage(Component.literal(
                "食用油剩餘：" + fryer.charges() + " / 20 次油炸（每桶可補 2 次）"), true);
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // One fryer accepts exactly one prepared raw metal basket. Restore its
        // complete contents atomically and start frying only after successful placement.
        if (held.is(SaltedFriedChickenDelight.LOADED_RAW_BASKET.get())) {
            var raw = FriedBatchData.readAll(held, level.registryAccess());
            BlockPos basketPos = pos.above();
            if (!FryingRecipes.isValidRawBatch(raw, FryingBasketBlockEntity.MAX_PIECES)
                    || !level.getBlockState(basketPos).canBeReplaced()) return ItemInteractionResult.FAIL;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockState basketState = SaltedFriedChickenDelight.FRYING_BASKET_BLOCK.get()
                    .defaultBlockState()
                    .setValue(FryingBasketBlock.SUBMERGED, true)
                    .setValue(FryingBasketBlock.RAISED, true);
            if (!level.setBlock(basketPos, basketState, 3)) return ItemInteractionResult.FAIL;
            if (!(level.getBlockEntity(basketPos) instanceof FryingBasketBlockEntity basket)
                    || !basket.restoreRaw(raw)) {
                level.removeBlock(basketPos, false);
                return ItemInteractionResult.FAIL;
            }
            // This is a physical container transfer, including creative inventory.
            // Keeping the held item would duplicate the ten food portions.
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (held.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get())) {
            var foods = FriedBatchData.readAll(held, level.registryAccess());
            BlockPos basketPos = pos.above();
            if (!FryingRecipes.isValidCookedBatch(foods, FryingBasketBlockEntity.MAX_PIECES)
                    || !level.getBlockState(basketPos).canBeReplaced()) return ItemInteractionResult.FAIL;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            BlockState basketState = SaltedFriedChickenDelight.FRYING_BASKET_BLOCK.get()
                    .defaultBlockState().setValue(FryingBasketBlock.SUBMERGED, true);
            if (!level.setBlock(basketPos, basketState, 3)) return ItemInteractionResult.FAIL;
            if (!(level.getBlockEntity(basketPos) instanceof FryingBasketBlockEntity basket)
                    || !basket.restoreCooked(foods)) {
                level.removeBlock(basketPos, false);
                return ItemInteractionResult.FAIL;
            }
            // A filled hot basket is also a physical transfer, not an item copy.
            held.shrink(1);
            return ItemInteractionResult.SUCCESS;
        }
        if (!held.is(SaltedFriedChickenDelight.COOKING_OIL.get()) || !player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof FryingStationBlockEntity fryer)) return ItemInteractionResult.FAIL;
        if (!fryer.addOil()) {
            player.displayClientMessage(Component.translatable("message.sfcdelight.oil_full"), true);
            return ItemInteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) held.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }
}
