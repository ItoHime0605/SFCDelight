package tw.saltedfriedchicken.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/** Physical-client-only render registration. No client classes load on dedicated servers. */
@EventBusSubscriber(modid = SaltedFriedChickenDelight.MOD_ID, value = Dist.CLIENT)
public final class ClientBlockEntityRenderers {
    private ClientBlockEntityRenderers() {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SaltedFriedChickenDelight.FRYING_BASKET_ENTITY.get(),
                FryingBasketContentsRenderer::new);
        event.registerBlockEntityRenderer(SaltedFriedChickenDelight.CART_ENTITY.get(),
                FriedFoodCartContentsRenderer::new);
        event.registerBlockEntityRenderer(SaltedFriedChickenDelight.DEOILER_ENTITY.get(),
                DeOilerContentsRenderer::new);
        event.registerBlockEntityRenderer(SaltedFriedChickenDelight.PREPARATION_COUNTER_ENTITY.get(),
                PreparationCounterContentsRenderer::new);
    }
}
