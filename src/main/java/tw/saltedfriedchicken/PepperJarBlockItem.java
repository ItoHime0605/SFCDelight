package tw.saltedfriedchicken;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Transfer the portable jar's charge count into its placed block state. */
public final class PepperJarBlockItem extends BlockItem {
    public PepperJarBlockItem(Block block, Properties properties) { super(block, properties); }
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
