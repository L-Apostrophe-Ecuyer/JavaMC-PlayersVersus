package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

public class FrostedZombieRenderer extends ZombieEntityRenderer {

    public FrostedZombieRenderer(EntityRendererFactory.Context context) {
        super(context, EntityModelLayers.ZOMBIE, EntityModelLayers.ZOMBIE_INNER_ARMOR, EntityModelLayers.ZOMBIE_OUTER_ARMOR);
    }

    @Override
    public Identifier getTexture(ZombieEntity zombieEntity) {
        return new Identifier(VersusMod.MOD_ID  + ":textures/entity/frosted_zombie.png");
    }
}
