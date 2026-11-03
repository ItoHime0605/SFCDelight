package tw.saltedfriedchicken;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Data-preserving shapeless refill: exactly one shaker and one pepper powder. */
public final class PepperRefillRecipe extends CustomRecipe {
    public PepperRefillRecipe(CraftingBookCategory category) { super(category); }

    private ItemStack findJar(CraftingInput input) {
        ItemStack jar = ItemStack.EMPTY;
        int powder = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(SaltedFriedChickenDelight.PEPPER_SHAKER.get()) && jar.isEmpty()) jar = stack;
            else if (stack.is(SaltedFriedChickenDelight.WHITE_PEPPER_POWDER.get())) powder++;
            else return ItemStack.EMPTY;
        }
        return powder == 1 ? jar : ItemStack.EMPTY;
    }

    @Override public boolean matches(CraftingInput input, Level level) {
        ItemStack jar = findJar(input);
        return !jar.isEmpty() && PepperShakerItem.charges(jar) <= 50;
    }

    @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack jar = findJar(input);
        if (jar.isEmpty() || PepperShakerItem.charges(jar) > 50) return ItemStack.EMPTY;
        ItemStack result = jar.copyWithCount(1);
        PepperShakerItem.setCharges(result, PepperShakerItem.charges(jar) + 10);
        return result;
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }
    @Override public RecipeSerializer<?> getSerializer() { return SaltedFriedChickenDelight.PEPPER_REFILL_SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<PepperRefillRecipe> {
        public static final MapCodec<PepperRefillRecipe> CODEC = MapCodec.unit(new PepperRefillRecipe(CraftingBookCategory.MISC));
        public static final StreamCodec<RegistryFriendlyByteBuf, PepperRefillRecipe> STREAM_CODEC =
                // Network sync must accept recipe objects created by the JSON codec.
                // StreamCodec.unit(value) checks *object identity* on encode, which
                // breaks when the JSON parser supplies another recipe instance.
                // The recipe has no payload beyond its fixed MISC category.
                StreamCodec.of((buffer, recipe) -> {
                    // No per-recipe fields to transmit.
                }, buffer -> new PepperRefillRecipe(CraftingBookCategory.MISC));
        @Override public MapCodec<PepperRefillRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, PepperRefillRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
