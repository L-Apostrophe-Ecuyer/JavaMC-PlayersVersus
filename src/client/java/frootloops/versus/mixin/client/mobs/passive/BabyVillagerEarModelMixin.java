package frootloops.versus.mixin.client.mobs.passive;

import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import frootloops.versus.mod.mobs.VillagerEarParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BabyVillagerModel.class)
public abstract class BabyVillagerEarModelMixin {

    @Inject(method = "createBodyModel", at = @At("RETURN"), cancellable = true)
    private static void addEars(CallbackInfoReturnable<MeshDefinition> cir) {
        MeshDefinition modelData = cir.getReturnValue();
        VillagerEarParts.addTo(modelData);
        cir.setReturnValue(modelData);
    }
}
