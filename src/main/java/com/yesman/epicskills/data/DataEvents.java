package com.yesman.epicskills.data;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.common.data.SkillTreeProvider;
import com.yesman.epicskills.data.provider.EpicSkillsSkillTreeProvider;

import net.minecraft.data.DataProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = EpicSkills.MODID)
public final class DataEvents {
	private DataEvents() {}
	
	@SubscribeEvent
	public static void epicskills$gatherData(GatherDataEvent evnet) {
		evnet.getGenerator().addProvider(true, (DataProvider.Factory<SkillTreeProvider>)EpicSkillsSkillTreeProvider::new);
	}
}
