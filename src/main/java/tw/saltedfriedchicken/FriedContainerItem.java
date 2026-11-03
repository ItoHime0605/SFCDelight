package tw.saltedfriedchicken;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Takeaway fried food: one right click consumes one actual portion, preserving
 * the remaining original stacks and custom data. No inventory tick is needed.
 */
public final class FriedContainerItem extends Item {
    public FriedContainerItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        // Both raw and cooked metal baskets are ONE physical container item.
        // List each ingredient and its quantity; never show only a single food.
        boolean rawBasket = stack.is(SaltedFriedChickenDelight.LOADED_RAW_BASKET.get());
        boolean hotBasket = stack.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get());
        if (rawBasket || hotBasket) {
            FoodContentsTooltip.appendMetalBasketContents(
                    tooltip, FriedBatchData.readAll(stack, context.registries()), hotBasket);
            tooltip.add(Component.literal(hotBasket
                    ? "拿著金屬炸籃右鍵脫油機，整籃轉移炸物"
                    : "拿著金屬炸籃右鍵油炸爐，整籃開始油炸"));
        } else {
            tooltip.add(FoodContentsTooltip.summarize(
                    FriedBatchData.readAll(stack, context.registries()), true));
        }
        if (WhitePepperItem.isPackage(stack)) {
            boolean whitePepper = WhitePepperItem.hasWhitePepper(stack);
            tooltip.add(Component.literal(whitePepper ? "調味：白胡椒（每份額外 +0.1 飽和度）" : "調味：原味"));
            tooltip.add(Component.literal("每吃一份：飢餓值 +1，飽和度 +" + (whitePepper ? "0.6" : "0.5")));
        }
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack packageStack = player.getItemInHand(hand);
        // Hot frying baskets and loaded scoops use this item class for tooltips,
        // but must never be edible directly; only takeaway packages are food.
        if (!packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())
                && !packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BOX.get()))
            return InteractionResultHolder.pass(packageStack);
        List<ItemStack> contents = FriedBatchData.readAll(packageStack, level.registryAccess());
        // Reject malformed packages before consuming food or replacing the held item.
        int capacity = packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(contents, capacity)) return InteractionResultHolder.fail(packageStack);
        if (!player.canEat(false)) return InteractionResultHolder.fail(packageStack);

        if (!level.isClientSide) {
            // Copy before mutation; original container data is untouched on failure.
            List<ItemStack> remaining = new ArrayList<>();
            for (ItemStack entry : contents) remaining.add(entry.copy());
            ItemStack eaten = ItemStack.EMPTY;
            for (int i = 0; i < remaining.size(); i++) {
                ItemStack entry = remaining.get(i);
                if (!entry.isEmpty()) {
                    eaten = entry.copyWithCount(1);
                    entry.shrink(1);
                    if (entry.isEmpty()) remaining.remove(i);
                    break;
                }
            }
            if (eaten.isEmpty()) return InteractionResultHolder.fail(packageStack);
            // Minecraft adds nutrition * saturationModifier * 2 to saturation.
            // One portion restores 1 hunger and exactly 0.5 saturation, or 0.6
            // when seasoned with white pepper, independent of container size.
            boolean whitePepper = WhitePepperItem.hasWhitePepper(packageStack);
            player.getFoodData().eat(1, whitePepper ? 0.30f : 0.25f);
            Item replacement = packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())
                    ? SaltedFriedChickenDelight.OILY_WASTE_PAPER_BAG.get()
                    : SaltedFriedChickenDelight.OILY_WASTE_PAPER_BOX.get();
            ItemStack next = remaining.isEmpty()
                    ? new ItemStack(replacement)
                    : FriedBatchData.wrapMany(packageStack.getItem(), remaining, level.registryAccess());
            if (!remaining.isEmpty() && whitePepper) WhitePepperItem.setWhitePepper(next);
            player.setItemInHand(hand, next);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
