package com.yesman.epicskills.world.item;

import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;
import com.yesman.epicskills.registry.entry.EpicSkillsSounds;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class AbilityStoneItem extends Item {
	public AbilityStoneItem(Properties pProperties) {
		super(pProperties);
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		
		player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoints -> {
			abilityPoints.setAbilityPoints(abilityPoints.getAbilityPoints() + 1);
			player.playSound(EpicSkillsSounds.GAIN_ABILITY_POINTS.get(), 1.0F, 1.0F);
		});
		
		if (!player.getAbilities().instabuild) {
			itemstack.shrink(1);
		}
		
		player.awardStat(Stats.ITEM_USED.get(this));
		
		return InteractionResultHolder.consume(itemstack);
	}
}
