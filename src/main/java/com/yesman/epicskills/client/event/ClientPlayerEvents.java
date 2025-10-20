package com.yesman.epicskills.client.event;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
public abstract class ClientPlayerEvents {
	@SubscribeEvent
	public static void epicskills$clientPlayerRespawn(ClientPlayerNetworkEvent.Clone event) {
		if (event.getOldPlayer().getRemovalReason() == Entity.RemovalReason.KILLED) {
			return;
		}
		
		event.getOldPlayer().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			event.getNewPlayer().getExistingData(EpicSkillsAttachmentTypes.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression$2 -> {
				CompoundTag compound = new CompoundTag();
				skillTreeProgression.serializeTo(compound);
				skillTreeProgression$2.deserializeFrom(compound);
			});
		});
	}
}
