package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.world.item.AbilityStoneItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EpicSkillsItems {
	private EpicSkillsItems() {}
	
	public static final DeferredRegister<Item> REGISTRY = DeferredRegister.create(Registries.ITEM, EpicSkills.MODID);
	
	public static final DeferredHolder<Item, AbilityStoneItem> ABILIITY_STONE = REGISTRY.register("ability_stone", () -> {
		return new AbilityStoneItem(new Item.Properties().rarity(Rarity.RARE));
	});
}
