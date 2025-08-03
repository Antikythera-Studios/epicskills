package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundTreeInitSyncPacket(CompoundTag compound) {
	public static ClientBoundTreeInitSyncPacket fromBytes(FriendlyByteBuf buf) {
		return new ClientBoundTreeInitSyncPacket(buf.readNbt());
	}
	
	public static void toBytes(ClientBoundTreeInitSyncPacket msg, FriendlyByteBuf buf) {
		buf.writeNbt(msg.compound());
	}
	
	public static void handle(ClientBoundTreeInitSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			NetworkManager.getPlayerInClient().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
				skillTreeProgression.deserializeFrom(msg.compound());
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
