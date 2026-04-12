package com.yesman.epicskills.event;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundReloadSkillTree;
import com.yesman.epicskills.network.client.ClientBoundTreeInitSyncPacket;
import com.yesman.epicskills.registry.entry.EpicSkillsItems;
import com.yesman.epicskills.world.capability.AbilityPoints;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

@Mod.EventBusSubscriber(modid = EpicSkills.MODID)
public abstract class GameEvents {
	@SubscribeEvent
	public static void epicskills$entityJoinLevel(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof ServerPlayer serverplayer) {
			serverplayer.getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoints -> {
				abilityPoints.markDirty();
				abilityPoints.sendChanges();
			});
			
			serverplayer.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
				CompoundTag compound = new CompoundTag();
				skillTreeProgression.serializeTo(compound);
				
				NetworkManager.sendToPlayer(new ClientBoundTreeInitSyncPacket(compound), serverplayer);
			});
		}
	}
	
	@SubscribeEvent
	public static void epicskills$onDatapackSync(OnDatapackSyncEvent event) {
		if (event.getPlayer() == null) {
			for (ServerPlayer serverPlayer : event.getPlayers()) {
				serverPlayer.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
					skillTreeProgression.reload(true);
					NetworkManager.sendToPlayer(new ClientBoundReloadSkillTree(true), serverPlayer);
				});
			}
		}
	}
	
	@SubscribeEvent
	public static void epicskills$playerTickPost(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()) {
			if (event.player.tickCount % event.player.getType().updateInterval() == 0) {
				event.player.getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoints -> abilityPoints.sendChanges());
				event.player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> skilltreeProgression.tick());
			}
		}
	}
	
	@SubscribeEvent
	public static void epicskills$playerClone(PlayerEvent.Clone event) {
		boolean copyCaps = !event.isWasDeath() || EpicFightGameRules.KEEP_SKILLS.getRuleValue(event.getOriginal().level());
		
		if (!copyCaps) {
			return;
		}
		
		event.getOriginal().reviveCaps();
		
		event.getOriginal().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			event.getEntity().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression$2 -> {
				CompoundTag compound = new CompoundTag();
				skillTreeProgression.serializeTo(compound);
				skillTreeProgression$2.deserializeFrom(compound);
			});
		});
		
		event.getOriginal().getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoints -> {
			event.getEntity().getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoints$2 -> {
				CompoundTag compound = new CompoundTag();
				abilityPoints.serializeTo(compound);
				abilityPoints$2.deserializeFrom(compound);
			});
		});
		
		event.getOriginal().invalidateCaps();
	}
	
	@SubscribeEvent
	public static void epicskills$lootTableLoad(final LootTableLoadEvent event) {
		// Low chance - many rolls
		if (
			event.getName().equals(BuiltInLootTables.SIMPLE_DUNGEON) ||
			event.getName().equals(BuiltInLootTables.UNDERWATER_RUIN_SMALL) ||
			event.getName().equals(BuiltInLootTables.UNDERWATER_RUIN_BIG) ||
			event.getName().equals(BuiltInLootTables.ABANDONED_MINESHAFT) ||
			event.getName().equals(BuiltInLootTables.NETHER_BRIDGE) ||
			event.getName().equals(BuiltInLootTables.RUINED_PORTAL) ||
			event.getName().equals(BuiltInLootTables.SHIPWRECK_SUPPLY) ||
			event.getName().equals(BuiltInLootTables.SHIPWRECK_MAP) ||
			event.getName().equals(BuiltInLootTables.BASTION_OTHER)
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
			event.getName().equals(BuiltInLootTables.ANCIENT_CITY) ||
			event.getName().equals(BuiltInLootTables.END_CITY_TREASURE) ||
			event.getName().equals(BuiltInLootTables.BASTION_BRIDGE) ||
			event.getName().equals(BuiltInLootTables.BASTION_HOGLIN_STABLE) ||
			event.getName().equals(BuiltInLootTables.DESERT_PYRAMID) ||
			event.getName().equals(BuiltInLootTables.PILLAGER_OUTPOST) ||
			event.getName().equals(BuiltInLootTables.JUNGLE_TEMPLE_DISPENSER) ||
			event.getName().equals(BuiltInLootTables.SHIPWRECK_TREASURE) ||
			event.getName().equals(BuiltInLootTables.STRONGHOLD_CORRIDOR) ||
			event.getName().equals(BuiltInLootTables.STRONGHOLD_CROSSING)
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
			event.getName().equals(BuiltInLootTables.BURIED_TREASURE) ||
			event.getName().equals(BuiltInLootTables.JUNGLE_TEMPLE) ||
			event.getName().equals(BuiltInLootTables.STRONGHOLD_LIBRARY) ||
			event.getName().equals(BuiltInLootTables.BASTION_TREASURE) ||
			event.getName().equals(BuiltInLootTables.WOODLAND_MANSION)
			
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
