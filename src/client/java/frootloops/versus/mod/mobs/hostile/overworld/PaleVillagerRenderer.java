package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.npc.BabyVillagerModel;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;
import net.minecraft.resources.Identifier;

/**
 * A pale villager: the villager's model in the villager's own clothes (vanilla's type and profession textures, drawn as
 * a villager's are), over its skin gone white (tools/pale-villager.py). It shakes while it's being cured.
 */
@Environment(EnvType.CLIENT)
public class PaleVillagerRenderer extends AgeableMobRenderer<PaleVillagerEntity, VillagerRenderState, VillagerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_villager.png");
    private static final Identifier BABY_TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/pale_villager_baby.png");

    public PaleVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER)), new BabyVillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY)), 0.5F);
        this.addLayer(new VillagerProfessionLayer<>(this, context.getResourceManager(), "villager",
                new VillagerModel(context.bakeLayer(ModelLayers.VILLAGER_NO_HAT)), new BabyVillagerModel(context.bakeLayer(ModelLayers.VILLAGER_BABY_NO_HAT))));
    }

    @Override
    public VillagerRenderState createRenderState() {
        return new VillagerRenderState();
    }

    @Override
    public void extractRenderState(PaleVillagerEntity entity, VillagerRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.villagerData = entity.getVillagerData();
        state.isUnhappy = false;
        // Being cured, it shakes as a zombie villager does (isShaking reads this)
        state.isFullyFrozen |= entity.isConverting();
    }

    @Override
    public Identifier getTextureLocation(VillagerRenderState state) {
        return state.isBaby ? BABY_TEXTURE : TEXTURE;
    }
}
