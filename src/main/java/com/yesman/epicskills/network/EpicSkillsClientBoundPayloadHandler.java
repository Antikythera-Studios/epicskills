package com.yesman.epicskills.network;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.network.client.ClientBoundReloadSkillTree;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;
import com.yesman.epicskills.network.client.ClientBoundSetTreeState;
import com.yesman.epicskills.network.client.ClientBoundSyncTreeState;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface EpicSkillsClientBoundPayloadHandler {
	public static void handleReloadSkillTree(final ClientBoundReloadSkillTree data, final IPayloadContext context) {
		context.player().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			skillTreeProgression.reload(data.readOldData());
		});
	}
	
	public static void handleSetAbilityPoints(final ClientBoundSetAbilityPoints data, final IPayloadContext context) {
		if (Minecraft.getInstance().screen instanceof SkillTreeScreen skillTreeScreen) {
			skillTreeScreen.onSyncPacketArrived(data);
		}
		
		context.player().getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilityPoint -> {
			abilityPoint.setAbilityPoints(data.abilityPoint());
			abilityPoint.setRequiredExp(data.requiredExp());
			context.player().giveExperiencePoints(-abilityPoint.getRequiredExp());
		});
	}
	
	public static void handleSyncTreeState(final ClientBoundSyncTreeState data, final IPayloadContext context) {
		context.player().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			skillTreeProgression.deserializeFrom(data.compound());
		});
	}
	
	public static void handleUnlockNode(final ClientBoundUnlockNode data, final IPayloadContext context) {
		context.player().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> {
			skilltreeProgression.processSyncPacket(data);
		});
	}
	
	public static void handleUnlockTree(final ClientBoundSetTreeState data, final IPayloadContext context) {
		context.player().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> {
			skilltreeProgression.processSyncPacket(data);
		});
	}
}
