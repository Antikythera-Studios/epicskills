package com.yesman.epicskills.network.server;

import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.network.ManagedCustomPacketPayload;
import yesman.epicfight.skill.Skill;

public record ServerBoundUnlockSkillRequest(ResourceKey<SkillTree> skillTree, Holder<Skill> skill) implements ManagedCustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundUnlockSkillRequest> STREAM_CODEC =
		StreamCodec.composite(
	        ByteBufCodecsExtends.getResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY),
	        ServerBoundUnlockSkillRequest::skillTree,
	        Skill.STREAM_CODEC,
	        ServerBoundUnlockSkillRequest::skill,
	        ServerBoundUnlockSkillRequest::new
	    );
}
