package com.yesman.epicskills.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.AbilityPoints;
import com.yesman.epicskills.neoforge.attachment.SkillTreeProgression;
import com.yesman.epicskills.network.client.ClientBoundUnlockAchievedNode;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.network.server.ServerBoundUnlockSkillRequest;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;
import com.yesman.epicskills.skilltree.SkillTree;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import yesman.epicfight.EpicFight;
import yesman.epicfight.client.gui.datapack.screen.MessageScreen;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.SlotSelectScreen;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class SkillInfoScreen extends SkillBookScreen {
	private final Holder.Reference<SkillTree> skillTree;
	private final SkillTreeProgression.TopDownTreeNode node;
	private final AbilityPoints abilityPoints;
	private boolean backgroundMode;
    private Button actionButton;

    public Button getActionButton() {
        return actionButton;
    }

	public SkillInfoScreen(Player opener, Holder.Reference<SkillTree> skillTree, SkillTreeProgression.TopDownTreeNode node, Screen parentScreen) {
		super(opener, node.nodeInfo().skill(), null, parentScreen);
		
		this.skillTree = skillTree;
		this.node = node;
		this.abilityPoints = opener.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).orElseThrow(() -> new IllegalStateException("No ability points"));
	}
	
	@Override
	protected void init() {
		super.init();
		
		this.parentScreen.init(this.minecraft, this.width, this.height);
		
		boolean active = true;
		Component tooltip = null;
		Component message = this.node.nodeState().displayedOnButton();
		
		if (this.node.nodeState() == SkillTreeProgression.NodeState.UNLOCKED) {
			int shortestCooldown = EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(this.playerpatch.getOriginal().level());
			
			for (SkillContainer skillContainer : this.playerpatch.getPlayerSkills().getSkillContainersFor(this.skill.getCategory())) {
				if (shortestCooldown > skillContainer.getReplaceCooldown()) shortestCooldown = skillContainer.getReplaceCooldown();
			}
			
			if (this.playerpatch.getPlayerSkills().isEquipping(this.skill)) {
				message = Component.translatable(EpicSkills.format("gui.%s.skillinfo.unequip"));
			} else if (shortestCooldown > 0 && !this.playerpatch.getOriginal().isCreative()) {
				tooltip = Component.translatable(EpicFightMod.format("gui.%s.container_on_cooldown"), shortestCooldown / 20);
				active = false;
			}
		} else if (this.node.nodeState() == SkillTreeProgression.NodeState.UNLOCKABLE) {
			if (this.node.nodeInfo().requiredAbilityPoints() > this.abilityPoints.getAbilityPoints()) {
				tooltip = Component.translatable(EpicSkills.format("gui.%s.skillinfo.no_ability_points.tooltip"), this.node.nodeInfo().requiredAbilityPoints(), this.abilityPoints.getAbilityPoints());
				active = false;
			}
		} else if (this.node.nodeState() == SkillTreeProgression.NodeState.LOCKED) {
			tooltip = Component.translatable(EpicSkills.format("gui.%s.skillinfo.locked.tooltip"));
			active = false;
		}
		
		actionButton =
			Button.builder(
				message,
				button -> {
					if (this.node.nodeState() == SkillTreeProgression.NodeState.UNLOCKABLE) {
						button.active = false;
						EpicFightNetworkManager.sendToServer(new ServerBoundUnlockSkillRequest(this.skillTree.key(), this.skill.holder()));
					} else if (this.node.nodeState() == SkillTreeProgression.NodeState.UNLOCKED) {
						if (this.playerpatch.getPlayerSkills().isEquipping(this.skill)) {
							this.playerpatch.getSkillContainerFor(this.skill).ifPresent(skillContainer -> {
								skillContainer.setSkill(null);
								EpicFightNetworkManager.sendToServer(new CPChangeSkill(skillContainer.getSlot(), null, -1));
							});
							
							this.minecraft.setScreen(this.parentScreen);
						} else {
							Set<SkillContainer> skillContainers = this.playerpatch.getPlayerSkills().getSkillContainersFor(this.skill.getCategory());
							
							if (skillContainers.size() == 1) {
								this.acquireSkillTo(skillContainers.iterator().next());
							} else {
								SlotSelectScreen slotSelectScreen = new SlotSelectScreen(skillContainers, this);
								this.minecraft.setScreen(slotSelectScreen);
							}
						}
					}
				}
			)
			.bounds((this.width) / 2 + 54, (this.height) / 2 + 90, 67, 21)
			.tooltip(tooltip == null ? null : Tooltip.create(tooltip))
			.build(LearnButton::new);
		
		actionButton.active = active;
		
		this.addRenderableWidget(actionButton);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, boolean asBackground) {
		this.backgroundMode = asBackground;
		
		if (this.parentScreen instanceof BackgroundRenderableScreen bgModeScreen) {
			bgModeScreen.setBackgroundMode(true);
		}
		
		this.parentScreen.render(guiGraphics, mouseX, mouseY, partialTick);
		
		if (this.parentScreen instanceof BackgroundRenderableScreen bgModeScreen) {
			bgModeScreen.setBackgroundMode(false);
		}
		
		guiGraphics.pose().pushPose();
		//guiGraphics.pose().translate(0.0D, 0.0D, 1000.0D);
		
		super.render(guiGraphics, mouseX, mouseY, partialTick, asBackground);
		
		guiGraphics.pose().popPose();
		
		this.backgroundMode = false;
	}
	
	@Override
	protected void acquireSkillTo(SkillContainer skillContainer) {
		skillContainer.setSkill(this.skill);
		EpicFightNetworkManager.sendToServer(new CPChangeSkill(skillContainer.getSlot(), this.skill.holder(), -1));
		this.minecraft.setScreen(this.parentScreen);
	}
	
	public void onSyncPacketArrived(ClientBoundUnlockNode feedbackPacket) {
		if (feedbackPacket.closeScreen()) {
			if (feedbackPacket.askChange() && this.playerpatch.getSkillContainerFor(feedbackPacket.skill().value()).isEmpty()) {
				var containers = this.playerpatch.getPlayerSkills().getSkillContainersFor(feedbackPacket.skill().value().getCategory());
				int shortestCooldown = EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(this.playerpatch.getOriginal().level());
				
				for (SkillContainer skillContainer : containers) {
					if (shortestCooldown > skillContainer.getReplaceCooldown()) shortestCooldown = skillContainer.getReplaceCooldown();
				}
				
				if (shortestCooldown == 0 || this.playerpatch.getOriginal().isCreative()) {
					this.minecraft.setScreen(
						new MessageScreen<> (
							"",
							containers.size() > 1 ? 
								Component.translatable(
									EpicSkills.format("gui.%s.messages.change_skill_multiple"),
									Component.translatable(feedbackPacket.skill().value().getTranslationKey()).getString()
								) :
								Component.translatable(
									EpicSkills.format("gui.%s.messages.change_skill_one"),
									Component.translatable(containers.iterator().next().getSkill().getTranslationKey()).getString(),
									Component.translatable(feedbackPacket.skill().value().getTranslationKey()).getString()
								),
							this,
							button -> {
								if (containers.size() > 1) {
									SlotSelectScreen slotSelectScreen = new SlotSelectScreen(containers, this);
									this.minecraft.setScreen(slotSelectScreen);
								} else {
									this.acquireSkillTo(containers.iterator().next());
								}
							},
							button -> {
								this.minecraft.setScreen(this.parentScreen);
							},
							180,
							0
						).setLayerFarPlane(2000).autoCalculateHeight()
					);
					
					return;
				}
			}
			
			this.minecraft.setScreen(this.parentScreen);
		}
	}

    public void onSyncPacketArrived(ClientBoundUnlockAchievedNode feedbackPacket) {
        if (feedbackPacket.closeScreen()) {
            if (feedbackPacket.askChange() && this.playerpatch.getSkillContainerFor(feedbackPacket.skill().value()).isEmpty()) {
                var containers = this.playerpatch.getPlayerSkills().getSkillContainersFor(feedbackPacket.skill().value().getCategory());
                int shortestCooldown = EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(this.playerpatch.getOriginal().level());

                for (SkillContainer skillContainer : containers) {
                    if (shortestCooldown > skillContainer.getReplaceCooldown()) shortestCooldown = skillContainer.getReplaceCooldown();
                }

                if (shortestCooldown == 0 || this.playerpatch.getOriginal().isCreative()) {
                    this.minecraft.setScreen(
                        new MessageScreen<> (
                            "",
                            containers.size() > 1 ?
                                Component.translatable(
                                    EpicSkills.format("gui.%s.messages.change_skill_multiple"),
                                    Component.translatable(feedbackPacket.skill().value().getTranslationKey()).getString()
                                ) :
                                Component.translatable(
                                    EpicSkills.format("gui.%s.messages.change_skill_one"),
                                    Component.translatable(containers.iterator().next().getSkill().getTranslationKey()).getString(),
                                    Component.translatable(feedbackPacket.skill().value().getTranslationKey()).getString()
                                ),
                            this,
                            button -> {
                                if (containers.size() > 1) {
                                    SlotSelectScreen slotSelectScreen = new SlotSelectScreen(containers, this);
                                    this.minecraft.setScreen(slotSelectScreen);
                                } else {
                                    this.acquireSkillTo(containers.iterator().next());
                                }
                            },
                            button -> {
                                this.minecraft.setScreen(this.parentScreen);
                            },
                            180,
                            0
                        ).setLayerFarPlane(2000).autoCalculateHeight()
                    );

                    return;
                }
            }

            this.minecraft.setScreen(this.parentScreen);
        }
    }

	@Override
	protected boolean consumesItem() {
		return false;
	}
	
	@OnlyIn(Dist.CLIENT)
	private class LearnButton extends Button {
		protected static final WidgetSprites SPRITES = new WidgetSprites(
	        ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "widget/skillbook_button"),
	        ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "widget/skillbook_button_disabled"),
	        ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "widget/skillbook_button_highlighted")
	    );
		
		private final Tooltip customTooltip;
		
		protected LearnButton(Builder builder) {
			super(builder);
			
			this.customTooltip = this.getTooltip() != null ? this.getTooltip() : null;
			this.setTooltip(null);
		}
		
		@Override
		protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
			Minecraft minecraft = Minecraft.getInstance();
			guiGraphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			
			if (this.isHoveredOrFocused() && this.customTooltip != null && !SkillInfoScreen.this.backgroundMode) {
				guiGraphics.renderTooltip(font, this.customTooltip.toCharSequence(minecraft), mouseX, mouseY);
			}
			
			guiGraphics.pose().pushPose();
			guiGraphics.blitSprite(SPRITES.get(this.active, this.isHoveredOrFocused()), this.getX(), this.getY(), this.getWidth(), this.getHeight());
			guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			guiGraphics.pose().popPose();
			
			int i = this.getFGColor();
			this.renderString(guiGraphics, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
		}
	}
}
