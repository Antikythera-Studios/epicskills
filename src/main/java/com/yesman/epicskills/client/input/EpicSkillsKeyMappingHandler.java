package com.yesman.epicskills.client.input;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
public class EpicSkillsKeyMappingHandler {
    @SubscribeEvent
    private static void onClientTick(ClientTickEvent.Post event) {
        final LocalPlayerPatch localPlayerPatch = ControlEngine.getInstance().getPlayerPatch();
        if (localPlayerPatch == null) {
            return;
        }
        while (EpicSkillsKeyMappings.OPEN_SKILL_TREE.consumeClick()) {
            openSkillTree(localPlayerPatch);
        }
    }

    private static void openSkillTree(@NotNull LocalPlayerPatch localPlayerPatch) {
        SkillTreeScreen skillTreeScreen = new SkillTreeScreen(localPlayerPatch);
        if (skillTreeScreen.discarded()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(skillTreeScreen);
    }
}
