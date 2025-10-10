package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.SkillTreeProgression;
import com.yesman.epicskills.world.capability.SkillTreeProgression.NodeState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.RegistryManager;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.skill.Skill;

public record ClientBoundUnlockNode(ResourceKey<SkillTree> skillTree, Skill skill, NodeState nodeState, boolean unlockAlarm, boolean unequip, boolean askChange, boolean closeScreen) {
	public static ClientBoundUnlockNode fromBytes(FriendlyByteBuf buf) {
		ClientBoundUnlockNode msg = new ClientBoundUnlockNode(buf.readResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY), buf.readRegistryId(), buf.readEnum(NodeState.class), buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
		return msg;
	}
	
	public static void toBytes(ClientBoundUnlockNode msg, FriendlyByteBuf buf) {
		buf.writeResourceKey(msg.skillTree());
		buf.writeRegistryId(RegistryManager.ACTIVE.getRegistry(SkillManager.SKILL_REGISTRY_KEY), msg.skill());
		buf.writeEnum(msg.nodeState());
		buf.writeBoolean(msg.unlockAlarm());
		buf.writeBoolean(msg.unequip());
		buf.writeBoolean(msg.askChange());
		buf.writeBoolean(msg.closeScreen());
	}
	
	public static void handle(ClientBoundUnlockNode msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> {
				skilltreeProgression.processSyncPacket(msg);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
