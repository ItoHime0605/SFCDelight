package tw.saltedfriedchicken;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Food-preparation effects use vanilla attributes only, without entity AI ticks. */
public final class WorkshopEffect extends MobEffect {
    public WorkshopEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
