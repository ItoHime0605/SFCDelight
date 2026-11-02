package tw.saltedfriedchicken;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

/** Fixed server-side recipes. Optional mod recipes can be added after validating IDs. */
public final class FryingRecipes {
    private FryingRecipes() {}
    public static Item output(ItemStack input) {
        if (input == null || input.isEmpty()) return null;
        if (input.is(SaltedFriedChickenDelight.BATTERED_SQUID_TENTACLES.get())) return SaltedFriedChickenDelight.FRIED_SQUID_TENTACLES.get();
        if (input.is(SaltedFriedChickenDelight.RAW_TEMPURA_FISH_CAKE.get())) return SaltedFriedChickenDelight.FRIED_TEMPURA_FISH_CAKE.get();
        if (input.is(SaltedFriedChickenDelight.RAW_SQUID_BALL.get())) return SaltedFriedChickenDelight.FRIED_SQUID_BALL.get();
        if (input.is(SaltedFriedChickenDelight.RAW_FRIES.get())) return SaltedFriedChickenDelight.FRIED_FRIES.get();
        if (input.is(SaltedFriedChickenDelight.RAW_SWEET_POTATO_FRIES.get())) return SaltedFriedChickenDelight.FRIED_SWEET_POTATO_FRIES.get();
        if (input.is(SaltedFriedChickenDelight.RAW_CHICKEN_DICE.get())) return SaltedFriedChickenDelight.FRIED_CHICKEN_DICE.get();
        if (input.is(SaltedFriedChickenDelight.RAW_COD_DICE.get())) return SaltedFriedChickenDelight.FRIED_COD_DICE.get();
        if (input.is(SaltedFriedChickenDelight.DRIED_TOFU.get())) return SaltedFriedChickenDelight.FRIED_DRIED_TOFU.get();
        if (input.is(SaltedFriedChickenDelight.HUNDRED_LAYER_TOFU.get())) return SaltedFriedChickenDelight.FRIED_HUNDRED_LAYER_TOFU.get();
        if (input.is(SaltedFriedChickenDelight.RICE_BLOOD_CAKE.get())) return SaltedFriedChickenDelight.FRIED_RICE_BLOOD_CAKE.get();
        return null;
    }
    /** Only food produced by this fryer's recipes may be restored as cooked food. */
    public static boolean isCookedOutput(ItemStack food) {
        if (food == null || food.isEmpty()) return false;
        return food.is(SaltedFriedChickenDelight.FRIED_SQUID_TENTACLES.get())
            || food.is(SaltedFriedChickenDelight.FRIED_TEMPURA_FISH_CAKE.get())
            || food.is(SaltedFriedChickenDelight.FRIED_SQUID_BALL.get())
            || food.is(SaltedFriedChickenDelight.FRIED_FRIES.get())
            || food.is(SaltedFriedChickenDelight.FRIED_SWEET_POTATO_FRIES.get())
            || food.is(SaltedFriedChickenDelight.FRIED_CHICKEN_DICE.get())
            || food.is(SaltedFriedChickenDelight.FRIED_COD_DICE.get())
            || food.is(SaltedFriedChickenDelight.FRIED_DRIED_TOFU.get())
            || food.is(SaltedFriedChickenDelight.FRIED_HUNDRED_LAYER_TOFU.get())
            || food.is(SaltedFriedChickenDelight.FRIED_RICE_BLOOD_CAKE.get());
    }

    /** Validate every entry before changing the world or consuming a held basket. */
    public static boolean isValidCookedBatch(List<ItemStack> foods, int maxPortions) {
        if (foods == null || foods.isEmpty() || foods.size() > 64 || maxPortions <= 0) return false;
        int portions = FriedBatchData.countPortions(foods);
        return portions > 0 && portions <= maxPortions
            && foods.stream().allMatch(food -> isCookedOutput(food)
                && food.getCount() > 0 && food.getCount() <= food.getMaxStackSize());
    }

    public static List<ItemStack> cook(List<ItemStack> raw) {
        List<ItemStack> cooked = new ArrayList<>();
        if (raw == null || raw.isEmpty() || raw.size() > 64) return List.of();
        int portions = FriedBatchData.countPortions(raw);
        if (portions <= 0 || portions > FryingBasketBlockEntity.MAX_PIECES) return List.of();
        for (ItemStack stack : raw) {
            // Reject malformed or externally edited ingredient stacks before producing food.
            if (stack == null || stack.isEmpty() || stack.getCount() <= 0
                    || stack.getCount() > stack.getMaxStackSize()) return List.of();
            Item item = output(stack);
            if (item == null) return List.of();
            ItemStack result = new ItemStack(item, stack.getCount());
            if (result.getCount() > result.getMaxStackSize()) return List.of();
            // Combine only when the destination stack has enough room.
            // Otherwise preserve the second stack instead of exceeding its limit.
            boolean merged = false;
            for (ItemStack existing : cooked) {
                if (ItemStack.isSameItemSameComponents(existing, result)
                        && existing.getCount() <= existing.getMaxStackSize() - result.getCount()) {
                    existing.grow(result.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) cooked.add(result);
        }
        return cooked;
    }
}
