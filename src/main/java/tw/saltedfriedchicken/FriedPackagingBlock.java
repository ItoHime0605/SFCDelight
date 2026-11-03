package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Bag holds five total portions; box holds ten, regardless of varieties. */
public class FriedPackagingBlock extends BaseEntityBlock {
    public static final MapCodec<FriedPackagingBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockBehaviour.propertiesCodec(),
            Codec.intRange(1, 64).fieldOf("capacity").forGetter(block -> block.capacity)
    ).apply(instance, FriedPackagingBlock::new));
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    private final int capacity;
    public FriedPackagingBlock(Properties properties, int capacity) { super(properties); this.capacity = capacity; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FriedPackagingBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !held.is(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FriedPackagingBlockEntity container)) return ItemInteractionResult.FAIL;
        java.util.List<ItemStack> foods = FriedBatchData.readAll(held, level.registryAccess());
        java.util.List<ItemStack> stored = FriedBatchData.readAll(container.getContents(), level.registryAccess());
        if (!container.isEmpty() && (!container.getContents().is(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get())
                || !FryingRecipes.isValidCookedBatch(stored, capacity))) return ItemInteractionResult.FAIL;
        int available = capacity - FriedBatchData.countPortions(stored);
        if (available <= 0) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal("太多炸物了，放不進去。"), true);
            return ItemInteractionResult.FAIL;
        }
        // Validate the whole scoop on both sides before acknowledging the interaction.
        // Otherwise the client can report success while the server rejects a damaged
        // or overfilled scoop, producing a misleading ghost transfer animation.
        if (!FryingRecipes.isValidCookedBatch(foods, 10)) return ItemInteractionResult.FAIL;
        if (!level.isClientSide) {
            int moved = container.putPortions(foods, capacity);
            if (moved <= 0) return ItemInteractionResult.FAIL;
            int current = FriedBatchData.countPortions(
                    FriedBatchData.readAll(container.getContents(), level.registryAccess()));
            player.displayClientMessage(Component.literal(
                    "已裝入 " + moved + " 份炸物；目前 " + current + " / " + capacity + " 份。"), true);
            java.util.ArrayList<ItemStack> remaining = new java.util.ArrayList<>();
            int consumed = moved;
            for (ItemStack food : foods) {
                int used = Math.min(consumed, food.getCount());
                consumed -= used;
                if (food.getCount() > used) remaining.add(food.copyWithCount(food.getCount() - used));
            }
            ItemStack replacement = remaining.isEmpty()
                    ? new ItemStack(SaltedFriedChickenDelight.STEEL_SCOOP.get())
                    : FriedBatchData.wrapMany(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get(), remaining, level.registryAccess());
            if (held.getCount() == 1) {
                player.setItemInHand(hand, replacement);
            } else {
                held.shrink(1);
                if (!player.getInventory().add(replacement)) player.drop(replacement, false);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FriedPackagingBlockEntity container))
            return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                java.util.List<ItemStack> stored = FriedBatchData.readAll(
                        container.getContents(), level.registryAccess());
                int portions = container.isEmpty() ? 0 : FriedBatchData.countPortions(stored);
                if (!container.isEmpty() && !FryingRecipes.isValidCookedBatch(stored, capacity)) {
                    player.displayClientMessage(Component.literal("包裝內容資料異常，無法取出成品。"), true);
                } else {
                    player.displayClientMessage(Component.literal(
                            "紙" + (capacity == 5 ? "袋" : "盒") + "目前 " + portions
                            + " / " + capacity + " 份。"
                            + (stored.isEmpty() ? "" : " " + FoodContentsTooltip.summarize(stored, true).getString())), true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (container.isEmpty()) {
            if (!level.isClientSide) player.displayClientMessage(Component.literal(
                    "紙" + (capacity == 5 ? "袋" : "盒") + "目前是空的，請使用裝有炸物的撈勺加入食材。"), true);
            return InteractionResult.SUCCESS;
        }
        // Never convert a corrupted saved scoop into a takeaway package. Doing so
        // would hide invalid food data behind a seemingly usable food item.
        if (!container.getContents().is(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get())
                || !FryingRecipes.isValidCookedBatch(
                FriedBatchData.readAll(container.getContents(), level.registryAccess()), capacity))
            return InteractionResult.FAIL;
        if (!level.isClientSide) {
            ItemStack scoop = container.take();
            if (scoop.isEmpty()) return InteractionResult.FAIL;
            ItemStack packed = FriedBatchData.transfer(scoop, capacity == 5 ? SaltedFriedChickenDelight.FILLED_PAPER_BAG.get() : SaltedFriedChickenDelight.FILLED_PAPER_BOX.get());
            container.suppressEquipmentDrop();
            level.removeBlock(pos, false);
            if (!player.getInventory().add(packed)) player.drop(packed, false);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof FriedPackagingBlockEntity packageEntity)
            packageEntity.suppressEquipmentDrop();
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean piston) {
        if (!state.is(next.getBlock()) && !level.isClientSide && !piston
                && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                && level.getBlockEntity(pos) instanceof FriedPackagingBlockEntity container
                && container.shouldDropEquipment()) {
            ItemStack stored = container.take();
            ItemStack dropped;
            if (stored.isEmpty()) {
                // An unused placed bag/box is recovered as the original empty item.
                dropped = new ItemStack(capacity == 5
                        ? SaltedFriedChickenDelight.PAPER_BAG_ITEM.get()
                        : SaltedFriedChickenDelight.PAPER_BOX_ITEM.get());
            } else if (stored.is(SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get())
                    && FryingRecipes.isValidCookedBatch(
                            FriedBatchData.readAll(stored, level.registryAccess()), capacity)) {
                // One packed takeaway replaces the placed empty package.
                dropped = FriedBatchData.transfer(stored, capacity == 5
                        ? SaltedFriedChickenDelight.FILLED_PAPER_BAG.get()
                        : SaltedFriedChickenDelight.FILLED_PAPER_BOX.get());
            } else {
                // Never silently discard unrecognized data or make a fake food bag.
                dropped = stored;
            }
            if (!dropped.isEmpty())
                net.minecraft.world.Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, dropped);
        }
        super.onRemove(state, level, pos, next, piston);
    }
}
