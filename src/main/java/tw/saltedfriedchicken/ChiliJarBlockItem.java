package tw.saltedfriedchicken;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Red chili shaker, separately refillable and placeable. */
public final class ChiliJarBlockItem extends BlockItem {
    public ChiliJarBlockItem(Block block,Properties props){super(block,props);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext ctx,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,ctx,lines,flag);
        lines.add(Component.literal("辣椒粉剩餘次數："+PepperShakerItem.charges(stack)+" / 60"));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack jar=player.getItemInHand(hand);
        ItemStack other=player.getItemInHand(hand==InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if (other.is(SaltedFriedChickenDelight.CHILI_POWDER.get())) {
            if (PepperShakerItem.charges(jar)>50) return InteractionResultHolder.fail(jar);
            if (!level.isClientSide) {
                PepperShakerItem.setCharges(jar,PepperShakerItem.charges(jar)+10);
                if (!player.getAbilities().instabuild) other.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(jar,level.isClientSide);
        }
        if (!WhitePepperItem.isPackage(other) || PepperShakerItem.charges(jar)==0
                || ChiliPowderItem.level(other)>=4) return super.use(level,player,hand);
        int cap=other.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())?5:10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(other,level.registryAccess()),cap))
            return InteractionResultHolder.fail(jar);
        if (!level.isClientSide) {
            ChiliPowderItem.setLevel(other,ChiliPowderItem.level(other)+1);
            if (!player.getAbilities().instabuild)
                PepperShakerItem.setCharges(jar,PepperShakerItem.charges(jar)-1);
        }
        return InteractionResultHolder.sidedSuccess(jar,level.isClientSide);
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        int charges=PepperShakerItem.charges(context.getItemInHand());
        InteractionResult result=super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            BlockState state=context.getLevel().getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof ChiliJarBlock)
                context.getLevel().setBlock(context.getClickedPos(),state.setValue(ChiliJarBlock.CHARGES,charges),3);
        }
        return result;
    }
}
