package com.yesman.epicskills.data;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.common.data.SkillTreeProvider;
import com.yesman.epicskills.data.provider.EpicSkillsSkillTreeProvider;

import net.minecraft.data.DataProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EpicSkills.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataEvents {
	private DataEvents() {}
	
	@SubscribeEvent
	public static void epicskills$gatherData(GatherDataEvent evnet) {
		evnet.getGenerator().addProvider(true, (DataProvider.Factory<SkillTreeProvider>)EpicSkillsSkillTreeProvider::new);
	}
}
