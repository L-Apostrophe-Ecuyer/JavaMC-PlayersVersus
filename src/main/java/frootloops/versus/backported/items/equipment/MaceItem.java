package frootloops.versus.backported.items.equipment;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public class MaceItem extends MiningToolItem {


    public MaceItem(ToolMaterial material, float attackDamage, float attackSpeed, Item.Settings settings) {
        super(attackDamage, attackSpeed, material, BlockTags.AXE_MINEABLE, settings);
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return state.getSoundGroup() == BlockSoundGroup.GLASS || state.isIn(BlockTags.SHOVEL_MINEABLE);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        if (state.isIn(BlockTags.PICKAXE_MINEABLE) || state.isIn(BlockTags.SHOVEL_MINEABLE)) {
            return 6.0f;
        }
        if (state.getSoundGroup() == BlockSoundGroup.GLASS) {
            return 24.0f;
        }
        return 0.0f;
    }
}
