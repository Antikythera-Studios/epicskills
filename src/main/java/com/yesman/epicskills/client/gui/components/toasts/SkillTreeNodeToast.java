package com.yesman.epicskills.client.gui.components.toasts;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import yesman.epicfight.skill.Skill;

@OnlyIn(Dist.CLIENT)
public class SkillTreeNodeToast implements Toast {
	private final Skill skill;
	
	public SkillTreeNodeToast(Skill skill) {
		this.skill = skill;
	}
	
	@Override
	public Visibility render(GuiGraphics guiGraphics, ToastComponent toastComponent, long timeSinceLastVisible) {
		if (this.skill == null) {
			return Toast.Visibility.HIDE;
		}
		
		guiGraphics.blit(TEXTURE, 0, 0, 0, 0, this.width(), this.height());
		guiGraphics.blit(this.skill.getSkillTexture(), 5, 3, 26, 26, 0.0F, 0.0F, 32, 32, 32, 32);
		
		Component line1 = Component.translatable("chat.epicskills.unlock_notification");
		Component line2 = ComponentUtils.wrapInSquareBrackets(
				Component.
					translatable(this.skill.getTranslationKey())
			)
			.withStyle(ChatFormatting.GOLD);
		
		guiGraphics.drawString(toastComponent.getMinecraft().font, line1, 36, 6, -1);
		guiGraphics.drawString(toastComponent.getMinecraft().font, line2, 36, 17, -1);
		
		return (double)timeSinceLastVisible >= 5000.0D * toastComponent.getNotificationDisplayTimeMultiplier() ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
	}
}
