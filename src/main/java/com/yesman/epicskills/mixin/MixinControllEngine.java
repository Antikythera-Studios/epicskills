package com.yesman.epicskills.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import yesman.epicfight.client.events.engine.ControllEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = ControllEngine.class)
public class MixinControllEngine {
	@Shadow
	private LocalPlayer player;
	
	@Shadow
	private LocalPlayerPatch playerpatch;
	
	@Redirect(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
			ordinal = 0
		),
		method = "handleEpicFightKeyMappings()V"
	)
	public void epicskills$handleEpicFightKeyMappings(Minecraft minecraft, Screen screen) {
		SkillTreeScreen skilltreescreen = new SkillTreeScreen(this.playerpatch);
		
		if (!skilltreescreen.discarded()) {
			minecraft.setScreen(skilltreescreen);
		}
	}
}
