package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FryingBasketBlock extends BaseEntityBlock {
    public static final MapCodec<FryingBasketBlock> CODEC = simpleCodec(FryingBasketBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    public static final BooleanProperty SUBMERGED = BooleanProperty.create("submerged");
    public static final BooleanProperty LIFTING = BooleanProperty.create("lifting");
    public static final BooleanProperty RAISED = BooleanProperty.create("raised");
    public FryingBasketBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SUBMERGED, false).setValue(LIFTING, false).setValue(RAISED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(SUBMERGED, LIFTING, RAISED);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(SUBMERGED,
            context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof FryingStationBlock);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            boolean onFryer = neighborState.getBlock() instanceof FryingStationBlock;
            return state.setValue(SUBMERGED, onFryer).setValue(LIFTING, onFryer && state.getValue(LIFTING)).setValue(RAISED, onFryer && state.getValue(RAISED));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FryingBasketBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SaltedFriedChickenDelight.FRYING_BASKET_ENTITY.get(), FryingBasketBlockEntity::serverTick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (FryingRecipes.output(held) == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        // The client predicts the interaction, but the server alone changes inventories.
        if (level.isClientSide) {
            return basket.isEmpty() || (basket.hasPendingFry()
                    && !state.getValue(LIFTING) && !state.getValue(RAISED))
                    ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        if (!basket.insert(held)) return ItemInteractionResult.FAIL;
        if (!player.getAbilities().instabuild) held.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket) || basket.ingredient().isEmpty()) return net.minecraft.world.InteractionResult.PASS;
        if (state.getValue(RAISED)) {
            // Build the portable basket before mutating the block entity. A failed
            // component serialization must not consume the player's cooked food.
            java.util.List<ItemStack> cooked = basket.ingredients();
            if (!FryingRecipes.isValidCookedBatch(cooked, FryingBasketBlockEntity.MAX_PIECES))
                return net.minecraft.world.InteractionResult.FAIL;
            if (!level.isClientSide) {
                ItemStack hotBasket = FriedBatchData.wrapMany(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get(), cooked, level.registryAccess());
                if (hotBasket.isEmpty()) return net.minecraft.world.InteractionResult.FAIL;
                basket.extractAll();
                level.removeBlock(pos, false);
                // A filled hot basket already includes the metal basket: no duplicate.
                if (!player.getInventory().add(hotBasket)) player.drop(hotBasket, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            for (ItemStack out : basket.extractAll()) {
                if (!player.getInventory().add(out)) player.drop(out, false);
            }
            if (level.getBlockEntity(pos.below()) instanceof FryingStationBlockEntity fryer) fryer.setFrying(false);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket) {
            for (ItemStack contents : basket.extractAll()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, contents);
            }
            if (level.getBlockEntity(pos.below()) instanceof FryingStationBlockEntity fryer) fryer.setFrying(false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
