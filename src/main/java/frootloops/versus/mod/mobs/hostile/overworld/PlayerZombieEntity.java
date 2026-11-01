package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A player the Blight took: it rises where they died blighted, wearing what they wore and holding what they held,
 * carrying the rest of what they had on them, in their skin (its colour drawn greener and duller: PlayerZombieRenderer)
 * and their name. Killed, it drops it all (a long while before it despawns). It never turns into anything else (no
 * drowning into a drowned, no pale), doesn't burn in the sun, stays where it rose, and its hits blight as the pale's do.
 * With keepInventory, or as a creative player's copy, it wears copies and drops nothing.
 */
public class PlayerZombieEntity extends Zombie {
    private static final EntityDataAccessor<ResolvableProfile> DATA_PROFILE = SynchedEntityData.defineId(PlayerZombieEntity.class, EntityDataSerializers.RESOLVABLE_PROFILE);
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    /** What it carries besides what it wears and holds. */
    private final List<ItemStack> stash = new ArrayList<>();

    public PlayerZombieEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, Mannequin.DEFAULT_PROFILE);
    }

    /** Whose skin it wears. */
    public ResolvableProfile getProfile() {
        return this.entityData.get(DATA_PROFILE);
    }

    public void setProfile(ResolvableProfile profile) {
        this.entityData.set(DATA_PROFILE, profile);
    }

    /** What it carries, besides what it wears and holds. */
    public List<ItemStack> getStash() {
        return this.stash;
    }

    /**
     * A player zombie rising where the player is: in their skin and name, in what they wear and hold, carrying the rest.
     * Their things move to it (the player's inventory is left empty, for the death that follows); with keepInventory, or
     * for a copy (a creative player's: they don't die), it wears copies, holds none and drops nothing. Vanishing cursed
     * things vanish, as they do from a dying player.
     */
    @Nullable
    public static PlayerZombieEntity rise(ServerLevel level, ServerPlayer player, boolean copy) {
        PlayerZombieEntity zombie = ModEntities.PLAYER_ZOMBIE.create(level, EntitySpawnReason.CONVERSION);
        if (zombie == null) return null;
        zombie.snapTo(player.position(), player.getYRot(), 0.0F);
        zombie.setYHeadRot(player.getYHeadRot());
        zombie.setProfile(player.getProfile());
        zombie.setCustomName(player.getName());
        zombie.setCustomNameVisible(true);
        zombie.setPersistenceRequired();
        zombie.setCanPickUpLoot(false);
        boolean keeps = copy || level.getGameRules().get(GameRules.KEEP_INVENTORY);
        for (EquipmentSlot slot : WORN) {
            ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) continue;
            if (keeps) {
                zombie.setItemSlot(slot, worn.copy());
                zombie.setDropChance(slot, 0.0F);
                continue;
            }
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (EnchantRegistryHelper.hasEnchantment(worn, Enchantments.VANISHING_CURSE)) continue;
            zombie.setItemSlot(slot, worn);
            zombie.setGuaranteedDrop(slot);
        }
        if (!keeps) {
            NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();
            for (int slot = 0; slot < items.size(); slot++) {
                ItemStack stack = items.get(slot);
                if (stack.isEmpty()) continue;
                items.set(slot, ItemStack.EMPTY);
                if (!EnchantRegistryHelper.hasEnchantment(stack, Enchantments.VANISHING_CURSE)) zombie.stash.add(stack);
            }
        }
        level.addFreshEntity(zombie);
        return zombie;
    }

    /** It drops what it wore and held (guaranteed), and what it carried. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (ItemStack stack : this.stash) {
            ItemEntity item = this.spawnAtLocation(level, stack);
            if (item != null) item.setExtendedLifetime();
        }
        this.stash.clear();
    }

    /** The dead are no less dead in the sun, but this one is someone's grave: it stays. */
    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        super.addAdditionalSaveData(view);
        view.store("Profile", ResolvableProfile.CODEC, this.getProfile());
        ValueOutput.TypedOutputList<ItemStack> list = view.list("Stash", ItemStack.CODEC);
        for (ItemStack stack : this.stash) list.add(stack);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        super.readAdditionalSaveData(view);
        view.read("Profile", ResolvableProfile.CODEC).ifPresent(this::setProfile);
        this.stash.clear();
        for (ItemStack stack : view.listOrEmpty("Stash", ItemStack.CODEC)) this.stash.add(stack);
    }
}
