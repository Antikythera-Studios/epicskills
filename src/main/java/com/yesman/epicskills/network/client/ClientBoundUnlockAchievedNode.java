package com.yesman.epicskills.network.client;

import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.network.ManagedCustomPacketPayload;
import yesman.epicfight.skill.Skill;

public record ClientBoundUnlockAchievedNode(ResourceKey<SkillTree> skillTree, Holder<Skill> skill, SkillTreeProgression.NodeState nodeState, boolean unlockAlarm, boolean unequip, boolean askChange, boolean closeScreen) implements ManagedCustomPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientBoundUnlockAchievedNode> STREAM_CODEC =
        ByteBufCodecsExtends.composite7(
            ByteBufCodecsExtends.getResourceKey(SkillTree.SKILL_TREE_REGISTRY_KEY),
            ClientBoundUnlockAchievedNode::skillTree,
            Skill.STREAM_CODEC,
            ClientBoundUnlockAchievedNode::skill,
            ByteBufCodecsExtends.enumCodec(SkillTreeProgression.NodeState.class),
            ClientBoundUnlockAchievedNode::nodeState,
            ByteBufCodecs.BOOL,
            ClientBoundUnlockAchievedNode::unlockAlarm,
            ByteBufCodecs.BOOL,
            ClientBoundUnlockAchievedNode::unequip,
            ByteBufCodecs.BOOL,
            ClientBoundUnlockAchievedNode::askChange,
            ByteBufCodecs.BOOL,
            ClientBoundUnlockAchievedNode::closeScreen,
            ClientBoundUnlockAchievedNode::new
        );
}