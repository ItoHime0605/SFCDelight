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
        boolean onFryer = context.getLevel().getBlockState(context.getClickedPos().below()).getBlock() instanceof FryingStationBlock;
        // A fryer holds its empty basket above the oil until a valid batch is transferred.
        return defaultBlockState().setValue(SUBMERGED, onFryer).setValue(RAISED, onFryer);
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            boolean onFryer = neighborState.getBlock() instanceof FryingStationBlock;
            return state.setValue(SUBMERGED, onFryer)
                    .setValue(LIFTING, onFryer && state.getValue(LIFTING))
                    .setValue(RAISED, onFryer && (state.getValue(RAISED) || !state.getValue(SUBMERGED)));
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
        // Main-hand plastic basket, or iron tongs in main hand with plastic basket offhand.
        ItemStack plastic = held.getItem() instanceof PlasticFoodBasketItem ? held
                : held.is(SaltedFriedChickenDelight.IRON_TONGS.get())
                    && player.getOffhandItem().getItem() instanceof PlasticFoodBasketItem
                    ? player.getOffhandItem() : ItemStack.EMPTY;
        if (!plastic.isEmpty()) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            return basket.transferFromPlastic(plastic) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
        }
        // On the fryer, loose ingredients must NOT bypass the plastic basket batch step.
        if (FryingRecipes.output(held) == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (state.getValue(SUBMERGED)) return ItemInteractionResult.FAIL;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!basket.insert(held)) return ItemInteractionResult.FAIL;
        // The table stores a physical ingredient, even for creative players.
        held.shrink(1);
        return ItemInteractionResult.SUCCESS;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket) || basket.ingredient().isEmpty()) return net.minecraft.world.InteractionResult.PASS;
        // On a preparation table, a simple empty-hand right click lifts the
        // entire metal basket with all its raw portions. No sneak key required.
        // On a fryer, keep sneak-click for reclaiming an uncooked raised batch,
        // so ordinary fryer interactions are not unintentionally interrupted.
        if ((!state.getValue(SUBMERGED)
                    || (player.isShiftKeyDown() && state.getValue(RAISED)))
                && FryingRecipes.isValidRawBatch(basket.ingredients(), FryingBasketBlockEntity.MAX_PIECES)) {
            if (!level.isClientSide) {
                java.util.List<ItemStack> raw = basket.ingredients();
                ItemStack loaded = FriedBatchData.wrapMany(
                        SaltedFriedChickenDelight.LOADED_RAW_BASKET.get(),
                        raw, level.registryAccess());
                if (loaded.isEmpty()) return net.minecraft.world.InteractionResult.FAIL;
                basket.extractAll();
                basket.suppressEquipmentDrop();
                level.removeBlock(pos, false);
                if (!player.getInventory().add(loaded)) player.drop(loaded, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (state.getValue(RAISED) && FryingRecipes.isValidCookedBatch(basket.ingredients(), FryingBasketBlockEntity.MAX_PIECES)) {
            // Build the portable basket before mutating the block entity. A failed
            // component serialization must not consume the player's cooked food.
            java.util.List<ItemStack> cooked = basket.ingredients();
            if (!FryingRecipes.isValidCookedBatch(cooked, FryingBasketBlockEntity.MAX_PIECES))
                return net.minecraft.world.InteractionResult.FAIL;
            if (!level.isClientSide) {
                ItemStack hotBasket = FriedBatchData.wrapMany(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get(), cooked, level.registryAccess());
                if (hotBasket.isEmpty()) return net.minecraft.world.InteractionResult.FAIL;
                basket.extractAll();
                basket.suppressEquipmentDrop();
                level.removeBlock(pos, false);
                // A filled hot basket already includes the metal basket: no duplicate.
                if (!player.getInventory().add(hotBasket)) player.drop(hotBasket, false);
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Never scatter individual contents from an occupied metal frying basket.
        // Raw tabletop food is picked up as a whole above; cooked fryer contents
        // are wrapped in a whole hot basket. A raised raw fryer basket still
        // requires crouch-click to avoid accidental removal before frying.
        return net.minecraft.world.InteractionResult.FAIL;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket)
            basket.suppressEquipmentDrop();
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * Return one portable basket that reflects the actual contents at break time.
     * Cooking in progress remains RAW: a 15-second batch must never instantly
     * become cooked simply because the station below was removed.
     */
    public static void dropPortableBasket(Level level, BlockPos pos, FryingBasketBlockEntity basket) {
        java.util.List<ItemStack> foods = basket.ingredients();
        ItemStack dropped = ItemStack.EMPTY;
        if (foods.isEmpty()) {
            dropped = new ItemStack(SaltedFriedChickenDelight.FRYING_BASKET_ITEM.get());
        } else if (FryingRecipes.isValidRawBatch(foods, FryingBasketBlockEntity.MAX_PIECES)) {
            dropped = FriedBatchData.wrapMany(SaltedFriedChickenDelight.LOADED_RAW_BASKET.get(),
                    foods, level.registryAccess());
        } else if (FryingRecipes.isValidCookedBatch(foods, FryingBasketBlockEntity.MAX_PIECES)) {
            dropped = FriedBatchData.wrapMany(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get(),
                    foods, level.registryAccess());
        }
        if (!dropped.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, dropped);
        } else {
            // Corrupted legacy data: retain surviving ingredients and the
            // physical basket rather than silently deleting them.
            for (ItemStack food : foods) if (!food.isEmpty())
                net.minecraft.world.Containers.dropItemStack(
                        level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, food);
            net.minecraft.world.Containers.dropItemStack(level,
                    pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5,
                    new ItemStack(SaltedFriedChickenDelight.FRYING_BASKET_ITEM.get()));
        }
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof FryingBasketBlockEntity basket) {
            if (!movedByPiston
                    && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                    && basket.shouldDropEquipment()) {
                dropPortableBasket(level, pos, basket);
            }
            basket.extractAll();
            if (level.getBlockEntity(pos.below()) instanceof FryingStationBlockEntity fryer)
                fryer.setFrying(false);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
