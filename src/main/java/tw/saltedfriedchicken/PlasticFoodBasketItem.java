package tw.saltedfriedchicken;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** All 16 plastic basket colors share the same live contents tooltip. */
public class PlasticFoodBasketItem extends Item {
    public PlasticFoodBasketItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(FoodContentsTooltip.fromContainer(stack, false));
    }
}
