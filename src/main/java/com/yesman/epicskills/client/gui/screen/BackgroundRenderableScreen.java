package com.yesman.epicskills.client.gui.screen;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BackgroundRenderableScreen {
	
	public boolean isBackgroundMode();
	
	public void setBackgroundMode(boolean flag);
}
