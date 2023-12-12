package frootloops.versus.mixin.mobs.hostile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEntity.class)
public abstract class MobEquipmentMixin extends Entity {

    public MobEquipmentMixin(EntityType<?> type, World world, DefaultedList<ItemStack> handItems, DefaultedList<ItemStack> armorItems, float[] handDropChances, float[] armorDropChances) {
        super(type, world);
    }

    @Redirect(at=@At(value = "INVOKE", target="Lnet/minecraft/world/LocalDifficulty;getClampedLocalDifficulty()F"), method= "Lnet/minecraft/entity/mob/MobEntity;initEquipment(Lnet/minecraft/util/math/random/Random;Lnet/minecraft/world/LocalDifficulty;)V")
    private float harderFartherAndDeeper(LocalDifficulty localDifficulty) {
        float distanceMultiplier = ((float)(this.getBlockPos().getX() - this.getWorld().getSpawnPos().getX()))/512f + ((float)(this.getBlockPos().getZ() - this.getWorld().getSpawnPos().getZ()))/512f;
        float depthBonus = 160.0f/Math.abs((float)this.getPos().y - 64f);
        return (localDifficulty.getClampedLocalDifficulty() + depthBonus) * distanceMultiplier;
    }

}
