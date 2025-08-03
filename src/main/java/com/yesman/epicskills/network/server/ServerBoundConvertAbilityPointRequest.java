package com.yesman.epicskills.network.server;

import java.util.function.Supplier;

import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;
import com.yesman.epicskills.world.capability.AbilityPoints;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

public record ServerBoundConvertAbilityPointRequest() {
	public static ServerBoundConvertAbilityPointRequest fromBytes(FriendlyByteBuf buf) {
		return new ServerBoundConvertAbilityPointRequest();
	}

	public static void toBytes(ServerBoundConvertAbilityPointRequest msg, FriendlyByteBuf buf) {
	}
	
	public static void handle(ServerBoundConvertAbilityPointRequest msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			Player player = ctx.get().getSender();
			
			player.getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoint -> {
				boolean success = abilityPoint.convertExpToAbilityPoints();
				NetworkManager.sendToPlayer(new ClientBoundSetAbilityPoints(success, abilityPoint.getAbilityPoints(), abilityPoint.getRequiredExp()), (ServerPlayer)player);
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
