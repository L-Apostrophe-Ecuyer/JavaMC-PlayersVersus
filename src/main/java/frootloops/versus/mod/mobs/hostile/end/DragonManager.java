package frootloops.versus.mod.mobs.hostile.end;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class DragonManager {

	public static final double chargePlayerMaxChance = 0.3;
	public static final double fireballMaxChance = 0.3;

	public static boolean onPhaseEnd(EnderDragon dragon) {
		boolean chargePlayer = shouldChargePlayer(dragon);
		boolean fireballPlayer = shouldFireballPlayer(dragon);

		if (fireballPlayer)
			fireballPlayer(dragon);
		else if (chargePlayer)
			chargePlayer(dragon);

		return chargePlayer || fireballPlayer;
	}

	private static boolean shouldChargePlayer(EnderDragon dragon) {
		if (dragon.getDragonFight() == null)
			return false;

		double chance = chargePlayerMaxChance;

		BlockPos centerPodium = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(new BlockPos(0,0,0)));
		AABB boundingBox = new AABB(centerPodium).inflate(64d);
		List<Player> players = dragon.level().getEntitiesOfClass(Player.class, boundingBox, EntitySelector.NO_CREATIVE_OR_SPECTATOR);

		for (Player player : players) {
			List<EndCrystal> endCrystals = player.level().getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(10d));
			if (endCrystals.size() > 0) {
				chance *= 2d;
				break;
			}
		}
		double rng = dragon.getRandom().nextDouble();
		return rng < chance;
	}

	private static void chargePlayer(EnderDragon dragon) {
		BlockPos centerPodium = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(new BlockPos(0,0,0)));
		AABB bb = new AABB(centerPodium).inflate(64d);
		ServerPlayer player = (ServerPlayer) getRandomPlayerNearCrystal(dragon.level(), bb);

		if (player == null)
			return;

		dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
		Vec3 targetPos = player.position();
		if (targetPos.y < dragon.getY())
			targetPos = targetPos.add(0d, -5d, 0d);
		else
			targetPos = targetPos.add(0d, 6d, 0d);
		dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(targetPos);
	}

	private static boolean shouldFireballPlayer(EnderDragon dragon) {
		return dragon.getRandom().nextDouble() < fireballMaxChance;
	}

	private static void fireballPlayer(EnderDragon dragon) {
		BlockPos centerPodium = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(new BlockPos(0,0,0)));
		AABB bb = new AABB(centerPodium).inflate(64d);

		ServerPlayer player = (ServerPlayer) getRandomPlayer(dragon.level(), bb);
		if (player == null) return;

		dragon.getPhaseManager().setPhase(EnderDragonPhase.STRAFE_PLAYER);
		dragon.getPhaseManager().getPhase(EnderDragonPhase.STRAFE_PLAYER).setTarget(player);
	}


	public static void fireFireball(EnderDragon dragon, LivingEntity attackTarget) {
		Vec3 vector3d2 = dragon.getViewVector(1.0F);
		double x = dragon.head.getX() - vector3d2.x;
		double y = dragon.head.getY(0.5D) + 0.5D;
		double z = dragon.head.getZ() - vector3d2.z;
		double xPower = attackTarget.getX() - x;
		double yPower = attackTarget.getY(0.5D) - y;
		double zPower = attackTarget.getZ() - z;
		if (!dragon.isSilent()) {
			dragon.level().levelEvent(null, 1017, dragon.blockPosition(), 0);
		}

		DragonFireball dragonfireballentity = new DragonFireball(dragon.level(), dragon, new Vec3(xPower, yPower, zPower));
		dragonfireballentity.snapTo(x, y, z, 0.0F, 0.0F);
		dragon.level().addFreshEntity(dragonfireballentity);

		double numFireballs = 3.0D;

		for (int i = 0; i < (int)numFireballs; i++) {
			x = dragon.head.getX() - vector3d2.x;
			y = dragon.head.getY(0.5D) + 0.5D;
			z = dragon.head.getZ() - vector3d2.z;

			double randomOffset = dragon.getRandom().nextDouble() * (numFireballs * numFireballs) - numFireballs;
			xPower = attackTarget.getX() - x + randomOffset;
			yPower = attackTarget.getY(0.5D) - y + randomOffset;
			zPower = attackTarget.getZ() - z + randomOffset;
			if (!dragon.isSilent()) {
				dragon.level().levelEvent(null, 1017, dragon.blockPosition(), 0);
			}

			dragonfireballentity = new DragonFireball(dragon.level(), dragon, new Vec3(xPower, yPower, zPower));
			dragonfireballentity.snapTo(x, y, z, 0.0F, 0.0F);
			dragon.level().addFreshEntity(dragonfireballentity);
		}
	}

	@Nullable
	public static Player getRandomPlayer(Level world, AABB boundingBox) {
		List<Player> players = world.getEntitiesOfClass(Player.class, boundingBox, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
		if (players.isEmpty())
			return null;

		int r = world.random.nextInt(players.size());
		return players.get(r);
	}

	//Returns a random player that is at least 10 blocks near a Crystal or a random player if no players are near crystals
	@Nullable
	public static Player getRandomPlayerNearCrystal(Level world, AABB boundingBox) {
		List<Player> players = world.getEntitiesOfClass(Player.class, boundingBox);
		if (players.isEmpty())
			return null;

		List<Player> playersNearCrystals = new ArrayList<>();

 		for (Player player : players) {
			List<EndCrystal> endCrystals = player.level().getEntitiesOfClass(EndCrystal.class, player.getBoundingBox().inflate(10d), EntitySelector.NO_CREATIVE_OR_SPECTATOR);
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