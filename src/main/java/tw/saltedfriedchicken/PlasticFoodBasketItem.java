package tw.saltedfriedchicken;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** All 16 plastic basket colors share the same live contents tooltip. */
public class PlasticFoodBasketItem extends Item {
    public PlasticFoodBasketItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int portions = contents.stream().filter(item -> !item.isEmpty())
                .mapToInt(ItemStack::getCount).sum();
        tooltip.add(Component.literal("食材份數：" + portions + " / " + FryingBasketBlockEntity.MAX_PIECES));
        tooltip.add(FoodContentsTooltip.fromContainer(stack, false));
    }
}
