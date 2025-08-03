package com.yesman.epicskills.client.event;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
public abstract class ClientPlayerEvents {
	@SubscribeEvent
	public static void epicskills$clientPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
		if (event.getOldPlayer().getRemovalReason() == Entity.RemovalReason.KILLED) {
			return;
		}
		
		event.getOldPlayer().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			event.getNewPlayer().getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression$2 -> {
				CompoundTag compound = new CompoundTag();
				skillTreeProgression.serializeTo(compound);
				skillTreeProgression$2.deserializeFrom(compound);
			});
		});
	}
}
