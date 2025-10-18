package com.yesman.epicskills;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.yesman.epicskills.client.gui.screen.CategorySlotTexture;
import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.registry.entry.EpicSkiillsGlobalLootModifer;
import com.yesman.epicskills.registry.entry.EpicSkillsItems;
import com.yesman.epicskills.registry.entry.EpicSkillsSounds;
import com.yesman.epicskills.server.commands.PlayerAbilityPointsCommand;
import com.yesman.epicskills.server.commands.PlayerSkillTreeCommand;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.skilltree.SkillTreeEntry;
import com.yesman.epicskills.world.capability.AbilityPoints;
import com.yesman.epicskills.world.capability.SkillTreeProgression;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DataPackRegistryEvent;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.item.EpicFightCreativeTabs;

/**
 *  ***************************************************************
 *  Major changes
 *  ***************************************************************
 *  20.2.0
 *  
 *  UI enhancements
 *  Replace default skill tree open key to 'N', And now you can open original skill edit screen in Epic Fight
 *  Added skill editor open button in skill tree screen
 *  
 *  System changes
 *  Players now get restricted by a cooldown to replace skills, which added in Epic Fight 20.13.1
 *  
 *  Bugfix
 *  Fixed husk not dropping Ability stone
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
    
	public EpicSkills(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        modEventBus.addListener(this::epicskills$newDataPackRegistryEvent);
        modEventBus.addListener(this::epicskills$doCommonStuff);
        modEventBus.addListener(this::epicskills$buildCreativeTabContents);
        
        EpicSkillsSounds.SOUNDS.register(modEventBus);
        EpicSkillsItems.ITEMS.register(modEventBus);
        EpicSkiillsGlobalLootModifer.GLOBAL_LOOT_LOOT_MODIFIERS.register(modEventBus);
        
        MinecraftForge.EVENT_BUS.addListener(this::epicskills$registerCommands);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, AbilityPoints::epicskills$attachCapabilities);
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, SkillTreeProgression::epicskills$attachCapabilities);
        
        if (EpicFightSharedConstants.isPhysicalClient()) {
        	CategorySlotTexture.ENUM_MANAGER.registerEnumCls(EpicSkills.MODID, SkillTreeScreen.TreePage.NodeButton.CategorySlotTextures.class);
        }
    }
	
	public void epicskills$newDataPackRegistryEvent(DataPackRegistryEvent.NewRegistry event) {
		event.dataPackRegistry(SkillTree.SKILL_TREE_REGISTRY_KEY, SkillTree.CODEC, SkillTree.CODEC);
		event.dataPackRegistry(SkillTreeEntry.SKILL_TREE_ENTRY_REGISTRY_KEY, SkillTreeEntry.CODEC, SkillTreeEntry.CODEC);
	}
	
	private void epicskills$doCommonStuff(final FMLCommonSetupEvent event) {
		event.enqueueWork(NetworkManager::registerPackets);
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
	
	@Mod.EventBusSubscriber(modid = EpicSkills.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void epicskills$fmlClientSetup(FMLClientSetupEvent event) {
        	event.enqueueWork(CategorySlotTexture.ENUM_MANAGER::loadEnum);
        }
	}
}
