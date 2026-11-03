package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/** Read-only, server-synchronised water gauge for the right-hand sink.
 * No extra block entity and no client-authoritative water counter.
 */
public final class PreparationSinkMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;

    /** Client-side factory required by MenuType. Water is synced as a data slot. */
    public PreparationSinkMenu(int id, Inventory inventory) {
        this(id, new SimpleContainerData(1), ContainerLevelAccess.NULL);
    }

    /** Server-side menu bound to the master bench's existing water count. */
    public PreparationSinkMenu(int id, Inventory inventory, PreparationCounterBlockEntity bench, BlockPos sinkPos) {
        this(id, new ContainerData() {
            @Override public int get(int index) { return index == 0 ? bench.washes() : 0; }
            @Override public void set(int index, int value) { /* Server owns this value. */ }
            @Override public int getCount() { return 1; }
        }, ContainerLevelAccess.create(inventory.player.level(), sinkPos));
    }

    private PreparationSinkMenu(int id, ContainerData data, ContainerLevelAccess access) {
        super(SaltedFriedChickenDelight.PREPARATION_SINK_MENU.get(), id);
        checkContainerDataCount(data, 1);
        this.data = data;
        this.access = access;
        addDataSlots(data);
    }

    public int washes() { return Math.max(0, Math.min(PreparationCounterBlockEntity.MAX_WASHES, data.get(0))); }

    @Override public boolean stillValid(Player player) {
        return stillValid(access, player, SaltedFriedChickenDelight.PREPARATION_COUNTER.get());
    }

    /** No inventory slots or transferable items on this informational screen. */
    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
