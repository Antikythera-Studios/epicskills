package com.yesman.epicskills.compat.controlify;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.client.gui.screen.SkillInfoScreen;
import com.yesman.epicskills.client.gui.screen.SkillTreeScreen;
import com.yesman.epicskills.client.input.EpicSkillsKeyMappings;
import dev.isxander.controlify.InputMode;
import dev.isxander.controlify.api.ControlifyApi;
import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.api.entrypoint.ControlifyEntrypoint;
import dev.isxander.controlify.api.entrypoint.InitContext;
import dev.isxander.controlify.api.entrypoint.PreInitContext;
import dev.isxander.controlify.bindings.BindContext;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.bindings.RadialIcons;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.screenop.ScreenProcessor;
import dev.isxander.controlify.screenop.ScreenProcessorProvider;
import dev.isxander.controlify.utils.render.Blit;
import dev.isxander.controlify.utils.render.CGuiPose;
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ControlifyCompat implements ControlifyEntrypoint {
    private static InputBindingSupplier openSkillTreeScreen;

    @Override
    public void onControllersDiscovered(ControlifyApi controlify) {

    }

    @Override
    public void onControlifyInit(InitContext context) {

    }

    @Override
    public void onControlifyPreInit(PreInitContext context) {
        final ControlifyBindApi registrar = ControlifyBindApi.get();
        registerCustomRadialIcons();
        registerInputBindings(registrar);
        registerScreenProcessors();
    }

    private enum EpicSkillsRadialIcons {
        ABILITY_STONE(EpicSkills.rl("textures/item/ability_stone.png"));

        private final @NotNull ResourceLocation id;

        EpicSkillsRadialIcons(@NotNull ResourceLocation id) {
            this.id = id;
        }

        public @NotNull ResourceLocation getId() {
            return id;
        }
    }

    private static void registerCustomRadialIcons() {
        for (EpicSkillsRadialIcons icon : EpicSkillsRadialIcons.values()) {
            final ResourceLocation location = icon.getId();
            RadialIcons.registerIcon(location, (graphics, x, y, tickDelta) -> {
                var pose = CGuiPose.ofPush(graphics);
                pose.translate(x, y);
                pose.scale(0.5f, 0.5f);
                Blit.tex(graphics, location, 0, 0, 0, 0, 32, 32, 32, 32);
                pose.pop();
            });
        }
    }

    private static void registerInputBindings(ControlifyBindApi registrar) {
        final Component guiCategory = Component.translatable("key.epicfight.gui");
        openSkillTreeScreen = registrar.registerBinding(
                builder -> builder.id(EpicSkills.rl("attack"))
                        .category(guiCategory)
                        .allowedContexts(BindContext.IN_GAME)
                        .name(Component.translatable("key.epicskills.open_skill_tree"))
                        .description(Component.translatable("key.epicskills.open_skill_tree.description"))
                        .addKeyCorrelation(EpicSkillsKeyMappings.OPEN_SKILL_TREE)
                        .keyEmulation(EpicSkillsKeyMappings.OPEN_SKILL_TREE)
                        .radialCandidate(EpicSkillsRadialIcons.ABILITY_STONE.getId())
        );
    }

    private static void registerScreenProcessors() {
        ScreenProcessorProvider.registerProvider(
                SkillTreeScreen.class,
                SkillTreeScreenProcessor::new
        );
        ScreenProcessorProvider.registerProvider(
                SkillInfoScreen.class,
                SkillInfoScreenProcessor::new
        );
    }

    private static class SkillTreeScreenProcessor extends ScreenProcessor<SkillTreeScreen> {
        public SkillTreeScreenProcessor(SkillTreeScreen screen) {
            super(screen);
        }

        @Override
        public void onControllerUpdate(ControllerEntity controller) {
            super.onControllerUpdate(controller);
            updateDisableMouseDragging(ControlifyApi.get().currentInputMode());
        }

        @Override
        public VirtualMouseBehaviour virtualMouseBehaviour() {
            // The skill tree screen does not natively support controllers.
            // To save development time, we work around this issue by enforcing the virtual mouse.
            return VirtualMouseBehaviour.ENABLED;
        }

        @Override
        public void onInputModeChanged(InputMode mode) {
            super.onInputModeChanged(mode);
            // Important to allow dragging when switching to a keyboard
            updateDisableMouseDragging(mode);
        }

        private void updateDisableMouseDragging(InputMode mode) {
            screen.setDisableMouseDragging(mode.isController());
        }
    }

    private static class SkillInfoScreenProcessor extends ScreenProcessor<SkillInfoScreen> {
        public SkillInfoScreenProcessor(SkillInfoScreen screen) {
            super(screen);
        }

        private static final InputBindingSupplier ACTION = ControlifyBindings.GUI_PRESS;

        @Override
        protected void handleButtons(ControllerEntity controller) {
            if (ACTION.on(controller).guiPressed().get()) {
                screen.getActionButton().onPress();
            }
            super.handleButtons(controller);
        }

        @Override
        protected void setInitialFocus() {
            // No-op
        }

        @Override
        protected void handleComponentNavigation(ControllerEntity controller) {
            // No-op
        }

        @Override
        public void onWidgetRebuild() {
            super.onWidgetRebuild();

            ButtonGuideApi.addGuideToButton(
                    this.screen.getActionButton(),
                    ACTION,
                    ButtonGuidePredicate.always()
            );
        }
    }
}
