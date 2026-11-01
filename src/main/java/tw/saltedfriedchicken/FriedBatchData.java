package tw.saltedfriedchicken;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Keeps complete batches intact across hot basket, de-oiler and scoop. */
public final class FriedBatchData {
    private FriedBatchData() {}
    public static ItemStack wrap(Item container, ItemStack food, HolderLookup.Provider registries) {
        return wrapMany(container, List.of(food), registries);
    }
    public static ItemStack wrapMany(Item container, List<ItemStack> foods, HolderLookup.Provider registries) {
        // Validate all inputs before allocating or serializing a container.
        // A missing registry provider cannot encode 1.21.1 item components safely.
        if (container == null || registries == null)
            throw new IllegalArgumentException("Missing container item or registry provider");
        // Reject the whole batch before serializing anything. Never silently
        // drop a corrupt entry and accidentally consume portions on transfer.
        if (foods == null || foods.isEmpty() || foods.size() > 64)
            throw new IllegalArgumentException("Invalid fried food batch size");
        for (ItemStack food : foods) {
            if (food == null || food.isEmpty() || food.getCount() <= 0
                    || food.getCount() > food.getMaxStackSize())
                throw new IllegalArgumentException("Invalid fried food entry");
        }
        ItemStack result = new ItemStack(container);
        ListTag serialized = new ListTag();
        for (ItemStack food : foods) {
            serialized.add(food.save(registries));
        }
        if (!serialized.isEmpty()) {
            CompoundTag tag = new CompoundTag();
            tag.put("FriedFoods", serialized);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return result;
    }
    public static List<ItemStack> readAll(ItemStack container, HolderLookup.Provider registries) {
        if (container == null || container.isEmpty() || registries == null) return List.of();
        CustomData data = container.get(DataComponents.CUSTOM_DATA);
        if (data == null) return List.of();
        CompoundTag tag = data.copyTag();
        List<ItemStack> foods = new ArrayList<>();
        // Ambiguous old/new payloads are invalid; never guess which batch is authoritative.
        if (tag.contains("FriedFoods") && tag.contains("FriedFood")) return List.of();
        if (tag.contains("FriedFoods")) {
            if (!tag.contains("FriedFoods", 9)) return List.of();
            // getList(key, 10) returns an empty list when the element type is wrong.
            // Reject that case explicitly so malformed payloads cannot look like
            // legitimate empty containers. A serialized batch must contain food.
            ListTag list = tag.getList("FriedFoods", 10);
            if (list.isEmpty() || list.size() > 64) return List.of();
            for (int i = 0; i < list.size(); i++) {
                if (!(list.get(i) instanceof CompoundTag)) return List.of();
                ItemStack food = ItemStack.parseOptional(registries, list.getCompound(i));
                if (food.isEmpty() || food.getCount() <= 0 || food.getCount() > food.getMaxStackSize()) return List.of();
                foods.add(food);
            }
        } else if (tag.contains("FriedFood")) {
            if (!tag.contains("FriedFood", 10)) return List.of();
            ItemStack oldFood = ItemStack.parseOptional(registries, tag.getCompound("FriedFood"));
            if (oldFood.isEmpty() || oldFood.getCount() <= 0 || oldFood.getCount() > oldFood.getMaxStackSize()) return List.of();
            foods.add(oldFood);
        }
        return List.copyOf(foods);
    }
    /** Total individual portions; independent of number of varieties or serialized entries. */
    public static int countPortions(List<ItemStack> foods) {
        long total = 0;
        if (foods == null || foods.size() > 64) return Integer.MAX_VALUE;
        for (ItemStack food : foods) {
            if (food == null || food.isEmpty() || food.getCount() <= 0
                    || food.getCount() > food.getMaxStackSize()) return Integer.MAX_VALUE;
            total += food.getCount();
            if (total >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** Count distinct foods including data components (seasoning, custom variants, etc.). */
    public static int countDistinctTypes(List<ItemStack> foods) {
        if (foods == null || foods.size() > 64) return Integer.MAX_VALUE;
        List<ItemStack> unique = new ArrayList<>();
        for (ItemStack food : foods) {
            if (food == null || food.isEmpty() || food.getCount() <= 0
                    || food.getCount() > food.getMaxStackSize()) return Integer.MAX_VALUE;
            boolean seen = false;
            for (ItemStack previous : unique) {
                if (ItemStack.isSameItemSameComponents(food, previous)) { seen = true; break; }
            }
            if (!seen) unique.add(food);
        }
        return unique.size();
    }

    public static ItemStack read(ItemStack container, HolderLookup.Provider registries) {
        List<ItemStack> foods = readAll(container, registries);
        return foods.size() == 1 ? foods.get(0).copy() : ItemStack.EMPTY;
    }
    public static ItemStack transfer(ItemStack from, Item to) {
        if (from == null || from.isEmpty() || to == null) return ItemStack.EMPTY;
        CustomData data = from.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ItemStack.EMPTY;
        CompoundTag payload = data.copyTag();
        if (!payload.contains("FriedFoods") && !payload.contains("FriedFood")) return ItemStack.EMPTY;
        ItemStack result = new ItemStack(to);
        result.set(DataComponents.CUSTOM_DATA, data);
        return result;
    }
}
