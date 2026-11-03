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
import tw.saltedfriedchicken.PreparationCounterBlock;
import tw.saltedfriedchicken.PreparationCounterBlockEntity;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/**
 * Left-hand prep surface: only the real bench inventory is visible.
 * A marination box excludes a bowl; bowl, marinated food and flour are shown
 * independently while the player performs the three manual coating clicks.
 * Render-only and fully client-side: no extra entities or server timer.
 */
public final class PreparationCounterContentsRenderer implements BlockEntityRenderer<PreparationCounterBlockEntity> {
    private final ItemRenderer items;

    public PreparationCounterContentsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    private void draw(PreparationCounterBlockEntity bench, ItemStack stack, PoseStack pose,
                      MultiBufferSource buffers, int light, int overlay, float angle,
                      double x, double y, double z, float size, float tilt, int seed) {
        if (stack.isEmpty()) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.mulPose(Axis.XP.rotationDegrees(tilt));
        pose.scale(size, size, size);
        items.renderStatic(stack.copyWithCount(1), ItemDisplayContext.FIXED,
                light, overlay, pose, buffers, bench.getLevel(), seed);
        pose.popPose();
    }

    @Override
    public void render(PreparationCounterBlockEntity bench, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (bench.getLevel() == null
                || bench.getBlockState().getValue(PreparationCounterBlock.PART) != 0) return;

        Direction front = bench.getBlockState().getValue(PreparationCounterBlock.FACING);
        float angle = switch (front) {
            case NORTH -> 0.0f;
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
        ItemStack marinade = bench.displayedMarination();
        ItemStack bowl = bench.displayedBowl();
        // The real 3D bowl and marinade box sit above the 14.2px-high worktop.
        if (!marinade.isEmpty()) {
            draw(bench, marinade, pose, buffers, packedLight, packedOverlay,
                    angle, 0.5, 0.98, 0.5, 0.58f, 0.0f, 1100);
            return;
        }
        if (bowl.isEmpty()) return;
        draw(bench, bowl, pose, buffers, packedLight, packedOverlay,
                angle, 0.5, 0.965, 0.5, 0.72f, 0.0f, 1101);

        ItemStack ingredient = bench.displayedIngredient();
        draw(bench, ingredient, pose, buffers, packedLight, packedOverlay,
                angle, 0.48, 1.06, 0.5, 0.25f, 74.0f, 1102);
        if (bench.displayedFlour()) {
            draw(bench, new ItemStack(SaltedFriedChickenDelight.WHEAT_FLOUR.get()),
                    pose, buffers, packedLight, packedOverlay,
                    angle, 0.62, 1.052, 0.53, 0.19f, 75.0f, 1103);
        }
    }
}
