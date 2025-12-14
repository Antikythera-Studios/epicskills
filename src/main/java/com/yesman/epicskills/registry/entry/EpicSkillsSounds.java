package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public abstract class EpicSkillsSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, EpicSkills.MODID);
	
	// UI sounds
	public static final DeferredHolder<SoundEvent, SoundEvent> HOVER = registerSound("ui.hover");
	public static final DeferredHolder<SoundEvent, SoundEvent> GAIN_ABILITY_POINTS = registerSound("ui.gain_ability_points");
	
	private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        ResourceLocation res = EpicSkills.identifier(name);
		return REGISTRY.register(name, () -> SoundEvent.createVariableRangeEvent(res));
	}
}
