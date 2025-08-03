package com.yesman.epicskills.client.gui.components.toasts;

import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.skilltree.SkillTree;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkillTreeToast implements Toast {
	private final Holder.Reference<SkillTree> skillTree;
	
	public SkillTreeToast(Holder.Reference<SkillTree> skillTree) {
		this.skillTree = skillTree;
	}
	
	@Override
	public Visibility render(GuiGraphics pGuiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
		if (this.skillTree == null || this.skillTree.get() == null) {
			return Toast.Visibility.HIDE;
		}
		
		pGuiGraphics.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());
		pGuiGraphics.blit(SkillTreeScreen.SKILL_TREE_ICON_TEXTURES.apply(this.skillTree), 3, -1, 0.0F, 0.0F, 32, 32, 32, 32);
		
		Component line1 = Component.translatable("chat.epicskills.unlock_notification");
		Component line2 = ComponentUtils.wrapInSquareBrackets(
			Component.
				translatable(SkillTree.toDescriptionId(this.skillTree.key()))
		);
		
		Vec3i color = this.skillTree.get().menuBarColor();
		
		pGuiGraphics.drawString(pToastComponent.getMinecraft().font, line1, 36, 6, -1);
		pGuiGraphics.drawString(pToastComponent.getMinecraft().font, line2, 36, 17, ARGB32.color(255, color.getX(), color.getY(), color.getZ()));
		
		return (double)pTimeSinceLastVisible >= 5000.0D * pToastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
	}
}
