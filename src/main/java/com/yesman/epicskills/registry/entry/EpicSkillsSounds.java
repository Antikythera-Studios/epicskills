package com.yesman.epicskills.registry.entry;

import com.yesman.epicskills.EpicSkills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public abstract class EpicSkillsSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EpicSkills.MODID);
	
	// UI sounds
	public static final RegistryObject<SoundEvent> HOVER = registerSound("ui.hover");
	public static final RegistryObject<SoundEvent> GAIN_ABILITY_POINTS = registerSound("ui.gain_ability_points");
	
	private static RegistryObject<SoundEvent> registerSound(String name) {
		ResourceLocation res = ResourceLocation.fromNamespaceAndPath(EpicSkills.MODID, name);
		return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(res));
	}
}
