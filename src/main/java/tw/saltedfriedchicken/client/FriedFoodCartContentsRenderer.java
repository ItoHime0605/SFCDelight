package tw.saltedfriedchicken.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import tw.saltedfriedchicken.FriedFoodCartBlock;
import tw.saltedfriedchicken.FriedFoodCartBlockEntity;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/** Counter left, front-facing 4x3 tilted food display across the two blocks on the right. */
public final class FriedFoodCartContentsRenderer implements BlockEntityRenderer<FriedFoodCartBlockEntity> {
    private final ItemRenderer items;

    public FriedFoodCartContentsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public AABB getRenderBoundingBox(FriedFoodCartBlockEntity cart) {
        Direction right = cart.getBlockState().getValue(FriedFoodCartBlock.FACING).getClockWise();
        BlockPos origin = cart.getBlockPos();
        BlockPos far = origin.relative(right, 2);
        return new AABB(
                Math.min(origin.getX(), far.getX()), origin.getY(),
                Math.min(origin.getZ(), far.getZ()),
                Math.max(origin.getX(), far.getX()) + 1, origin.getY() + 2,
                Math.max(origin.getZ(), far.getZ()) + 1).inflate(0.5);
    }

    @Override
    public void render(FriedFoodCartBlockEntity cart, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction front = cart.getBlockState().getValue(FriedFoodCartBlock.FACING);
        Direction right = front.getClockWise();
        float angle = switch (front) {
            case NORTH -> 0.0f;
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 4; column++) {
                int slot = row * 4 + column;
                if (!cart.hasFrame(slot)) continue;
                // The counter is the first block; the showcase spans the next two.
                double across = 0.5 + (column + 0.5) * 0.5;
                double x = 0.5 + right.getStepX() * across + front.getStepX() * 0.27;
                double z = 0.5 + right.getStepZ() * across + front.getStepZ() * 0.27;
                double y = 1.78 - row * 0.27;

                // One installed plastic display frame per slot, even when its food is empty.
                pose.pushPose();
                pose.translate(x, y, z);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                pose.mulPose(Axis.XP.rotationDegrees(-19.0f));
                pose.scale(0.36f, 0.31f, 0.18f);
                items.renderStatic(new ItemStack(SaltedFriedChickenDelight.PLASTIC_FRAME.get()),
                        ItemDisplayContext.FIXED, packedLight, packedOverlay,
                        pose, buffers, cart.getLevel(), slot);
                pose.popPose();

                ItemStack food = cart.getDisplayStack(slot);
                if (food.isEmpty()) continue;
                int visible = Math.min(4, 1 + (food.getCount() - 1) / 3);
                ItemStack one = food.copyWithCount(1);
                for (int n = 0; n < visible; n++) {
                    pose.pushPose();
                    pose.translate(x + right.getStepX() * (n - 1.5) * 0.045,
                                   y + 0.035 + (n % 2) * 0.027,
                                   z + right.getStepZ() * (n - 1.5) * 0.045);
                    pose.mulPose(Axis.YP.rotationDegrees(angle));
                    pose.mulPose(Axis.XP.rotationDegrees(17.0f));
                    pose.mulPose(Axis.ZP.rotationDegrees((n % 3 - 1) * 11.0f));
                    pose.scale(0.22f, 0.22f, 0.22f);
                    items.renderStatic(one, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                            pose, buffers, cart.getLevel(), slot * 4 + n);
                    pose.popPose();
                }
            }
        }

        // The eight physical plastic baskets appear on the left shelf, including
        // each basket's real dye color. We never invent extra decorative baskets:
        // the render count exactly matches the inventory, bottom shelf first.
        for (int basketIndex = 0; basketIndex < cart.basketCount(); basketIndex++) {
            ItemStack stored = cart.getBasket(basketIndex);
            if (stored.isEmpty()) continue;
            int col = basketIndex % 4;
            int shelf = basketIndex / 4;
            double offset = (col - 1.5) * 0.215;
            pose.pushPose();
            pose.translate(0.5 + right.getStepX() * offset
                                      - front.getStepX() * 0.19,
                           1.34 + shelf * 0.25,
                           0.5 + right.getStepZ() * offset
                                      - front.getStepZ() * 0.19);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.mulPose(Axis.XP.rotationDegrees(17.5f));
            pose.scale(0.205f, 0.205f, 0.205f);
            items.renderStatic(stored, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    pose, buffers, cart.getLevel(), 500 + basketIndex);
            pose.popPose();
        }

        // The customer's seasonings sit on the left counter, not inside a food tray.
        // This renders the real stored pepper shaker, including its item model.
        ItemStack pepper = cart.getPepperJar();
        if (!pepper.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5 + front.getStepX() * 0.17, 1.14, 0.5 + front.getStepZ() * 0.17);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.scale(0.48f, 0.48f, 0.48f);
            items.renderStatic(pepper, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                    pose, buffers, cart.getLevel(), 360);
            pose.popPose();
        }
    }
}
