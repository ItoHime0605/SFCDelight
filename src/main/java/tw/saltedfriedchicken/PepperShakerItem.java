package tw.saltedfriedchicken;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A reusable seasoning jar. Each shake seasons a complete takeaway package. */
public final class PepperShakerItem extends Item {
    public static final int CAPACITY = 60;
    public PepperShakerItem(Properties properties) { super(properties); }

    public static int charges(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0;
        return Math.max(0, Math.min(CAPACITY, data.copyTag().getInt("PepperCharges")));
    }

    public static void setCharges(ItemStack stack, int count) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("PepperCharges", Math.max(0, Math.min(CAPACITY, count)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("白胡椒粉剩餘次數：" + charges(stack) + " / " + CAPACITY));
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack jar = player.getItemInHand(hand);
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (other.is(SaltedFriedChickenDelight.WHITE_PEPPER_POWDER.get())) {
            if (charges(jar) > CAPACITY - 10) return InteractionResultHolder.fail(jar);
            if (!level.isClientSide) {
                setCharges(jar, charges(jar) + 10);
                if (!player.getAbilities().instabuild) other.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(jar, level.isClientSide);
        }
        if (!WhitePepperItem.isPackage(other) || charges(jar) <= 0 || WhitePepperItem.hasWhitePepper(other))
            return InteractionResultHolder.pass(jar);
        int capacity = other.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(other, level.registryAccess()), capacity))
            return InteractionResultHolder.fail(jar);
        if (!level.isClientSide) {
            WhitePepperItem.setWhitePepper(other);
            if (!player.getAbilities().instabuild) setCharges(jar, charges(jar) - 1);
        }
        return InteractionResultHolder.sidedSuccess(jar, level.isClientSide);
    }
}
