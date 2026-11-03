package tw.saltedfriedchicken.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tw.saltedfriedchicken.FriedFoodCartBlockEntity;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/**
 * One-block night market cart, six plastic display frames across and three rows deep.
 * Each display shows its real stocked ingredient rather than a hard-coded texture.
 * The slanted food tiles follow the existing 18 frame positions in fried_food_cart.json.
 */
public final class FriedFoodCartContentsRenderer implements BlockEntityRenderer<FriedFoodCartBlockEntity> {
    private final ItemRenderer items;

    public FriedFoodCartContentsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(FriedFoodCartBlockEntity cart, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 6; column++) {
                if (!cart.hasFrame(row * 6 + column)) continue;
                ItemStack display = cart.getDisplayStack(row * 6 + column);
                // Actual plastic frame centers in model pixels:
                // X: 2.31 + 2.25*column; Z: 3.375 + 4.35*row; top around Y=10.45.
                double x = (2.31 + 2.25 * column) / 16.0;
                double z = (3.375 + 4.35 * row) / 16.0;
                // The small 3D plastic frame is actually stocked into this slot.
                // The cart's permanent supports remain visible even while the slot is empty.
                pose.pushPose();
                pose.translate(x, 0.673, z);
                pose.mulPose(Axis.XP.rotationDegrees(-22.5f));
                pose.scale(0.116f, 0.050f, 0.205f);
                items.renderStatic(new ItemStack(SaltedFriedChickenDelight.PLASTIC_FRAME.get()),
                        ItemDisplayContext.FIXED, packedLight, packedOverlay,
                        pose, buffers, cart.getLevel(), row * 6 + column);
                pose.popPose();
                if (display.isEmpty()) continue;
                int visible = Math.min(3, 1 + (display.getCount() - 1) / 4);
                ItemStack one = display.copyWithCount(1);
                for (int i = 0; i < visible; i++) {
                    pose.pushPose();
                    pose.translate(x + (i - (visible - 1) / 2.0) * 0.029,
                                   0.691 + i * 0.005, z + i * 0.030);
                    pose.mulPose(Axis.XP.rotationDegrees(64.0f));
                    pose.mulPose(Axis.ZP.rotationDegrees(-8.0f + i * 8.0f));
                    pose.scale(0.108f, 0.108f, 0.108f);
                    items.renderStatic(one, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                            pose, buffers, cart.getLevel(), row * 6 + column);
                    pose.popPose();
                }
            }
        }
    }
}
