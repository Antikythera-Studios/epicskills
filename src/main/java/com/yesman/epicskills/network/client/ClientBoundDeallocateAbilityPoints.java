package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundDeallocateAbilityPoints(boolean unequipSkills) {
	public static ClientBoundDeallocateAbilityPoints fromBytes(FriendlyByteBuf buf) {
		return new ClientBoundDeallocateAbilityPoints(buf.readBoolean());
	}
	
	public static void toBytes(ClientBoundDeallocateAbilityPoints msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.unequipSkills());
	}
	
	public static void handle(ClientBoundDeallocateAbilityPoints msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
				skillTreeProgression.deallocateAbilityPoints(msg.unequipSkills());
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
