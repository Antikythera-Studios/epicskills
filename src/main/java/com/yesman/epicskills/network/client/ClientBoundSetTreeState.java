package com.yesman.epicskills.network.client;

import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression.TreeState;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ClientBoundSetTreeState(ResourceKey<SkillTree> skillTree, TreeState treeState, boolean unequip) implements ManagedCustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundSetTreeState> STREAM_CODEC =
		StreamCodec.composite(
	        ByteBufCodecsExtends.getResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY),
	        ClientBoundSetTreeState::skillTree,
	        ByteBufCodecsExtends.enumCodec(TreeState.class),
	        ClientBoundSetTreeState::treeState,
	        ByteBufCodecs.BOOL,
	        ClientBoundSetTreeState::unequip,
	        ClientBoundSetTreeState::new
	    );
}
