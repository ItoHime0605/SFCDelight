package tw.saltedfriedchicken.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import tw.saltedfriedchicken.DeOilerBlockEntity;
import tw.saltedfriedchicken.FriedBatchData;

/**
 * Displays real food portions in the stainless-steel de-oiler.
 * Animation uses local render time only; the server owns food and 8-second progress.
 * No per-tick network messages, spawned item entities, or idle particles.
 */
public final class DeOilerContentsRenderer implements BlockEntityRenderer<DeOilerBlockEntity> {
    private final ItemRenderer items;

    public DeOilerContentsRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(DeOilerBlockEntity machine, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (machine.getLevel() == null || machine.isEmpty()) return;
        ItemStack stored = machine.contentsForDisplay();
        List<ItemStack> foods = FriedBatchData.readAll(stored, machine.getLevel().registryAccess());
        if (foods.isEmpty()) return;

        // Five evenly spaced portions per ring, at most ten pieces across two
        // layers. Once the real eight-second cycle finishes, the tray stops.
        boolean spinning = !machine.isComplete();
        float rotation = spinning ? (machine.getLevel().getGameTime() + partialTick) * 16.0f : 0.0f;
        int pieceIndex = 0;
        for (ItemStack stack : foods) {
            if (stack.isEmpty()) continue;
            ItemStack one = stack.copyWithCount(1);
            for (int count = 0; count < stack.getCount() && pieceIndex < 10; count++) {
                int ringPosition = pieceIndex % 5;
                int layer = pieceIndex / 5;
                pose.pushPose();
                // This raised wire-tray display sits just above the 13px-high
                // machine casing, so the food can be seen without clipping.
                pose.translate(0.5, 0.84 + layer * 0.053, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(rotation));
                float phase = (float)(Math.PI * 2.0 * ringPosition / 5.0);
                pose.translate(Math.cos(phase) * 0.17, 0,
                               Math.sin(phase) * 0.17);
                pose.mulPose(Axis.YP.rotationDegrees(ringPosition * 72.0f));
                pose.mulPose(Axis.XP.rotationDegrees(18.0f));
                pose.scale(0.15f, 0.15f, 0.15f);
                items.renderStatic(one, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                        pose, buffers, machine.getLevel(), 900 + pieceIndex);
                pose.popPose();
                pieceIndex++;
            }
            if (pieceIndex >= 10) break;
        }
    }
}
