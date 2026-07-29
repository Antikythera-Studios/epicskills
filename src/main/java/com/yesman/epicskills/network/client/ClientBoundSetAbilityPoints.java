package com.yesman.epicskills.network.client;

import java.util.function.Supplier;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.world.capability.AbilityPoints;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ClientBoundSetAbilityPoints(boolean success, int abilityPoint, int requiredExp) {
	public static ClientBoundSetAbilityPoints fromBytes(FriendlyByteBuf buf) {
		ClientBoundSetAbilityPoints msg = new ClientBoundSetAbilityPoints(buf.readBoolean(), buf.readInt(), buf.readInt());
		return msg;
	}
	
	public static void toBytes(ClientBoundSetAbilityPoints msg, FriendlyByteBuf buf) {
		buf.writeBoolean(msg.success());
		buf.writeInt(msg.abilityPoint());
		buf.writeInt(msg.requiredExp());
	}
	
	public static void handle(ClientBoundSetAbilityPoints msg, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			if (Minecraft.getInstance().screen instanceof SkillTreeScreen skillTreeScreen) {
				skillTreeScreen.onSyncPacketArrived(msg);
			}
			
			NetworkManager.getPlayerInClient().getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoint -> {
				int oldRequiredExp = abilityPoint.getRequiredExp();
				abilityPoint.setAbilityPoints(msg.abilityPoint());
				abilityPoint.setRequiredExp(msg.requiredExp());
				if (msg.success()) {
                	NetworkManager.getPlayerInClient().giveExperiencePoints(-oldRequiredExp);
            	}
			});
		});
		
		ctx.get().setPacketHandled(true);
	}
}
