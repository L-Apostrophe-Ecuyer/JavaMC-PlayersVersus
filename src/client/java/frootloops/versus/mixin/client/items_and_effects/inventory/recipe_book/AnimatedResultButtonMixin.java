package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.RecipeButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static frootloops.versus.VersusModClient.*;


@Environment(EnvType.CLIENT)
@Mixin(RecipeButton.class)
public abstract class AnimatedResultButtonMixin extends AbstractWidget {
    @Shadow private static final ResourceLocation SLOT_MANY_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_craftable");
    @Shadow private static final ResourceLocation SLOT_CRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    @Shadow private static final ResourceLocation SLOT_MANY_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_many_uncraftable");
    @Shadow private static final ResourceLocation SLOT_UNCRAFTABLE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    @Shadow private float animationTime;
    @Shadow private RecipeCollection collection;
    @Shadow private boolean hasMultipleRecipes() {
        return true;
    }

    public AnimatedResultButtonMixin(int x, int y, int width, int height, Component message, RecipeCollection resultCollection) {
        super(x, y, width, height, message);
        this.collection = resultCollection;
    }


    @Shadow
    public ItemStack getDisplayStack() {
        return ItemStack.EMPTY;
    }


    @Overwrite
    public void renderWidget(GuiGraphics context, int mouseX, int mouseY, float delta) {

        // FIRST STEP -----------------
        // Draw background:
        ResourceLocation identifier;
        boolean isGroupOfRecipes = this.hasMultipleRecipes();
        boolean isCraftable = this.collection.hasCraftable();
        if (isCraftable) {
            identifier = isGroupOfRecipes ? SLOT_MANY_CRAFTABLE_SPRITE : SLOT_CRAFTABLE_SPRITE;
        } else {
            identifier = isGroupOfRecipes ? SLOT_MANY_UNCRAFTABLE_SPRITE : SLOT_UNCRAFTABLE_SPRITE;
        }
        context.blitSprite(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);

        // SECOND STEP ---------------
        // Determine bounce (whatever that is):
        boolean hasBounce = this.animationTime > 0.0F;
        if (hasBounce) {
            float f = 1.0F + 0.1F * (float)Math.sin((double)(this.animationTime / 15.0F * 3.1415927F));
            context.pose().pushMatrix();
            context.pose().translate((float)(this.getX() + 8), (float)(this.getY() + 12));
            context.pose().scale(f, f);
            context.pose().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)));
            this.animationTime -= delta;
        }

        // THIRD STEP ----------------
        // Draw item or group:
        //Recipe currentRecipe = this.currentRecipe().value();
        if(isGroupOfRecipes) {
            int offset = 4;
            /*
            if(!this.resultCollection.hasSingleOutput() && resultCollection.getAllRecipes().size() > 1){
                context.drawItem(resultCollection.getAllRecipes().get(0).display().result().getFirst(null), this.getX() + 2, this.getY() + 2, 0, 10);
                Identifier firstOverlayTextureID = isCraftable ? RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY : RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY;
                context.drawGuiTexture(RenderLayer::getGuiTexturedOverlay,firstOverlayTextureID, this.getX(), this.getY(), this.width, this.height);
                offset = 6;
            }*/
            ItemStack itemStack = ((RecipeButton)((Object)this)).getDisplayStack();
            context.renderItem(itemStack, this.getX() + offset, this.getY() + offset, 0);
            ResourceLocation overlayTextureID = isCraftable ? RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY : RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY;
            context.blitSprite(RenderPipelines.GUI_TEXTURED, overlayTextureID, this.getX(), this.getY(), this.width, this.height);

        }
        else {
            ItemStack itemStack = ((RecipeButton)((Object)this)).getDisplayStack();
            context.renderFakeItem(itemStack, this.getX() + 4, this.getY() + 4);
            if(!isCraftable) context.blitSprite(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_CRAFTABLE_SINGLE_OVERLAY, this.getX(), this.getY(), this.width, this.height);
        }

        if (hasBounce) context.pose().popMatrix();
    }
}
