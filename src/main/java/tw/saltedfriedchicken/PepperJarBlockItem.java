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

/** Transfer the portable jar's charge count into its placed block state. */
public final class PepperJarBlockItem extends BlockItem {
    public PepperJarBlockItem(Block block, Properties properties) { super(block, properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("白胡椒粉剩餘次數：" + PepperShakerItem.charges(stack) + " / 60"));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack jar = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.is(SaltedFriedChickenDelight.WHITE_PEPPER_POWDER.get())) {
            if (PepperShakerItem.charges(jar) > 50) return InteractionResultHolder.fail(jar);
            if (!level.isClientSide) {
                PepperShakerItem.setCharges(jar, PepperShakerItem.charges(jar) + 10);
                if (!player.getAbilities().instabuild) other.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(jar, level.isClientSide);
        }
        if (!WhitePepperItem.isPackage(other) || PepperShakerItem.charges(jar) <= 0 || WhitePepperItem.hasWhitePepper(other))
            return super.use(level, player, hand);
        int capacity = other.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(other, level.registryAccess()), capacity))
            return InteractionResultHolder.fail(jar);
        if (!level.isClientSide) {
            WhitePepperItem.setWhitePepper(other);
            if (!player.getAbilities().instabuild) PepperShakerItem.setCharges(jar, PepperShakerItem.charges(jar) - 1);
        }
        return InteractionResultHolder.sidedSuccess(jar, level.isClientSide);
    }
    @Override public InteractionResult place(BlockPlaceContext context) {
        int charges = PepperShakerItem.charges(context.getItemInHand());
        InteractionResult result = super.place(context);
        if (result.consumesAction() && !context.getLevel().isClientSide) {
            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            if (state.getBlock() instanceof PepperJarBlock)
                context.getLevel().setBlock(context.getClickedPos(), state.setValue(PepperJarBlock.CHARGES, charges), 3);
        }
        return result;
    }
}
