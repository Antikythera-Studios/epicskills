package com.yesman.epicskills;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.yesman.epicskills.client.gui.screen.CategorySlotTexture;
import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;
import com.yesman.epicskills.registry.entry.EpicSkillsGlobalLootModifer;
import com.yesman.epicskills.registry.entry.EpicSkillsItems;
import com.yesman.epicskills.registry.entry.EpicSkillsSounds;
import com.yesman.epicskills.server.commands.PlayerAbilityPointsCommand;
import com.yesman.epicskills.server.commands.PlayerSkillTreeCommand;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.skilltree.SkillTreeEntry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.registry.entries.EpicFightCreativeTabs;

/**
 *  ***************************************************************
 *  Major changes
 *  ***************************************************************
 *  21.2.0
 *  
 *  Ported from Epic Fight: Skill tree 20.2.0
 *  
 *  ***************************************************************
 *  
 *  @author yesman
 */
@Mod(EpicSkills.MODID)
public class EpicSkills {
    public static final String MODID = "epicskills";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public static String prefix(String s) {
		return String.format("%s:%s", MODID, s);
	}
	
	public static String format(String s) {
		return String.format(s, MODID);
	}
    
	public EpicSkills(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::epicskills$newDataPackRegistryEvent);
        modEventBus.addListener(this::epicskills$buildCreativeTabContents);
        
        EpicSkillsSounds.REGISTRY.register(modEventBus);
        EpicSkillsItems.REGISTRY.register(modEventBus);
        EpicSkillsAttachmentTypes.REGISTRY.register(modEventBus);
        EpicSkillsGlobalLootModifer.GLOBAL_LOOT_LOOT_MODIFIERS.register(modEventBus);
        
        NeoForge.EVENT_BUS.addListener(this::epicskills$registerCommands);
        
        if (EpicFightSharedConstants.isPhysicalClient()) {
        	CategorySlotTexture.ENUM_MANAGER.registerEnumCls(EpicSkills.MODID, SkillTreeScreen.TreePage.NodeButton.CategorySlotTextures.class);
        }
    }
	
	public void epicskills$newDataPackRegistryEvent(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(SkillTree.SKILL_TREE_REGISTRY_KEY, SkillTree.CODEC, SkillTree.CODEC);
		event.dataPackRegistry(SkillTreeEntry.SKILL_TREE_ENTRY_REGISTRY_KEY, SkillTreeEntry.CODEC, SkillTreeEntry.CODEC);
	}
	
	private void epicskills$registerCommands(final RegisterCommandsEvent event) {
		PlayerAbilityPointsCommand.register(event.getDispatcher());
		PlayerSkillTreeCommand.register(event.getDispatcher(), event.getBuildContext());
    }
	
	private void epicskills$buildCreativeTabContents(final BuildCreativeModeTabContentsEvent event) {
		if (event.getTab() == EpicFightCreativeTabs.ITEMS.get()) {
			event.accept(EpicSkillsItems.ABILIITY_STONE.get().getDefaultInstance());
		}
	}
	
	@EventBusSubscriber(modid = EpicSkills.MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void epicskills$fmlClientSetup(FMLClientSetupEvent event) {
        	event.enqueueWork(CategorySlotTexture.ENUM_MANAGER::loadEnum);
        }
	}

	/// Creates an identifier that points to an Epic Skills resource.
	///
	/// This was called `identifier` and not `resourceLocation` since [Mojang renamed `ResourceLocation` to `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).
	public static @NotNull ResourceLocation identifier(@NotNull String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	/// @deprecated Use [#identifier(String)] instead. [Mojang renamed `ResourceLocation` to `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).
	@Deprecated(forRemoval = true)
	public static @NotNull ResourceLocation rl(@NotNull String path) {
		return identifier(path);
	}
}
