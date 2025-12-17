package com.yesman.epicskills.client.input;

import org.jetbrains.annotations.NotNull;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mod.EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
public class EpicSkillsKeyMappingHandler {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        final LocalPlayerPatch localPlayerPatch = ClientEngine.getInstance().getPlayerPatch();
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
