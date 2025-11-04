package com.yesman.epicskills.mixin;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.client.input.EpicSkillsKeyMappings;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = ControlEngine.class)
public class MixinControlEngine {
	
	@Shadow(remap = false)
	private LocalPlayerPatch playerPatch;
	
	@Shadow(remap = false)
	private static boolean isKeyPressed(KeyMapping key, boolean eventCheck) { throw new AbstractMethodError(); }
	
	@Inject(at = @At(value = "HEAD"), method = "handleEpicFightKeyMappings()V", remap = false)
	public void epicskills$handleEpicFightKeyMappings(CallbackInfo callbackInfo) {
		if (this.playerPatch != null) {
			if (isKeyPressed(EpicSkillsKeyMappings.OPEN_SKILL_TREE, false)) {
				SkillTreeScreen skilltreescreen = new SkillTreeScreen(this.playerPatch);
				
				if (!skilltreescreen.discarded()) {
                    final Minecraft minecraft = Minecraft.getInstance();
					minecraft.setScreen(skilltreescreen);
				}
			}
		}
	}
}
