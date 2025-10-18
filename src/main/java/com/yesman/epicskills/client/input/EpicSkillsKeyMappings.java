package com.yesman.epicskills.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.yesman.epicskills.EpicSkills;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EpicSkillsKeyMappings {
	public static final KeyMapping OPEN_SKILL_TREE = new KeyMapping(EpicSkills.format("key.%s.open_skill_tree"), InputConstants.KEY_N, EpicSkills.format("key.%s.gui"));
	
	@SubscribeEvent
	public static void registerKeys(RegisterKeyMappingsEvent event) {
		event.register(OPEN_SKILL_TREE);
	}
}