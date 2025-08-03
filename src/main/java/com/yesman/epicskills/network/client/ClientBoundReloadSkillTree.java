package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundReloadSkillTree(boolean readOldData) {
	public static ClientBoundReloadSkillTree fromBytes(FriendlyByteBuf buf) {
		return new ClientBoundReloadSkillTree(buf.readBoolean());
	}
	
	public static void toBytes(ClientBoundReloadSkillTree msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.readOldData);
	}
	
	public static void handle(ClientBoundReloadSkillTree msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
				skillTreeProgression.reload(msg.readOldData);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
