package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;

/** A mushroom spider: a spider the red mushroom took, red and white, its eyes a spider's, red mushrooms on its back. */
@Environment(EnvType.CLIENT)
public class MushroomSpiderRenderer extends SpiderRenderer<MushroomSpiderEntity> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/mushroom_spider.png");
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;

    public MushroomSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockModelResolver = context.getBlockModelResolver();
        // The abdomen ("body1") is half a block tall about its pivot: its top a quarter block up
        this.addLayer(new RedMushroomsLayer<>(this, "body1",
                new RedMushroomsLayer.Spot(-0.15F, -0.75F, -0.1F, 20.0F, 0.9F),
                new RedMushroomsLayer.Spot(0.17F, -0.7F, 0.15F, -40.0F, 0.7F),
                new RedMushroomsLayer.Spot(0.0F, -0.65F, 0.3F, 70.0F, 0.55F)));
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MushroomSpiderEntity spider, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(spider, state, partialTick);
        if (state instanceof State holder) this.blockModelResolver.update(holder.mushroom, Blocks.RED_MUSHROOM.defaultBlockState(), DISPLAY);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    static class State extends LivingEntityRenderState implements RedMushroomsLayer.Holder {
        final BlockModelRenderState mushroom = new BlockModelRenderState();

        @Override
        public BlockModelRenderState redMushroom() {
            return this.mushroom;
        }
    }
}
