package com.yesman.epicskills.registry.entry;

import com.mojang.serialization.Codec;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.data.loot.AbilityStoneLootModifier;

import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public abstract class EpicSkiillsGlobalLootModifer {
	public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> GLOBAL_LOOT_LOOT_MODIFIERS = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EpicSkills.MODID);
	
	public static final RegistryObject<Codec<? extends IGlobalLootModifier>> ABILITY_STONE = GLOBAL_LOOT_LOOT_MODIFIERS.register("abiliity_stone_loot_modifier", () -> AbilityStoneLootModifier.CODEC);
}
