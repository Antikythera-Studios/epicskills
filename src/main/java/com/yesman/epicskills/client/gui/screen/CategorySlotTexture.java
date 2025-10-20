package com.yesman.epicskills.client.gui.screen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.ExtensibleEnum;
import yesman.epicfight.api.utils.ExtensibleEnumManager;

@OnlyIn(Dist.CLIENT)
public interface CategorySlotTexture extends ExtensibleEnum {
	ExtensibleEnumManager<CategorySlotTexture> ENUM_MANAGER = new ExtensibleEnumManager<> ("skill_category_slot_texture");
	
	int offsetX();
	
	int offsetY();
	
	int texWidth();
	
	int texHeight();
}
