package com.yesman.epicskills.network.client;

import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression.NodeState;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.network.ManagedCustomPacketPayload;
import yesman.epicfight.skill.Skill;

public record ClientBoundUnlockNode(ResourceKey<SkillTree> skillTree, Holder<Skill> skill, NodeState nodeState, boolean unlockAlarm, boolean unequip, boolean askChange, boolean closeScreen) implements ManagedCustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundUnlockNode> STREAM_CODEC =
		ByteBufCodecsExtends.composite7(
			ByteBufCodecsExtends.getResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY),
	        ClientBoundUnlockNode::skillTree,
	        Skill.STREAM_CODEC,
	        ClientBoundUnlockNode::skill,
	        ByteBufCodecsExtends.enumCodec(NodeState.class),
	        ClientBoundUnlockNode::nodeState,
	        ByteBufCodecs.BOOL,
	        ClientBoundUnlockNode::unlockAlarm,
	        ByteBufCodecs.BOOL,
	        ClientBoundUnlockNode::unequip,
	        ByteBufCodecs.BOOL,
	        ClientBoundUnlockNode::askChange,
	        ByteBufCodecs.BOOL,
	        ClientBoundUnlockNode::closeScreen,
	        ClientBoundUnlockNode::new
	    );
}
