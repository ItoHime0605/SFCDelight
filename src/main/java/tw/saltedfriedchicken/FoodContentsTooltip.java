package tw.saltedfriedchicken;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Single-line Taiwanese Chinese contents summary. Never modifies the stored ingredients. */
public final class FoodContentsTooltip {
    private FoodContentsTooltip() {}

    /** @param cooked Whether the entire batch has completed frying. */
    public static Component summarize(Iterable<ItemStack> stacks, boolean cooked) {
        List<String> names = new ArrayList<>();
        List<Integer> amounts = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            String name = stack.getHoverName().getString();
            int index = names.indexOf(name);
            if (index >= 0) amounts.set(index, amounts.get(index) + stack.getCount());
            else { names.add(name); amounts.add(stack.getCount()); }
        }
        List<String> entries = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) entries.add(names.get(i) + " ×" + amounts.get(i));
        if (entries.isEmpty()) return Component.literal("內含：無");
        if (cooked && !entries.isEmpty()) {
            String first = entries.get(0);
            // Avoid duplicate wording such as 「炸好的炸魷魚腳」.
            if (first.startsWith("炸")) first = "炸好的" + first.substring(1);
            else first = "炸好的" + first;
            entries.set(0, first);
        }
        return Component.literal("內含：" + String.join("、", entries));
    }

    public static Component fromContainer(ItemStack container, boolean cooked) {
        ItemContainerContents contents = container.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return summarize(contents.stream().toList(), cooked);
    }
}
