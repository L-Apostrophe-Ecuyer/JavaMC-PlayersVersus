package frootloops.versus.mixin.client.items.inventory.recipe_book;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.recipebook.AnimatedResultButton;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
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
    @Shadow private int currentResultIndex;
    @Shadow private AbstractRecipeScreenHandler<?, ?> craftingScreenHandler;
    @Shadow private RecipeBook recipeBook;

    @Shadow private float time;

    @Shadow
    private List<RecipeEntry<?>> getResults() {
        List<RecipeEntry<?>> list = this.resultCollection.getRecipes(true);
        if (!this.recipeBook.isFilteringCraftable(this.craftingScreenHandler)) {
            list.addAll(this.resultCollection.getRecipes(false));
        }
        return list;
    }

    public AnimatedResultButtonMixin(int x, int y, int width, int height, Text message, RecipeResultCollection resultCollection) {
        super(x, y, width, height, message);
        this.resultCollection = resultCollection;
    }

    @Overwrite
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {

        // FIRST STEP -----------------
        // Draw background:
        Identifier identifier;
        List<RecipeEntry<?>> list = this.getResults();
        boolean isGroupOfRecipes = list.size() > 1;
        boolean isCraftable = this.resultCollection.hasCraftableRecipes();
        if (isCraftable) {
            identifier = isGroupOfRecipes ? SLOT_MANY_CRAFTABLE_TEXTURE : SLOT_CRAFTABLE_TEXTURE;
        } else {
            identifier = isGroupOfRecipes ? SLOT_MANY_UNCRAFTABLE_TEXTURE : SLOT_UNCRAFTABLE_TEXTURE;
        }
        context.drawGuiTexture(identifier, this.getX(), this.getY(), this.width, this.height);

        // SECOND STEP ---------------
        // Determine bounce (whatever that is):
        boolean hasBounce = this.bounce > 0.0F;
        if (hasBounce) {
            float f = 1.0F + 0.1F * (float)Math.sin((double)(this.bounce / 15.0F * 3.1415927F));
            context.getMatrices().push();
            context.getMatrices().translate((float)(this.getX() + 8), (float)(this.getY() + 12), 0.0F);
            context.getMatrices().scale(f, f, 1.0F);
            context.getMatrices().translate((float)(-(this.getX() + 8)), (float)(-(this.getY() + 12)), 0.0F);
            this.bounce -= delta;
        }

        // THIRD STEP ----------------
        // Draw item or group:
        if(isGroupOfRecipes) {

            this.currentResultIndex = MathHelper.floor((float)(this.time / 30.0f)) % list.size();
            Recipe currentRecipe = list.get(this.currentResultIndex).value();
            ItemStack itemStack = currentRecipe.getResult((RegistryWrapper.WrapperLookup)this.resultCollection.getRegistryManager());

            int offset = 4;
            if(!this.resultCollection.hasSingleOutput()){
                String recipeGroup = currentRecipe.getGroup();
                if(recipeGroup.endsWith("copper_waxing"))  {
                    context.drawItem(Items.HONEYCOMB.getDefaultStack(), this.getX() + 5, this.getY() + 5, 0, 10);
                }
                else {
                    int nextRecipeIndex = (this.currentResultIndex + 1) % list.size();
                    Recipe nextRecipe = ((RecipeEntry) list.get(nextRecipeIndex)).value();
                    ItemStack nextItemStack = nextRecipe.getResult((RegistryWrapper.WrapperLookup)this.resultCollection.getRegistryManager());
                    context.drawItem(nextItemStack, this.getX() + 5, this.getY() + 5, 0, 10);
                }
                offset = 2;
            }
            context.drawItemWithoutEntity(itemStack, this.getX() + offset, this.getY() + offset);
            Identifier overlayTextureID = isCraftable ? RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY : RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY;
            context.drawGuiTexture(overlayTextureID, this.getX(), this.getY(), this.width, this.height);

        }
        else {
            ItemStack itemStack =  list.get(0).value().getResult(this.resultCollection.getRegistryManager());
            context.drawItemWithoutEntity(itemStack, this.getX() + 4, this.getY() + 4);
            if(!isCraftable) context.drawGuiTexture(RECIPE_BOOK_CRAFTABLE_SINGLE_OVERLAY, this.getX(), this.getY(), this.width, this.height);
        }


        if (hasBounce) {
            context.getMatrices().pop();
        }
    }
}
