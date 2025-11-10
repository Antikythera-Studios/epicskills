package com.yesman.epicskills.mixin;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.client.input.EpicSkillsKeyMappings;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = ControlEngine.class)
public class MixinControlEngine {
	@Inject(at = @At(value = "HEAD"), method = "handleEpicFightKeyMappings()V", remap = false)
	public void epicskills$handleEpicFightKeyMappings(CallbackInfo callbackInfo) {
        final ControlEngine controlEngine = ((ControlEngine) (Object) this);
        final LocalPlayerPatch localPlayerPatch = controlEngine.getPlayerPatch();
        if (localPlayerPatch == null) {
            return;
        }
        while (EpicSkillsKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
            epicskills$openSkillTree(localPlayerPatch);
        }
	}

    @Unique
    private void epicskills$openSkillTree(@NotNull LocalPlayerPatch localPlayerPatch) {
        SkillTreeScreen skilltreescreen = new SkillTreeScreen(localPlayerPatch);
        if (skilltreescreen.discarded()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(skilltreescreen);
    }
}
