package tw.saltedfriedchicken;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Season a complete takeaway package held in the other hand, up to four shakes. */
public final class WhitePepperItem extends Item {
    public WhitePepperItem(Properties properties) { super(properties); }

    public static boolean isPackage(ItemStack stack) {
        return stack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())
                || stack.is(SaltedFriedChickenDelight.FILLED_PAPER_BOX.get());
    }

    public static int spiceLevel(ItemStack stack) {
        if (!isPackage(stack)) return 0;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        int value = data.copyTag().getInt("PepperLevel");
        return Math.max(0, Math.min(4, value));
    }

    public static void setSpiceLevel(ItemStack stack, int value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("PepperLevel", Math.max(0, Math.min(4, value)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack powder = player.getItemInHand(hand);
        ItemStack packageStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!isPackage(packageStack)) return InteractionResultHolder.pass(powder);
        int capacity = packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(packageStack, level.registryAccess()), capacity))
            return InteractionResultHolder.fail(powder);
        int oldLevel = spiceLevel(packageStack);
        if (oldLevel >= 4) return InteractionResultHolder.fail(powder);
        if (!level.isClientSide) {
            setSpiceLevel(packageStack, oldLevel + 1);
            if (!player.getAbilities().instabuild) powder.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(powder, level.isClientSide);
    }
}
