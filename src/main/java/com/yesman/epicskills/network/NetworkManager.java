package com.yesman.epicskills.network;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.client.ClientBoundDeallocateAbilityPoints;
import com.yesman.epicskills.network.client.ClientBoundReloadSkillTree;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;
import com.yesman.epicskills.network.client.ClientBoundSetTreeState;
import com.yesman.epicskills.network.client.ClientBoundTreeInitSyncPacket;
import com.yesman.epicskills.network.client.ClientBoundUnlockAchievedNode;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.network.server.ServerBoundConvertAbilityPointRequest;
import com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PacketDistributor.PacketTarget;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkManager {
	private static final String PROTOCOL_VERSION = "1";
	
	public static final SimpleChannel INSTANCE =
		NetworkRegistry.newSimpleChannel(
                EpicSkills.identifier("network_manager"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

	public static <MSG> void sendToServer(MSG message) {
		INSTANCE.sendToServer(message);
	}
	
	private static <MSG> void sendToClient(MSG message, PacketTarget packetTarget) {
		INSTANCE.send(packetTarget, message);
	}
	
	public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
		sendToClient(message, PacketDistributor.PLAYER.with(() -> player));
	}
	
	public static void registerPackets() {
		int id = 0;
		
		INSTANCE.registerMessage(id++, ClientBoundReloadSkillTree.class, ClientBoundReloadSkillTree::toBytes, ClientBoundReloadSkillTree::fromBytes, ClientBoundReloadSkillTree::handle);
		INSTANCE.registerMessage(id++, ClientBoundSetAbilityPoints.class, ClientBoundSetAbilityPoints::toBytes, ClientBoundSetAbilityPoints::fromBytes, ClientBoundSetAbilityPoints::handle);
		INSTANCE.registerMessage(id++, ClientBoundUnlockNode.class, ClientBoundUnlockNode::toBytes, ClientBoundUnlockNode::fromBytes, ClientBoundUnlockNode::handle);
		INSTANCE.registerMessage(id++, ClientBoundSetTreeState.class, ClientBoundSetTreeState::toBytes, ClientBoundSetTreeState::fromBytes, ClientBoundSetTreeState::handle);
		INSTANCE.registerMessage(id++, ClientBoundTreeInitSyncPacket.class, ClientBoundTreeInitSyncPacket::toBytes, ClientBoundTreeInitSyncPacket::fromBytes, ClientBoundTreeInitSyncPacket::handle);
		INSTANCE.registerMessage(id++, ClientBoundDeallocateAbilityPoints.class, ClientBoundDeallocateAbilityPoints::toBytes, ClientBoundDeallocateAbilityPoints::fromBytes, ClientBoundDeallocateAbilityPoints::handle);
		INSTANCE.registerMessage(id++, ClientBoundUnlockAchievedNode.class, ClientBoundUnlockAchievedNode::toBytes, ClientBoundUnlockAchievedNode::fromBytes, ClientBoundUnlockAchievedNode::handle);
		
		INSTANCE.registerMessage(id++, ServerBoundConvertAbilityPointRequest.class, ServerBoundConvertAbilityPointRequest::toBytes, ServerBoundConvertAbilityPointRequest::fromBytes, ServerBoundConvertAbilityPointRequest::handle);
		INSTANCE.registerMessage(id++, ServerBoundUnlockSkillRequest.class, ServerBoundUnlockSkillRequest::toBytes, ServerBoundUnlockSkillRequest::fromBytes, ServerBoundUnlockSkillRequest::handle);
	}
	
	@OnlyIn(Dist.CLIENT)
	public static Player getPlayerInClient() {
		return Minecraft.getInstance().player;
	}
}
