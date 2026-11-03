package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A read-only, automatically synchronised status menu for four existing equipment blocks.
 * No new inventories, item slots, packets, ticking or client-authoritative data.
 */
public final class EquipmentStatusMenu extends AbstractContainerMenu {
    public static final int CART = 0;
    public static final int FRYER = 1;
    public static final int DEOILER = 2;
    public static final int PREPARATION = 3;
    public static final int DATA_SLOTS = 17;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final Block validBlock;

    public EquipmentStatusMenu(int id, Inventory inventory) {
        this(id, new SimpleContainerData(DATA_SLOTS), ContainerLevelAccess.NULL, null);
    }

    private EquipmentStatusMenu(int id, ContainerData values, ContainerLevelAccess access, Block block) {
        super(SaltedFriedChickenDelight.EQUIPMENT_STATUS_MENU.get(), id);
        checkContainerDataCount(values, DATA_SLOTS);
        data = values;
        this.access = access;
        this.validBlock = block;
        addDataSlots(data);
    }

    public EquipmentStatusMenu(int id, Inventory inventory, BlockEntity equipment, BlockPos pos) {
        this(id, values(equipment), ContainerLevelAccess.create(inventory.player.level(), pos),
                equipment.getBlockState().getBlock());
    }

    /** Uses the server-side BlockEntity; read-only ContainerData is pushed through vanilla menu sync. */
    private static ContainerData values(BlockEntity equipment) {
        return new ContainerData() {
            @Override public int get(int index) {
                if (index < 0 || index >= DATA_SLOTS) return 0;
                if (equipment instanceof FriedFoodCartBlockEntity cart) {
                    if (index == 0) return CART;
                    if (index >= 1 && index <= 12) return cart.getDisplayStack(index - 1).getCount();
                    if (index == 13) {
                        int bits = 0;
                        for (int slot = 0; slot < 12; slot++) if (cart.hasFrame(slot)) bits |= 1 << slot;
                        return bits;
                    }
                    if (index == 14) return cart.basketCount();
                    if (index == 15) return PepperShakerItem.charges(cart.getPepperJar());
                    if (index == 16) return PepperShakerItem.charges(cart.getChiliJar());
                    return 0;
                }
                if (equipment instanceof FryingStationBlockEntity fryer) {
                    return switch (index) {
                        case 0 -> FRYER;
                        case 1 -> fryer.charges();
                        case 2 -> fryer.isFrying() ? 1 : 0;
                        case 3 -> FryingStationBlock.stageForCharges(fryer.charges());
                        default -> 0;
                    };
                }
                if (equipment instanceof DeOilerBlockEntity deoiler) {
                    return switch (index) {
                        case 0 -> DEOILER;
                        case 1 -> deoiler.elapsed();
                        case 2 -> DeOilerBlockEntity.CYCLE_TICKS;
                        case 3 -> deoiler.isComplete() ? 1 : 0;
                        case 4 -> deoiler.isEmpty() ? 0 : 1;
                        default -> 0;
                    };
                }
                if (equipment instanceof PreparationCounterBlockEntity prep) {
                    return switch (index) {
                        case 0 -> PREPARATION;
                        case 1 -> prep.marinationSeconds();
                        case 2 -> prep.marinationReady() ? 1 : 0;
                        case 3 -> prep.hasBowl() ? 1 : 0;
                        case 4 -> prep.hasIngredient() ? 1 : 0;
                        case 5 -> prep.hasFlour() ? 1 : 0;
                        case 6 -> prep.batterClicks();
                        case 7 -> prep.washes();
                        case 8 -> prep.marinating() ? 1 : 0;
                        default -> 0;
                    };
                }
                return 0;
            }

            @Override public void set(int index, int value) { /* Always server-owned. */ }
            @Override public int getCount() { return DATA_SLOTS; }
        };
    }

    public static void open(ServerPlayer player, BlockEntity equipment, BlockPos pos) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, viewer) -> new EquipmentStatusMenu(id, inventory, equipment, pos),
                Component.translatable("menu.sfcdelight.equipment_status")));
    }

    public int kind() { return data.get(0); }
    public int stat(int index) { return index >= 0 && index < DATA_SLOTS ? data.get(index) : 0; }

    @Override public boolean stillValid(Player player) {
        return validBlock == null || stillValid(access, player, validBlock);
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }
}
