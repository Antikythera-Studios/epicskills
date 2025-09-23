package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.SkillTreeProgression;
import com.yesman.epicskills.world.capability.SkillTreeProgression.TreeState;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundSetTreeState(ResourceKey<SkillTree> skillTree, TreeState treeState, boolean unequip) {
	public static ClientBoundSetTreeState fromBytes(FriendlyByteBuf buf) {
		return new ClientBoundSetTreeState(buf.readResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY), buf.readEnum(TreeState.class), buf.readBoolean());
	}
	
	public static void toBytes(ClientBoundSetTreeState msg, FriendlyByteBuf buf) {
		buf.writeResourceKey(msg.skillTree());
		buf.writeEnum(msg.treeState());
		buf.writeBoolean(msg.unequip());
	}
	
	public static void handle(ClientBoundSetTreeState msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> {
				skilltreeProgression.processSyncPacket(msg);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
	
	public enum Action {
		LOCK, UNLOCK
	}
}
