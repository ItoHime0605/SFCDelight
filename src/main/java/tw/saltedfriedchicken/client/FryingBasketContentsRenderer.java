package tw.saltedfriedchicken.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import tw.saltedfriedchicken.FryingBasketBlock;
import tw.saltedfriedchicken.FryingBasketBlockEntity;

/** Shows the actual ten or fewer raw/cooked portions inside the one metal frying basket. */
public final class FryingBasketContentsRenderer implements BlockEntityRenderer<FryingBasketBlockEntity> {
    private final ItemRenderer items;

    public FryingBasketContentsRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    /** Food descends into the fryer block below and rises above the basket block.
     * The default one-block bounds would incorrectly cull one of those poses.
     */
    @Override
    public AABB getRenderBoundingBox(FryingBasketBlockEntity basket) {
        BlockPos pos = basket.getBlockPos();
        return new AABB(pos.getX() - 0.25, pos.getY() - 1.0, pos.getZ() - 0.25,
                pos.getX() + 1.25, pos.getY() + 2.0, pos.getZ() + 1.25);
    }

    @Override
    public void render(FryingBasketBlockEntity basket, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = basket.getBlockState();
        boolean onFryer = state.getValue(FryingBasketBlock.SUBMERGED);
        // The model moves relative to its block space: raised 3..21, submerged -9..9 (model pixels).
        // Keep food at the matching height, with no duplicate model or second frying basket.
        double y = !onFryer ? 0.40
                : state.getValue(FryingBasketBlock.RAISED) ? 0.32
                : state.getValue(FryingBasketBlock.LIFTING) ? -0.07 : -0.40;
        int portion = 0;
        for (ItemStack stack : basket.ingredients()) {
            if (stack.isEmpty()) continue;
            ItemStack one = stack.copyWithCount(1);
            for (int n = 0; n < stack.getCount() && portion < FryingBasketBlockEntity.MAX_PIECES; n++) {
                int column = portion % 4;
                int row = portion / 4;
                pose.pushPose();
                pose.translate(0.305 + 0.13 * column, y + ((portion % 3) * 0.009), 0.337 + 0.165 * row);
                pose.mulPose(Axis.XP.rotationDegrees(68.0f));
                pose.mulPose(Axis.ZP.rotationDegrees((portion % 3 - 1) * 11.0f));
                pose.scale(0.15f, 0.15f, 0.15f);
                items.renderStatic(one, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                        pose, buffers, basket.getLevel(), portion);
                pose.popPose();
                portion++;
            }
            if (portion >= FryingBasketBlockEntity.MAX_PIECES) break;
        }
    }
}
