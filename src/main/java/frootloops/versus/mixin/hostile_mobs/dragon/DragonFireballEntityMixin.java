package frootloops.versus.mixin.hostile_mobs.dragon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonFireballEntity.class)
public class DragonFireballEntityMixin extends ExplosiveProjectileEntity {
	protected DragonFireballEntityMixin(EntityType<? extends ExplosiveProjectileEntity> p_i50173_1_, World p_i50173_2_) {
		super(p_i50173_1_, p_i50173_2_);
	}

	@Override
	public boolean isAttackable(){
		return true;
	}

	@Override
	public boolean handleAttack(Entity attacker) {
		return false;
	}

	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	public void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {

		this.scheduleVelocityUpdate();
		Entity entity = source.getAttacker();

		if (entity != null) {
			if (!this.world.isClient) {
				Vec3d vec3d = entity.getRotationVector();
				this.setVelocity(vec3d);
				this.powerX = vec3d.x * 0.1;
				this.powerY = vec3d.y * 0.1;
				this.powerZ = vec3d.z * 0.1;
				this.setOwner(entity);
			}
			cir.setReturnValue(true);
		}
		cir.setReturnValue(false);
	}
}