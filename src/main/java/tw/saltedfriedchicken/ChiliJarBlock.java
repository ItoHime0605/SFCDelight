package tw.saltedfriedchicken;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.network.chat.Component;
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
import java.util.List;

/** Placeable refillable chili jar. Each portion of chili powder restores 10 charges. */
public final class ChiliJarBlock extends Block {
    public static final MapCodec<ChiliJarBlock> CODEC=simpleCodec(ChiliJarBlock::new);
    public static final IntegerProperty CHARGES=IntegerProperty.create("charges",0,60);
    private static final VoxelShape SHAPE=Block.box(5,0,5,11,11,11);
    public ChiliJarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGES,0));
    }
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(CHARGES);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params) {
        ItemStack jar=new ItemStack(SaltedFriedChickenDelight.CHILI_SHAKER.get());
        PepperShakerItem.setCharges(jar,state.getValue(CHARGES));
        return List.of(jar);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,BlockHitResult hit) {
        if (hand!=InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int charges=state.getValue(CHARGES);
        if (held.is(SaltedFriedChickenDelight.CHILI_POWDER.get())) {
            if (charges>50) return ItemInteractionResult.FAIL;
            if (!level.isClientSide) {
                level.setBlock(pos,state.setValue(CHARGES,charges+10),3);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (WhitePepperItem.isPackage(held)) {
            int cap=held.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
            if (charges==0 || ChiliPowderItem.level(held)>=4 || !FryingRecipes.isValidCookedBatch(
                    FriedBatchData.readAll(held,level.registryAccess()),cap)) return ItemInteractionResult.FAIL;
            if (!level.isClientSide) {
                ChiliPowderItem.setLevel(held,ChiliPowderItem.level(held)+1);
                if (!player.getAbilities().instabuild)
                    level.setBlock(pos,state.setValue(CHARGES,charges-1),3);
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            Player player,BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack jar=new ItemStack(SaltedFriedChickenDelight.CHILI_SHAKER.get());
            PepperShakerItem.setCharges(jar,state.getValue(CHARGES));
            level.removeBlock(pos,false);
            if (!player.getInventory().add(jar)) player.drop(jar,false);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
}
