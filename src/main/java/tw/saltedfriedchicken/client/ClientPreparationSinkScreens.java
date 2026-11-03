package tw.saltedfriedchicken.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import tw.saltedfriedchicken.SaltedFriedChickenDelight;

/** Run only on the physical client, never on a dedicated server. */
@EventBusSubscriber(modid = SaltedFriedChickenDelight.MOD_ID, value = Dist.CLIENT)
public final class ClientPreparationSinkScreens {
    private ClientPreparationSinkScreens() {}

    @SubscribeEvent
    public static void register(RegisterMenuScreensEvent event) {
        event.register(SaltedFriedChickenDelight.PREPARATION_SINK_MENU.get(), PreparationSinkScreen::new);
        event.register(SaltedFriedChickenDelight.EQUIPMENT_STATUS_MENU.get(), EquipmentStatusScreen::new);
    }
}
