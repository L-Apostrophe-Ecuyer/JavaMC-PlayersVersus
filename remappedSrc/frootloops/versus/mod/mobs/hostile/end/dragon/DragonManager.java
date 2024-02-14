package frootloops.versus.mod.mobs.hostile.end.dragon;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jetbrains.annotations.Nullable;

public class DragonManager {

	public static final double chargePlayerMaxChance = 0.3;
	public static final double fireballMaxChance = 0.3;

	public static boolean onPhaseEnd(EnderDragonEntity dragon) {
		boolean chargePlayer = shouldChargePlayer(dragon);
		boolean fireballPlayer = shouldFireballPlayer(dragon);

		if (fireballPlayer)
			fireballPlayer(dragon);
		else if (chargePlayer)
			chargePlayer(dragon);

		return chargePlayer || fireballPlayer;
	}

	private static boolean shouldChargePlayer(EnderDragonEntity dragon) {
		if (dragon.getFight() == null)
			return false;

		double chance = chargePlayerMaxChance;

		BlockPos centerPodium = dragon.method_48926().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(new BlockPos(0,0,0)));
		Box boundingBox = new Box(centerPodium).expand(64d);
		List<PlayerEntity> players = dragon.method_48926().getEntitiesByClass(PlayerEntity.class, boundingBox, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);

		for (PlayerEntity player : players) {
			List<EndCrystalEntity> endCrystals = player.method_48926().getNonSpectatingEntities(EndCrystalEntity.class, player.getBoundingBox().expand(10d));
			if (endCrystals.size() > 0) {
				chance *= 2d;
				break;
			}
		}
		double rng = dragon.getRandom().nextDouble();
		return rng < chance;
	}

	private static void chargePlayer(EnderDragonEntity dragon) {
		BlockPos centerPodium = dragon.method_48926().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(new BlockPos(0,0,0)));
		Box bb = new Box(centerPodium).expand(64d);
		ServerPlayerEntity player = (ServerPlayerEntity) getRandomPlayerNearCrystal(dragon.method_48926(), bb);

		if (player == null)
			return;

		dragon.getPhaseManager().setPhase(PhaseType.CHARGING_PLAYER);
		Vec3d targetPos = player.getPos();
		if (targetPos.y < dragon.getY())
			targetPos = targetPos.add(0d, -5d, 0d);
		else
			targetPos = targetPos.add(0d, 6d, 0d);
		dragon.getPhaseManager().create(PhaseType.CHARGING_PLAYER).setPathTarget(targetPos);
	}

	private static boolean shouldFireballPlayer(EnderDragonEntity dragon) {
		return dragon.getRandom().nextDouble() < fireballMaxChance;
	}

	private static void fireballPlayer(EnderDragonEntity dragon) {
		BlockPos centerPodium = dragon.method_48926().getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.offsetOrigin(new BlockPos(0,0,0)));
		Box bb = new Box(centerPodium).expand(64d);

		ServerPlayerEntity player = (ServerPlayerEntity) getRandomPlayer(dragon.method_48926(), bb);
		if (player == null) return;

		dragon.getPhaseManager().setPhase(PhaseType.STRAFE_PLAYER);
		dragon.getPhaseManager().create(PhaseType.STRAFE_PLAYER).setTargetEntity(player);
	}


	public static void fireFireball(EnderDragonEntity dragon, LivingEntity attackTarget) {
		Vec3d vector3d2 = dragon.getRotationVec(1.0F);
		double x = dragon.head.getX() - vector3d2.x;
		double y = dragon.head.getBodyY(0.5D) + 0.5D;
		double z = dragon.head.getZ() - vector3d2.z;
		double xPower = attackTarget.getX() - x;
		double yPower = attackTarget.getBodyY(0.5D) - y;
		double zPower = attackTarget.getZ() - z;
		if (!dragon.isSilent()) {
			dragon.method_48926().syncWorldEvent(null, 1017, dragon.getBlockPos(), 0);
		}

		DragonFireballEntity dragonfireballentity = new DragonFireballEntity(dragon.method_48926(), dragon, xPower, yPower, zPower);
		dragonfireballentity.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
		dragon.method_48926().spawnEntity(dragonfireballentity);

		double numFireballs = 3.0D;

		for (int i = 0; i < (int)numFireballs; i++) {
			x = dragon.head.getX() - vector3d2.x;
			y = dragon.head.getBodyY(0.5D) + 0.5D;
			z = dragon.head.getZ() - vector3d2.z;

			double randomOffset = dragon.getRandom().nextDouble() * (numFireballs * numFireballs) - numFireballs;
			xPower = attackTarget.getX() - x + randomOffset;
			yPower = attackTarget.getBodyY(0.5D) - y + randomOffset;
			zPower = attackTarget.getZ() - z + randomOffset;
			if (!dragon.isSilent()) {
				dragon.method_48926().syncWorldEvent(null, 1017, dragon.getBlockPos(), 0);
			}

			dragonfireballentity = new DragonFireballEntity(dragon.method_48926(), dragon, xPower, yPower, zPower);
			dragonfireballentity.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
			dragon.method_48926().spawnEntity(dragonfireballentity);
		}
	}

	@Nullable
	public static PlayerEntity getRandomPlayer(World world, Box boundingBox) {
		List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, boundingBox, EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
		if (players.isEmpty())
			return null;

		int r = world.random.nextInt(players.size());
		return players.get(r);
	}

	//Returns a random player that is at least 10 blocks near a Crystal or a random player if no players are near crystals
	@Nullable
	public static PlayerEntity getRandomPlayerNearCrystal(World world, Box boundingBox) {
		List<PlayerEntity> players = world.getNonSpectatingEntities(PlayerEntity.class, boundingBox);
		if (players.isEmpty())
			return null;

		List<PlayerEntity> playersNearCrystals = new ArrayList<>();

 		for (PlayerEntity player : players) {
			List<EndCrystalEntity> endCrystals = player.method_48926().getEntitiesByClass(EndCrystalEntity.class, player.getBoundingBox().expand(10d), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR);
			if (endCrystals.size() > 0)
				playersNearCrystals.add(player);
		}

 		int r;
 		if (playersNearCrystals.isEmpty()) {
			r = world.random.nextInt(players.size());
			return players.get(r);
		}

		r = world.random.nextInt(playersNearCrystals.size());
		return playersNearCrystals.get(r);
	}
}