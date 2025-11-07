package com.yesman.epicskills.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.yesman.epicskills.EpicSkills;

import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

@EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
public class EpicSkillsKeyMappings {
	public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(EpicSkills.format("key.%s.open_skill_tree"), KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, InputConstants.KEY_N, EpicSkills.format("key.%s.gui"));
	
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(OPEN_SKILL_TREE);
	}
}