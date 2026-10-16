package frootloops.versus.mod.mobs.melee;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.ModEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityTypes;

/**
 * Angry brows over a mob's face while it winds up a swing and just after ({@link MeleeAnimation}): the mob's model drawn
 * again with a texture that is clear but for a few dark pixels above the eyes. One texture fits every skin laid out like
 * the mob's own (texture packs included); faces under an outer layer (drowned, frostbites, strays, bogged) go
 * without.
 */
@Environment(EnvType.CLIENT)
public class AngryBrowsLayer<S extends LivingEntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private static final Identifier HUMANOID = texture("humanoid");
    private static final Identifier HUMANOID_64X32 = texture("humanoid_64x32");
    private static final Identifier PIGLIN = texture("piglin");
    private static final Identifier PIGLIN_BABY = texture("piglin_baby");

    private final Identifier adult;
    private final Identifier baby;

    public AngryBrowsLayer(RenderLayerParent<S, M> renderer, Identifier adult, Identifier baby) {
        super(renderer);
        this.adult = adult;
        this.baby = baby;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (state.isInvisible || !MeleeAnimation.look(state).angry()) return;
        renderColoredCutoutModel(this.getParentModel(), state.isBaby ? this.baby : this.adult, poseStack, collector, light, state, -1, 1);
    }

    /** Adds the brows to the mobs whose faces they fit; baby zombies' faces put the eyes on the same pixels. */
    static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            if (type == EntityTypes.ZOMBIE || type == EntityTypes.HUSK || type == EntityTypes.PARCHED) {
                add(helper, renderer, HUMANOID, HUMANOID);
            } else if (type == EntityTypes.SKELETON || type == EntityTypes.WITHER_SKELETON || type == ModEntities.PALE_ZOMBIE) {
                add(helper, renderer, HUMANOID_64X32, HUMANOID_64X32);
            } else if (type == EntityTypes.PIGLIN || type == EntityTypes.PIGLIN_BRUTE || type == EntityTypes.ZOMBIFIED_PIGLIN) {
                add(helper, renderer, PIGLIN, PIGLIN_BABY);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void add(LivingEntityRenderLayerRegistrationCallback.RegistrationHelper helper, LivingEntityRenderer<?, ?, ?> renderer, Identifier adult, Identifier baby) {
        helper.register(new AngryBrowsLayer((RenderLayerParent) renderer, adult, baby));
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/angry_brows/" + name + ".png");
    }
}
