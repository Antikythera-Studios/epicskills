package com.yesman.epicskills.network;

import org.apache.commons.lang3.mutable.MutableBoolean;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.AbilityPoints;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression.NodeState;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.network.server.ServerBoundConvertAbilityPointRequest;
import com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

public interface EpicSkillsServerBoundPayloadHandler {
	public static void handleConvertAbilityPointRequest(final ServerBoundConvertAbilityPointRequest data, final IPayloadContext context) {
		Player player = context.player();
		
		player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoint -> {
			boolean success = abilityPoint.convertExpToAbilityPoints();
			EpicFightNetworkManager.sendToPlayer(new ClientBoundSetAbilityPoints(success, abilityPoint.getAbilityPoints(), abilityPoint.getRequiredExp()), (ServerPlayer)player);
		});
	}
	
	public static void handleUnlockSkillRequest(final ServerBoundUnlockSkillRequest data, final IPayloadContext context) {
		Player player = context.player();
		SkillTreeProgression skillTreeProgression = player.getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).orElseThrow(() -> new IllegalStateException("No skill tree progression"));
		AbilityPoints abilityPoints = player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).orElseThrow(() -> new IllegalStateException("No skill tree progression"));
		
		player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).get(data.skillTree()).ifPresentOrElse(skillTree -> {
			if (skillTreeProgression.canUnlockNode(skillTree, data.skill().value(), abilityPoints, true)) {
				skillTreeProgression.unlockNode(skillTree, data.skill().value());
				
				MutableBoolean askSkillChangeLater = new MutableBoolean(false);
				
				EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(playerpatch -> {
					if (!playerpatch.getPlayerSkills().isEquipping(data.skill().value())) {
						SkillContainer container = playerpatch.getPlayerSkills().getFirstEmptyContainer(data.skill().value().getCategory());
						
						if (container != null) {
							if (container.setSkill(data.skill().value())) {
								EpicFightNetworkManager.sendToPlayer(container.createSyncPacketToLocalPlayer(), playerpatch.getOriginal());
								EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(container.createSyncPacketToRemotePlayer(), player);
							}
						} else {
							askSkillChangeLater.setTrue();
						}
					}
				});
				
				EpicFightNetworkManager.sendToPlayer(new ClientBoundUnlockNode(data.skillTree(), data.skill(), NodeState.UNLOCKED, false, false, askSkillChangeLater.booleanValue(), true), (ServerPlayer)player);
			}
			
			abilityPoints.sendChanges();
		}, () -> {
			EpicSkills.LOGGER.error("No skill tree: " + SkillTree.SKILL_TREE_REGISTRY_KEY);
		});
	}
}
