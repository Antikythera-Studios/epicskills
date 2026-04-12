package com.yesman.epicskills.event;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.AbilityPoints;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;
import com.yesman.epicskills.network.client.ClientBoundReloadSkillTree;
import com.yesman.epicskills.network.client.ClientBoundSyncTreeState;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;
import com.yesman.epicskills.registry.entry.EpicSkillsItems;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityEvent.EntityConstructing;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

@EventBusSubscriber(modid = EpicSkills.MODID)
public abstract class GameEvents {
	@SubscribeEvent
	public static void epicskills$entityConstructing(EntityConstructing event) {
		if (event.getEntity().getType() == EntityType.PLAYER) {
			// Create attachments
			event.getEntity().getData(EpicSkillsAttachmentTypes.ABILITY_POINTS);
			event.getEntity().getData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION);
		}
	}
	
	@SubscribeEvent
	public static void epicskills$entityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getEntity().getType() == EntityType.PLAYER) {
			AbilityPoints abilityPoints = event.getEntity().getData(EpicSkillsAttachmentTypes.ABILITY_POINTS);
			SkillTreeProgression skilltreeProgression = event.getEntity().getData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION);
			
			if (event.getEntity() instanceof ServerPlayer serverplayer) {
				abilityPoints.markDirty();
				abilityPoints.sendChanges();
				CompoundTag compound = new CompoundTag();
				skilltreeProgression.serializeTo(compound);
				EpicFightNetworkManager.sendToPlayer(new ClientBoundSyncTreeState(compound), serverplayer);
			}
		}
	}
	
	@SubscribeEvent
	public static void epicskills$onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) {
			for (ServerPlayer serverPlayer : event.getPlayerList().getPlayers()) {
				serverPlayer.getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
					skillTreeProgression.reload(true);
					EpicFightNetworkManager.sendToPlayer(new ClientBoundReloadSkillTree(true), serverPlayer);
				});
			}
		}
	}
	
	@SubscribeEvent
	public static void epicskills$playerTickPost(PlayerTickEvent.Post event) {
		if (!event.getEntity().level().isClientSide()) {
			if (event.getEntity().tickCount % event.getEntity().getType().updateInterval() == 0) {
				event.getEntity().getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoints -> abilityPoints.sendChanges());
				event.getEntity().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> skilltreeProgression.tick());
			}
		}
	}
	
	@SubscribeEvent
	public static void epicskills$playerClone(PlayerEvent.Clone event) {
		boolean copyCaps = !event.isWasDeath() || EpicFightGameRules.KEEP_SKILLS.getRuleValue(event.getOriginal().level());
		
		if (!copyCaps) {
			return;
		}
		
		event.getOriginal().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			event.getEntity().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression$2 -> {
				CompoundTag compound = new CompoundTag();
				skillTreeProgression.serializeTo(compound);
				skillTreeProgression$2.deserializeFrom(compound);
			});
		});
		
		event.getOriginal().getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoints -> {
			event.getEntity().getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoints$2 -> {
				CompoundTag compound = new CompoundTag();
				abilityPoints.serializeTo(compound);
				abilityPoints$2.deserializeFrom(compound);
			});
		});
	}
	
	@SubscribeEvent
	public static void epicskills$lootTableLoad(final LootTableLoadEvent event) {
		// Low chance - many rolls
		if (
			event.getKey().equals(BuiltInLootTables.SIMPLE_DUNGEON) ||
			event.getKey().equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL) ||
			event.getKey().equals(BuiltInLootTables.UNDERWATER_RUIN_BIG) ||
			event.getKey().equals(BuiltInLootTables.ABANDONED_MINESHAFT) ||
			event.getKey().equals(BuiltInLootTables.NETHER_BRIDGE) ||
			event.getKey().equals(BuiltInLootTables.RUINED_PORTAL) ||
			event.getKey().equals(BuiltInLootTables.SHIPWRECK_SUPPLY) ||
			event.getKey().equals(BuiltInLootTables.SHIPWRECK_MAP) ||
			event.getKey().equals(BuiltInLootTables.BASTION_OTHER)
		) {
			event
				.getTable()
					.addPool(
						LootPool.lootPool()
							.setRolls(UniformGenerator.between(1.0F, 10.0F))
							.add(LootItem.lootTableItem(EpicSkillsItems.ABILIITY_STONE.get()))
							.when(LootItemRandomChanceCondition.randomChance(0.05F))
							.build()
					);
		}
		
		// Middle chance - modest rolls
		if (
			event.getKey().equals(BuiltInLootTables.ANCIENT_CITY) ||
			event.getKey().equals(BuiltInLootTables.END_CITY_TREASURE) ||
			event.getKey().equals(BuiltInLootTables.BASTION_BRIDGE) ||
			event.getKey().equals(BuiltInLootTables.BASTION_HOGLIN_STABLE) ||
			event.getKey().equals(BuiltInLootTables.DESERT_PYRAMID) ||
			event.getKey().equals(BuiltInLootTables.PILLAGER_OUTPOST) ||
			event.getKey().equals(BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER) ||
			event.getKey().equals(BuiltInLootTables.SHIPWRECK_TREASURE) ||
			event.getKey().equals(BuiltInLootTables.STRONGHOLD_CORRIDOR) ||
			event.getKey().equals(BuiltInLootTables.STRONGHOLD_CROSSING)
		) {
			event
				.getTable()
					.addPool(
						LootPool.lootPool()
							.setRolls(UniformGenerator.between(1.0F, 4.0F))
							.add(LootItem.lootTableItem(EpicSkillsItems.ABILIITY_STONE.get()))
							.when(LootItemRandomChanceCondition.randomChance(0.15F))
							.build()
					);
		}
		
		// High chance - one roll
		if (
			event.getKey().equals(BuiltInLootTables.BURIED_TREASURE) ||
			event.getKey().equals(BuiltInLootTables.JUNGLE_TEMPLE) ||
			event.getKey().equals(BuiltInLootTables.STRONGHOLD_LIBRARY) ||
			event.getKey().equals(BuiltInLootTables.BASTION_TREASURE) ||
			event.getKey().equals(BuiltInLootTables.WOODLAND_MANSION)
		) {
			event
				.getTable()
					.addPool(
						LootPool.lootPool()
							.setRolls(ConstantValue.exactly(1.0F))
							.add(LootItem.lootTableItem(EpicSkillsItems.ABILIITY_STONE.get()))
							.when(LootItemRandomChanceCondition.randomChance(0.75F))
							.build()
					);
		}
	}
}
