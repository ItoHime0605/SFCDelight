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

/** White pepper is an optional one-time seasoning: +0.1 saturation per eaten portion. */
public final class WhitePepperItem extends Item {
    public WhitePepperItem(Properties properties) { super(properties); }

    public static boolean isPackage(ItemStack stack) {
        return stack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())
                || stack.is(SaltedFriedChickenDelight.FILLED_PAPER_BOX.get());
    }

    public static boolean hasWhitePepper(ItemStack stack) {
        if (!isPackage(stack)) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        CompoundTag tag = data.copyTag();
        // Migrate alpha worlds: PepperLevel previously (incorrectly) meant
        // white pepper. Never grant chili buffs or fire effects from legacy data.
        return tag.getBoolean("WhitePepper") || tag.getInt("PepperLevel") > 0;
    }

    public static void setWhitePepper(ItemStack stack) {
        if (!isPackage(stack)) return;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean("WhitePepper", true);
        tag.remove("PepperLevel");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack powder = player.getItemInHand(hand);
        ItemStack packageStack = player.getItemInHand(hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!isPackage(packageStack)) return InteractionResultHolder.pass(powder);
        int capacity = packageStack.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(packageStack, level.registryAccess()), capacity)
                || hasWhitePepper(packageStack)) return InteractionResultHolder.fail(powder);
        if (!level.isClientSide) {
            setWhitePepper(packageStack);
            if (!player.getAbilities().instabuild) powder.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(powder, level.isClientSide);
    }
}
