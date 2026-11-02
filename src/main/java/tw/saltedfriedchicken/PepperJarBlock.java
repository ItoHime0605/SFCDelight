package tw.saltedfriedchicken;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** Countertop pepper jar; the block state persists all sixty charges. */
public final class PepperJarBlock extends Block {
    public static final MapCodec<PepperJarBlock> CODEC = simpleCodec(PepperJarBlock::new);
    public static final IntegerProperty CHARGES = IntegerProperty.create("charges", 0, 60);
    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 11, 11);

    public PepperJarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGES, 0));
    }
    @Override protected MapCodec<? extends Block> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGES);
    }
    /** Preserve charges in drops, including ordinary mining and explosions. */
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack jar = new ItemStack(SaltedFriedChickenDelight.PEPPER_SHAKER.get());
        PepperShakerItem.setCharges(jar, state.getValue(CHARGES));
        return List.of(jar);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!held.is(SaltedFriedChickenDelight.WHITE_PEPPER_POWDER.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int current = state.getValue(CHARGES);
        if (current > 50) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(CHARGES, current + 10), 3);
            if (!player.getAbilities().instabuild) held.shrink(1);
        }
        return ItemInteractionResult.SUCCESS;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack jar = new ItemStack(SaltedFriedChickenDelight.PEPPER_SHAKER.get());
            PepperShakerItem.setCharges(jar, state.getValue(CHARGES));
            level.removeBlock(pos, false);
            if (!player.getInventory().add(jar)) player.drop(jar, false);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
