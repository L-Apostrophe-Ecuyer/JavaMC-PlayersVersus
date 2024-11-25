package frootloops.versus.mixin.client.mobs;

import net.minecraft.client.render.entity.model.BabyModelTransformer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.ModelTransformer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;


@Mixin(BipedEntityModel.class)
public class BipedalModelMixin {

    @Shadow
    public static final ModelTransformer BABY_TRANSFORMER = new BabyModelTransformer(false, 16.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head"));

}
