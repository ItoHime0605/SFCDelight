package tw.saltedfriedchicken;

import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class DeOilerBlock extends BaseEntityBlock {
    public static final MapCodec<DeOilerBlock> CODEC = simpleCodec(DeOilerBlock::new);
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    public DeOilerBlock(Properties properties) { super(properties); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeOilerBlockEntity(pos, state); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, SaltedFriedChickenDelight.DEOILER_ENTITY.get(), DeOilerBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !(level.getBlockEntity(pos) instanceof DeOilerBlockEntity machine)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(SaltedFriedChickenDelight.HOT_FOOD_BASKET.get())) {
            if (machine.isEmpty()) {
                if (!level.isClientSide) {
                    if (!machine.insert(held)) return ItemInteractionResult.FAIL;
                    held.shrink(1);
                    level.playSound(null,pos,SoundEvents.IRON_TRAPDOOR_CLOSE,SoundSource.BLOCKS,0.5f,1.2f);
                    // Return exactly one empty basket only when the hot basket is consumed.
                    ItemStack emptyBasket = new ItemStack(SaltedFriedChickenDelight.FRYING_BASKET_ITEM.get());
                    if (!player.getInventory().add(emptyBasket)) player.drop(emptyBasket, false);
                }
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.FAIL;
        }
        if (held.is(SaltedFriedChickenDelight.STEEL_SCOOP.get())) {
            if (!machine.isComplete()) {
                if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.sfcdelight.deoiling_not_ready"), true);
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide) {
                // Prepare and validate the destination before removing the source.
                // A failed conversion must never consume a completed batch.
                ItemStack source = machine.contentsForDisplay();
                if (source.isEmpty() || !FryingRecipes.isValidCookedBatch(
                        FriedBatchData.readAll(source, level.registryAccess()), 10)) return ItemInteractionResult.FAIL;
                ItemStack loaded = FriedBatchData.transfer(source, SaltedFriedChickenDelight.LOADED_STEEL_SCOOP.get());
                if (loaded.isEmpty()) return ItemInteractionResult.FAIL;
                ItemStack food = machine.extractCompleted();
                if (food.isEmpty()) return ItemInteractionResult.FAIL;
                level.playSound(null,pos,SoundEvents.IRON_TRAPDOOR_OPEN,SoundSource.BLOCKS,0.5f,1.15f);
                // Keep other scoops in a stack intact; replace the held one when possible.
                if (held.getCount() == 1) {
                    player.setItemInHand(hand, loaded);
                } else {
                    held.shrink(1);
                    if (!player.getInventory().add(loaded)) player.drop(loaded, false);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DeOilerBlockEntity machine)) return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide) {
            String message = machine.isEmpty() ? "脫油機目前沒有炸物。" :
                machine.isComplete() ? "脫油完成！請使用不鏽鋼撈勺取出炸物。" :
                "正在脫油：" + (machine.elapsed() / 20) + " / 8 秒";
            if (!machine.isEmpty()) {
                var foods = FriedBatchData.readAll(machine.contentsForDisplay(), level.registryAccess());
                message += "  " + FoodContentsTooltip.summarize(foods, true).getString();
            }
            player.displayClientMessage(Component.literal(message), true);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof DeOilerBlockEntity machine)
            machine.suppressEquipmentDrop();
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean piston) {
        if (!state.is(next.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof DeOilerBlockEntity machine) {
            if (!piston && level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS) && machine.shouldDropEquipment()) {
                // The empty metal basket was already handed back on insertion.
                // Return the machine plus actual remaining fried portions only.
                ItemStack stored = machine.dropContents();
                for (ItemStack food : FriedBatchData.readAll(stored, level.registryAccess()))
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, food);
                net.minecraft.world.Containers.dropItemStack(level, pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, new ItemStack(SaltedFriedChickenDelight.DEOILER_ITEM.get()));
            } else {
                machine.dropContents();
            }
        }
        super.onRemove(state, level, pos, next, piston);
    }
}
