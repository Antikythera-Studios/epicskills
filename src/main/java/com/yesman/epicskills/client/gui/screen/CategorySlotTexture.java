package com.yesman.epicskills.client.gui.screen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.api.utils.ExtendableEnum;
import yesman.epicfight.api.utils.ExtendableEnumManager;

@OnlyIn(Dist.CLIENT)
public interface CategorySlotTexture extends ExtendableEnum {
	ExtendableEnumManager<CategorySlotTexture> ENUM_MANAGER = new ExtendableEnumManager<> ("skill_category_slot_texture");
	
	int offsetX();
	
	int offsetY();
	
	int texWidth();
	
	int texHeight();
}
