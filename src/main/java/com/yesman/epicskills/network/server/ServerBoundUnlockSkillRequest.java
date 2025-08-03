package com.yesman.epicskills.network.server;

import java.util.function.Supplier;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.AbilityPoints;
import com.yesman.epicskills.world.capability.SkillTreeProgression;
import com.yesman.epicskills.world.capability.SkillTreeProgression.NodeState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.RegistryManager;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

public record ServerBoundUnlockSkillRequest(ResourceKey<SkillTree> skilltree, Skill skill) {
	public static ServerBoundUnlockSkillRequest fromBytes(FriendlyByteBuf buf) {
		return new ServerBoundUnlockSkillRequest(buf.readResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY), buf.readRegistryId());
	}
	
	public static void toBytes(ServerBoundUnlockSkillRequest msg, FriendlyByteBuf buf) {
		buf.writeResourceKey(msg.skilltree());
		buf.writeRegistryId(RegistryManager.ACTIVE.getRegistry(SkillManager.SKILL_REGISTRY_KEY), msg.skill());
	}
	
	public static void handle(ServerBoundUnlockSkillRequest msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer player = ctx.get().getSender();
			SkillTreeProgression skillTreeProgression = player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).orElseThrow(() -> new IllegalStateException("No skill tree progression"));
			AbilityPoints abilityPoints = player.getCapability(AbilityPoints.ABILITY_POINTS).orElseThrow(() -> new IllegalStateException("No skill tree progression"));
			
			player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).get(msg.skilltree()).ifPresentOrElse(skillTree -> {
				if (skillTreeProgression.canUnlockNode(skillTree, msg.skill(), abilityPoints, true)) {
					skillTreeProgression.unlockNode(skillTree, msg.skill());
					NetworkManager.sendToPlayer(new ClientBoundUnlockNode(msg.skilltree(), msg.skill(), NodeState.UNLOCKED, false, false, true), (ServerPlayer)player);
				}
				
				abilityPoints.sendChanges();
			}, () -> {
				EpicSkills.LOGGER.error("No skill tree: " + SkillTree.SKILL_TREE_REGISTRY_KEY);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
