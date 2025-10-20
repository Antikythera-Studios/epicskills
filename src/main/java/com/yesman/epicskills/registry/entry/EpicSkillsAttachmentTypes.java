package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.AbilityPoints;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class EpicSkillsAttachmentTypes {
	private EpicSkillsAttachmentTypes() {}
	
	public static final DeferredRegister<AttachmentType<?>> REGISTRY = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, EpicSkills.MODID);
	
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<AbilityPoints>> ABILITY_POINTS = REGISTRY.register(
        "ability_point",
        () ->
        	AttachmentType
                .builder(AbilityPoints::new)
                .build()
    );
	
	public static final DeferredHolder<AttachmentType<?>, AttachmentType<SkillTreeProgression>> SKILL_TREE_PROGRESSION = REGISTRY.register(
        "skill_tree_progression",
        () ->
        	AttachmentType
                .builder(SkillTreeProgression::new)
                .build()
    );
}
