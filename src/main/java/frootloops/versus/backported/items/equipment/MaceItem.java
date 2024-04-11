package frootloops.versus.backported.items.equipment;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.BlockSoundGroup;

public class MaceItem extends MiningToolItem {


    public MaceItem(ToolMaterial material, float attackDamage, float attackSpeed, Settings settings) {
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
