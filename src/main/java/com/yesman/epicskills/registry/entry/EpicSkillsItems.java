package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.world.item.AbilityStoneItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public abstract class EpicSkillsItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EpicSkills.MODID);
	
	public static final RegistryObject<Item> ABILIITY_STONE = ITEMS.register("ability_stone", () -> {
		return new AbilityStoneItem(new Item.Properties().rarity(Rarity.RARE));
	});
}
