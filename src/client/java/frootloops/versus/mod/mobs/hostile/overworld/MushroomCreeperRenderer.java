package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.block.Blocks;

/** A mushroom creeper: a creeper the red mushroom took, red and white, with red mushrooms growing out of its head. */
@Environment(EnvType.CLIENT)
public class MushroomCreeperRenderer extends CreeperRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/mushroom_creeper.png");
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    public MushroomCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        // The head's top is half a block above its pivot; the mushroom's block is a block tall, its mushroom the lower half
        this.addLayer(new RedMushroomsLayer<>(this, "head",
                new RedMushroomsLayer.Spot(-0.12F, -1.0F, 0.05F, 30.0F, 1.0F),
                new RedMushroomsLayer.Spot(0.16F, -0.85F, -0.1F, -50.0F, 0.7F)));
    }

    @Override
    public CreeperRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(Creeper creeper, CreeperRenderState state, float partialTick) {
        super.extractRenderState(creeper, state, partialTick);
        if (state instanceof State holder) this.blockModelResolver.update(holder.mushroom, Blocks.RED_MUSHROOM.defaultBlockState(), DISPLAY);
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        return TEXTURE;
    }

    static class State extends CreeperRenderState implements RedMushroomsLayer.Holder {
        final BlockModelRenderState mushroom = new BlockModelRenderState();

        @Override
        public BlockModelRenderState redMushroom() {
            return this.mushroom;
        }
    }
}
