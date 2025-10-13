package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * Resource keys of skill trees that is provided by this addon itself
 * Devs may use for unlocking skill trees manually
 * See {@link SkillTreeProgression#unlockNode}, {@link SkillTreeProgression#unlockNode}
 */
public interface EpicSkillsSkillTrees {
	ResourceKey<SkillTree> BATTLEBORN = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(EpicSkills.MODID, "battleborn"));
	ResourceKey<SkillTree> INFERNAL_MIGHT = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, ResourceLocation.fromNamespaceAndPath(EpicSkills.MODID, "infernal_might"));
}
