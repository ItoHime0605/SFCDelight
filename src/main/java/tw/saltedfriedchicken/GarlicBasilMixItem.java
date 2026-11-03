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

/** Garlic + basil seasoning: one serving refreshes five-minute Satisfaction. */
public final class GarlicBasilMixItem extends Item {
    public GarlicBasilMixItem(Properties properties){super(properties);}
    public static boolean hasMix(ItemStack food) {
        if (!WhitePepperItem.isPackage(food)) return false;
        CustomData data=food.get(DataComponents.CUSTOM_DATA);
        return data!=null && data.copyTag().getBoolean("GarlicBasil");
    }
    public static void mark(ItemStack food) {
        if (!WhitePepperItem.isPackage(food)) return;
        CompoundTag tag=food.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putBoolean("GarlicBasil",true);
        food.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack mix=player.getItemInHand(hand);
        ItemStack food=player.getItemInHand(hand==InteractionHand.MAIN_HAND?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
        if (!WhitePepperItem.isPackage(food)) return InteractionResultHolder.pass(mix);
        int capacity=food.is(SaltedFriedChickenDelight.FILLED_PAPER_BAG.get())?5:10;
        if (hasMix(food) || !FryingRecipes.isValidCookedBatch(
                FriedBatchData.readAll(food,level.registryAccess()),capacity))
            return InteractionResultHolder.fail(mix);
        if (!level.isClientSide) {
            mark(food);
            if (!player.getAbilities().instabuild) mix.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(mix,level.isClientSide);
    }
}
