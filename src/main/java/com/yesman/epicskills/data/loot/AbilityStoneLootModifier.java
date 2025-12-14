package com.yesman.epicskills.data.loot;

import org.jetbrains.annotations.NotNull;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.registry.entry.EpicSkillsItems;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class AbilityStoneLootModifier extends LootModifier {
	public static final MapCodec<AbilityStoneLootModifier> CODEC = RecordCodecBuilder.mapCodec(
		instance -> 
			LootModifier
				.codecStart(instance)
				.apply(instance, AbilityStoneLootModifier::new)
	);
	
	public static final TagKey<EntityType<?>> ABILITY_STONE_DROPPER_TAG = TagKey.create(Registries.ENTITY_TYPE, EpicSkills.identifier("ability_stone_dropper"));
	
	protected AbilityStoneLootModifier(LootItemCondition[] conditions) {
		super(conditions);
	}
	
	@Override
	public MapCodec<? extends IGlobalLootModifier> codec() {
		return CODEC;
	}
	
	@Override
	protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
		Entity entity = context.getParamOrNull(LootContextParams.THIS_ENTITY);
		
		if (entity != null && entity.getType().is(ABILITY_STONE_DROPPER_TAG)) {
			generatedLoot.add(EpicSkillsItems.ABILIITY_STONE.get().getDefaultInstance());
		}
		
		return generatedLoot;
	}
}
