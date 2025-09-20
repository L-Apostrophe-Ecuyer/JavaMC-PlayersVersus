package frootloops.versus.mixin.client.items_and_effects.inventory.recipe_book;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.AnimatedResultButton;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

import static frootloops.versus.VersusModClient.*;


@Environment(EnvType.CLIENT)
@Mixin(AnimatedResultButton.class)
public abstract class AnimatedResultButtonMixin extends ClickableWidget {
    @Shadow private static final Identifier SLOT_MANY_CRAFTABLE_TEXTURE = Identifier.ofVanilla("recipe_book/slot_many_craftable");
    @Shadow private static final Identifier SLOT_CRAFTABLE_TEXTURE = Identifier.ofVanilla("recipe_book/slot_craftable");
    @Shadow private static final Identifier SLOT_MANY_UNCRAFTABLE_TEXTURE = Identifier.ofVanilla("recipe_book/slot_many_uncraftable");
    @Shadow private static final Identifier SLOT_UNCRAFTABLE_TEXTURE = Identifier.ofVanilla("recipe_book/slot_uncraftable");
    @Shadow private float bounce;
    @Shadow private RecipeResultCollection resultCollection;
    @Shadow private boolean hasMultipleResults() {
        return true;
    }

    public AnimatedResultButtonMixin(int x, int y, int width, int height, Text message, RecipeResultCollection resultCollection) {
        super(x, y, width, height, message);
        this.resultCollection = resultCollection;
    }


    @Shadow
    public ItemStack getDisplayStack() {
        return ItemStack.EMPTY;
    }


    @Overwrite
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

        // FIRST STEP -----------------
        // Draw background:
        Identifier identifier;
        boolean isGroupOfRecipes = this.hasMultipleResults();
        boolean isCraftable = this.resultCollection.hasCraftableRecipes();
        if (isCraftable) {
            identifier = isGroupOfRecipes ? SLOT_MANY_CRAFTABLE_TEXTURE : SLOT_CRAFTABLE_TEXTURE;
        } else {
            identifier = isGroupOfRecipes ? SLOT_MANY_UNCRAFTABLE_TEXTURE : SLOT_UNCRAFTABLE_TEXTURE;
        }
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, identifier, this.getX(), this.getY(), this.width, this.height);

        // SECOND STEP ---------------
        // Determine bounce (whatever that is):
        boolean hasBounce = this.bounce > 0.0F;
        if (hasBounce) {
            float f = 1.0F + 0.1F * (float)Math.sin((double)(this.bounce / 15.0F * 3.1415927F));
            context.getMatrices().pushMatrix();
            context.getMatrices().translate((float)(this.getX() + 8), (float)(this.getY() + 12));
            context.getMatrices().scale(f, f);
            context.getMatrices().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)));
            this.bounce -= delta;
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
            ItemStack itemStack = ((AnimatedResultButton)((Object)this)).getDisplayStack();
            context.drawItem(itemStack, this.getX() + offset, this.getY() + offset, 0);
            Identifier overlayTextureID = isCraftable ? RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY : RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY;
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, overlayTextureID, this.getX(), this.getY(), this.width, this.height);

        }
        else {
            ItemStack itemStack = ((AnimatedResultButton)((Object)this)).getDisplayStack();
            context.drawItemWithoutEntity(itemStack, this.getX() + 4, this.getY() + 4);
            if(!isCraftable) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, RECIPE_BOOK_CRAFTABLE_SINGLE_OVERLAY, this.getX(), this.getY(), this.width, this.height);
        }

        if (hasBounce) context.getMatrices().popMatrix();
    }
}
