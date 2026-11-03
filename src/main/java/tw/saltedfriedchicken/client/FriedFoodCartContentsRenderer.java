package tw.saltedfriedchicken.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tw.saltedfriedchicken.FriedFoodCartBlock;
import tw.saltedfriedchicken.FriedFoodCartBlockEntity;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/**
 * Twelve separate angled plastic frames, four across and three deep.
 * One master block entity renders the contents across the whole 2x2x2 cart.
 */
public final class FriedFoodCartContentsRenderer implements BlockEntityRenderer<FriedFoodCartBlockEntity> {
    private final ItemRenderer items;

    public FriedFoodCartContentsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(FriedFoodCartBlockEntity cart) {
        Direction front = cart.getBlockState().getValue(FriedFoodCartBlock.FACING);
        Direction back = front.getOpposite();
        Direction right = front.getClockWise();
        net.minecraft.core.BlockPos origin = cart.getBlockPos();
        net.minecraft.core.BlockPos far = origin.relative(right).relative(back);
        return new net.minecraft.world.phys.AABB(
                Math.min(origin.getX(), far.getX()), origin.getY(),
                Math.min(origin.getZ(), far.getZ()),
                Math.max(origin.getX(), far.getX()) + 1, origin.getY() + 2,
                Math.max(origin.getZ(), far.getZ()) + 1).inflate(0.5);
    }

    @Override
    public void render(FriedFoodCartBlockEntity cart, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        Direction front = cart.getBlockState().getValue(FriedFoodCartBlock.FACING);
        Direction back = front.getOpposite();
        Direction right = front.getClockWise();

        // Four across the TWO-block width; three rows across the TWO-block depth.
        for (int depth = 0; depth < 3; depth++) {
            for (int column = 0; column < 4; column++) {
                int slot = depth * 4 + column;
                if (!cart.hasFrame(slot)) continue;

                // World-space positions relative to the front-left master block.
                // Each slot occupies 0.5 blocks across and 2/3 blocks front-to-back.
                double across = (column + 0.5) * 0.5 - 0.5;
                double frontBack = (depth + 0.5) * (2.0 / 3.0) - 0.5;
                double x = 0.5 + back.getStepX() * frontBack + right.getStepX() * across;
                double z = 0.5 + back.getStepZ() * frontBack + right.getStepZ() * across;
                float angle = switch (front) {
                    case NORTH -> 0.0f;
                    case EAST -> 90.0f;
                    case SOUTH -> 180.0f;
                    case WEST -> 270.0f;
                    default -> 0.0f;
                };

                // The physical plastic frame remains visible even when there is no food.
                pose.pushPose();
                pose.translate(x, 1.45, z);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                pose.mulPose(Axis.XP.rotationDegrees(-22.5f));
                pose.scale(0.40f, 0.08f, 0.55f);
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
                    // Food piles inside this frame; total count remains server-owned.
                    pose.translate(x + right.getStepX() * (n - 1.5) * 0.034
                                      + back.getStepX() * n * 0.048,
                                   1.49 + n * 0.012,
                                   z + right.getStepZ() * (n - 1.5) * 0.034
                                     + back.getStepZ() * n * 0.048);
                    pose.mulPose(Axis.YP.rotationDegrees(angle));
                    pose.mulPose(Axis.XP.rotationDegrees(66.0f));
                    pose.mulPose(Axis.ZP.rotationDegrees((n % 3 - 1) * 8.0f));
                    pose.scale(0.25f, 0.25f, 0.25f);
                    items.renderStatic(one, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                            pose, buffers, cart.getLevel(), slot * 4 + n);
                    pose.popPose();
                }
            }
        }
    }
}
