package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.skill.Skill;

/**
 * Resource keys of skill trees that is provided by this addon itself
 * Devs may use for unlocking skill trees manually
 * See {@link SkillTreeProgression#unlockNode(ResourceKey, Skill, ServerPlayer)}, {@link SkillTreeProgression#lockNode(ResourceKey, Skill, boolean, ServerPlayer)}
 */
public interface EpicSkillsSkillTrees {
	ResourceKey<SkillTree> BATTLEBORN = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, EpicSkills.identifier("battleborn"));
	ResourceKey<SkillTree> INFERNAL_MIGHT = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, EpicSkills.identifier("infernal_might"));
}
