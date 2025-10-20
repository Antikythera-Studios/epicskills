package com.yesman.epicskills.registry.entry;

import com.mojang.serialization.MapCodec;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.data.loot.AbilityStoneLootModifier;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EpicSkillsGlobalLootModifer {
	private EpicSkillsGlobalLootModifer() {}
	
	public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EpicSkills.MODID);
	
	public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AbilityStoneLootModifier>> ABILITY_STONE = GLOBAL_LOOT_LOOT_MODIFIERS.register("abiliity_stone_loot_modifier", () -> AbilityStoneLootModifier.CODEC);
}
