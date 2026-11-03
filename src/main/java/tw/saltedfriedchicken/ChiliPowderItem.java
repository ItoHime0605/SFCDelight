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

/** Four chili levels; 30-second movement bonus, no fire or fire resistance. */
public final class ChiliPowderItem extends Item {
    public ChiliPowderItem(Properties properties) { super(properties); }
    public static int level(ItemStack stack) {
        if (!WhitePepperItem.isPackage(stack)) return 0;
        CustomData data=stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? 0 : Math.clamp(data.copyTag().getInt("ChiliLevel"), 0,4);
    }
    public static void setLevel(ItemStack stack, int level) {
        if (!WhitePepperItem.isPackage(stack)) return;
        CompoundTag tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putInt("ChiliLevel",Math.max(0,Math.min(4,level)));
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack powder=player.getItemInHand(hand);
        ItemStack food=player.getItemInHand(hand==InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!WhitePepperItem.isPackage(food)) return InteractionResultHolder.pass(powder);
        int cap=food.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()) ? 5 : 10;
        if (!FryingRecipes.isValidCookedBatch(FriedBatchData.readAll(food,world.registryAccess()),cap)
                || level(food) >= 4) return InteractionResultHolder.fail(powder);
        if (!world.isClientSide) {
            setLevel(food,level(food)+1);
            if (!player.getAbilities().instabuild) powder.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(powder,world.isClientSide);
    }
}
