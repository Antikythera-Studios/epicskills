package com.yesman.epicskills.network;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.client.*;
import com.yesman.epicskills.network.server.ServerBoundConvertAbilityPointRequest;
import com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import yesman.epicfight.network.ManagedCustomPacketPayload;

@EventBusSubscriber(modid = EpicSkills.MODID)
public class NetworkManager {
	private static final String PROTOCOL_VERSION = "1";
	
	// Client bound payloads
	public static final CustomPacketPayload.Type<ClientBoundReloadSkillTree> CLIENT_BOUND_RELOAD_SKILL_TREE = ManagedCustomPacketPayload.registerPayloadType(ClientBoundReloadSkillTree.class, EpicSkills.MODID, "client_bound_reload_skill_tree");
	public static final CustomPacketPayload.Type<ClientBoundSetAbilityPoints> CLIENT_BOUND_SET_ABILITY_POINTS = ManagedCustomPacketPayload.registerPayloadType(ClientBoundSetAbilityPoints.class, EpicSkills.MODID, "client_bound_set_ability_points");
	public static final CustomPacketPayload.Type<ClientBoundSyncTreeState> CLIENT_BOUND_SYNC_TREE_STATE = ManagedCustomPacketPayload.registerPayloadType(ClientBoundSyncTreeState.class, EpicSkills.MODID, "client_bound_sync_tree_state");
	public static final CustomPacketPayload.Type<ClientBoundUnlockNode> CLIENT_BOUND_UNLOCK_NODE = ManagedCustomPacketPayload.registerPayloadType(ClientBoundUnlockNode.class, EpicSkills.MODID, "client_bound_reload_unlock_node");
	public static final CustomPacketPayload.Type<ClientBoundSetTreeState> CLIENT_BOUND_UNLOCK_TREE = ManagedCustomPacketPayload.registerPayloadType(ClientBoundSetTreeState.class, EpicSkills.MODID, "client_bound_reload_unlock_tree");
	public static final CustomPacketPayload.Type<ClientBoundDeallocateAbilityPoints> CLIENT_BOUND_DEALLOCATE_ABILITY_POINTS = ManagedCustomPacketPayload.registerPayloadType(ClientBoundDeallocateAbilityPoints.class, EpicSkills.MODID, "client_bound_deallocate_ability_points");
	public static final CustomPacketPayload.Type<ClientBoundUnlockAchievedNode> CLIENT_BOUND_UNLOCK_ACHIEVED_NODE = ManagedCustomPacketPayload.registerPayloadType(ClientBoundUnlockAchievedNode.class, EpicSkills.MODID, "client_bound_unlock_achieved_node");

	// Server bound payloads
	public static final CustomPacketPayload.Type<ServerBoundConvertAbilityPointRequest> SERVER_BOUND_CONVERT_ABILITY_POINT_REQUEST = ManagedCustomPacketPayload.registerPayloadType(ServerBoundConvertAbilityPointRequest.class, EpicSkills.MODID, "server_bound_animator_control");
	public static final CustomPacketPayload.Type<ServerBoundUnlockSkillRequest> SERVER_BOUND_UNLOCK_SKILL_REQUEST = ManagedCustomPacketPayload.registerPayloadType(ServerBoundUnlockSkillRequest.class, EpicSkills.MODID, "server_bound_change_player_mode");
	
	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
		
		registrar
			.playToClient(
	    		  CLIENT_BOUND_RELOAD_SKILL_TREE
	    		, ClientBoundReloadSkillTree.STREAM_CODEC
	    		, EpicSkillsClientBoundPayloadHandler::handleReloadSkillTree
	    	)
			.playToClient(
				  CLIENT_BOUND_SET_ABILITY_POINTS
	    		, ClientBoundSetAbilityPoints.STREAM_CODEC
	    		, EpicSkillsClientBoundPayloadHandler::handleSetAbilityPoints
	    	)
			.playToClient(
				  CLIENT_BOUND_SYNC_TREE_STATE
	    		, ClientBoundSyncTreeState.STREAM_CODEC
	    		, EpicSkillsClientBoundPayloadHandler::handleSyncTreeState
	    	)
			.playToClient(
				  CLIENT_BOUND_UNLOCK_NODE
	    		, ClientBoundUnlockNode.STREAM_CODEC
	    		, EpicSkillsClientBoundPayloadHandler::handleUnlockNode
	    	)
			.playToClient(
				  CLIENT_BOUND_UNLOCK_TREE
	    		, ClientBoundSetTreeState.STREAM_CODEC
	    		, EpicSkillsClientBoundPayloadHandler::handleUnlockTree
	    	)
            .playToClient(
                  CLIENT_BOUND_DEALLOCATE_ABILITY_POINTS
                , ClientBoundDeallocateAbilityPoints.STREAM_CODEC
                , EpicSkillsClientBoundPayloadHandler::handleDeallocateAbilityPoints
            )
            .playToClient(
                  CLIENT_BOUND_UNLOCK_ACHIEVED_NODE
                , ClientBoundUnlockAchievedNode.STREAM_CODEC
                , EpicSkillsClientBoundPayloadHandler::handleUnlockAchievedNode
            )
			;
		
		registrar
			.playToServer(
				  SERVER_BOUND_CONVERT_ABILITY_POINT_REQUEST
		    	, ServerBoundConvertAbilityPointRequest.STREAM_CODEC
		    	, EpicSkillsServerBoundPayloadHandler::handleConvertAbilityPointRequest
			)
			.playToServer(
				  SERVER_BOUND_UNLOCK_SKILL_REQUEST
		    	, ServerBoundUnlockSkillRequest.STREAM_CODEC
		    	, EpicSkillsServerBoundPayloadHandler::handleUnlockSkillRequest
			)
		;
	}
}
