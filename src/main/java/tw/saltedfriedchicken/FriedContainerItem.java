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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Takeaway fried food: one right click consumes one actual portion, preserving
 * the remaining original stacks and custom data. No inventory tick is needed.
 */
public final class FriedContainerItem extends Item {
    public FriedContainerItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(FoodContentsTooltip.summarize(FriedBatchData.readAll(stack, context.registries()), true));
        if (WhitePepperItem.isPackage(stack)) {
            int spice = WhitePepperItem.spiceLevel(stack);
            String[] names = {"原味", "微辣", "小辣", "中辣", "大辣"};
            tooltip.add(Component.literal("辣度：" + names[spice]));
            if (spice > 0) tooltip.add(Component.literal("火焰抗性：" + new int[]{0,15,30,60,90}[spice] + " 秒"));
            if (spice == 4) tooltip.add(Component.literal("注意：大辣食用後會著火 5 秒"));
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
            // Eating one serving; food-specific effects can be integrated after
            // their exact definitions are verified. Avoid applying arbitrary buffs.
            player.getFoodData().eat(2, 0.3f);
            int spice = WhitePepperItem.spiceLevel(packageStack);
            if (spice > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                        new int[]{0, 300, 600, 1200, 1800}[spice]));
                if (spice == 4) player.igniteForSeconds(5);
            }
            Item replacement = packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())
                    ? SaltedFriedChickenDelight.OILY_WASTE_PAPER_BAG.get()
                    : SaltedFriedChickenDelight.OILY_WASTE_PAPER_BOX.get();
            ItemStack next = remaining.isEmpty()
                    ? new ItemStack(replacement)
                    : FriedBatchData.wrapMany(packageStack.getItem(), remaining, level.registryAccess());
            if (!remaining.isEmpty() && spice > 0) WhitePepperItem.setSpiceLevel(next, spice);
            player.setItemInHand(hand, next);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
