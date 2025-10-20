package com.yesman.epicskills.client.gui.screen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BackgroundRenderableScreen {
	
	public boolean isBackgroundMode();
	
	public void setBackgroundMode(boolean flag);
}
