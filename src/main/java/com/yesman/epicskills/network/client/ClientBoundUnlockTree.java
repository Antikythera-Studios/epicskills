package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundUnlockTree(ResourceKey<SkillTree> skillTree) {
	public static ClientBoundUnlockTree fromBytes(FriendlyByteBuf buf) {
		ClientBoundUnlockTree msg = new ClientBoundUnlockTree(buf.readResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY));
		return msg;
	}
	
	public static void toBytes(ClientBoundUnlockTree msg, FriendlyByteBuf buf) {
		buf.writeResourceKey(msg.skillTree());
	}
	
	public static void handle(ClientBoundUnlockTree msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skilltreeProgression -> {
				skilltreeProgression.processSyncPacket(msg);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
