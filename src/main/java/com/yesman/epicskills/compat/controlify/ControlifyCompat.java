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
import dev.isxander.controlify.virtualmouse.VirtualMouseBehaviour;
import dev.isxander.controlify.virtualmouse.VirtualMouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class ControlifyCompat implements ControlifyEntrypoint {
    private static InputBindingSupplier openSkillTreeScreen;

    private static InputBindingSupplier moveSkillTreeUp;
    private static InputBindingSupplier moveSkillTreeDown;
    private static InputBindingSupplier moveSkillTreeLeft;
    private static InputBindingSupplier moveSkillTreeRight;

    private static InputBindingSupplier scaleSkillTreeUp;
    private static InputBindingSupplier scaleSkillTreeDown;

    private static InputBindingSupplier navigateSkillTreeNext;
    private static InputBindingSupplier navigateSkillTreePrev;

    private static InputBindingSupplier openSkillEditor;
    private static InputBindingSupplier convertXpToAbilityPoint;

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
        registrar.registerBindContext(EpicSkillsBindContext.IN_SKILL_TREE);
        registerInputBindings(registrar);
        registerScreenProcessors();
    }

    private enum EpicSkillsRadialIcons {
        ABILITY_STONE(EpicSkills.identifier("textures/item/ability_stone.png"));

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

            // For consistency with the current Controlify radial icons,
            // this code is equivalent to:
            // https://github.com/isXander/Controlify/blob/f5c94c57d5e0d4954e413624a0d7ead937b6e8ab/src/main/java/dev/isxander/controlify/bindings/RadialIcons.java#L106-L112
            RadialIcons.registerIcon(location, (graphics, x, y, tickDelta) -> {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0);
                graphics.pose().scale(0.5f, 0.5f, 1f);
                Blit.blitTex(graphics, location, 0, 0, 0, 0, 32, 32, 32, 32);
                graphics.pose().popPose();
            });
        }
    }

    private static class EpicSkillsBindContext {
        private static final BindContext IN_SKILL_TREE = new BindContext(
                EpicSkills.identifier("in_skill_tree"),
                mc -> mc.screen instanceof SkillTreeScreen
        );
    }

    private static void registerInputBindings(ControlifyBindApi registrar) {
        final Component guiCategory = Component.translatable("key.epicfight.gui");
        openSkillTreeScreen = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("attack"))
                        .category(guiCategory)
                        .allowedContexts(BindContext.IN_GAME)
                        .name(Component.translatable("key.epicskills.open_skill_tree"))
                        .description(Component.translatable("key.epicskills.open_skill_tree.description"))
                        .addKeyCorrelation(EpicSkillsKeyMappings.OPEN_SKILL_TREE)
                        .keyEmulation(EpicSkillsKeyMappings.OPEN_SKILL_TREE)
                        .radialCandidate(EpicSkillsRadialIcons.ABILITY_STONE.getId())
        );

        moveSkillTreeUp = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("move_skill_tree_up"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.move_skill_tree_up"))
                        .description(Component.translatable("controller.epicskills.move_skill_tree_up.description"))
        );
        moveSkillTreeDown = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("move_skill_tree_down"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.move_skill_tree_down"))
                        .description(Component.translatable("controller.epicskills.move_skill_tree_down.description"))
        );
        moveSkillTreeLeft = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("move_skill_tree_left"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.move_skill_tree_left"))
                        .description(Component.translatable("controller.epicskills.move_skill_tree_left.description"))
        );
        moveSkillTreeRight = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("move_skill_tree_right"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.move_skill_tree_right"))
                        .description(Component.translatable("controller.epicskills.move_skill_tree_right.description"))
        );

        scaleSkillTreeUp = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("scale_skill_tree_up"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.scale_skill_tree_up"))
                        .description(Component.translatable("controller.epicskills.scale_skill_tree_up.description"))
        );
        scaleSkillTreeDown = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("scale_skill_tree_down"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.scale_skill_tree_down"))
                        .description(Component.translatable("controller.epicskills.scale_skill_tree_down.description"))
        );

        navigateSkillTreeNext = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("navigate_skill_tree_next"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.navigate_skill_tree_next"))
                        .description(Component.translatable("controller.epicskills.navigate_skill_tree_next.description"))
        );
        navigateSkillTreePrev = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("navigate_skill_tree_prev"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.navigate_skill_tree_prev"))
                        .description(Component.translatable("controller.epicskills.navigate_skill_tree_prev.description"))
        );

        openSkillEditor = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("open_skill_editor"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.open_skill_editor"))
                        .description(Component.translatable("controller.epicskills.open_skill_editor.description"))
        );
        convertXpToAbilityPoint = registrar.registerBinding(
                builder -> builder.id(EpicSkills.identifier("convert_xp_to_ability_point"))
                        .category(guiCategory)
                        .allowedContexts(EpicSkillsBindContext.IN_SKILL_TREE)
                        .name(Component.translatable("controller.epicskills.convert_xp_to_ability_point"))
                        .description(Component.translatable("controller.epicskills.convert_xp_to_ability_point.description"))
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

        @Override
        protected void handleScreenVMouse(ControllerEntity controller, VirtualMouseHandler vmouse) {
            super.handleScreenVMouse(controller, vmouse);

            handleViewportMove(controller, vmouse);
            handleViewportScale(controller);
            handleTreeNavigation(controller);
            handleOtherActions(controller);
        }

        private void handleViewportMove(@NotNull ControllerEntity controller, @NotNull VirtualMouseHandler vmouse) {
            // Ignores "ControlifyBindings.VMOUSE_SCROLL_DOWN" and "ControlifyBindings.VMOUSE_SCROLL_UP" inputs,
            // since the left thumb stick is used for handle moving the skill tree viewpoint.
            vmouse.preventScrollingThisTick();

            final float up = moveSkillTreeUp.on(controller).analogueNow();
            final float down = moveSkillTreeDown.on(controller).analogueNow();
            final float right = moveSkillTreeRight.on(controller).analogueNow();
            final float left = moveSkillTreeLeft.on(controller).analogueNow();

            float horizontal = right - left;
            float vertical = up - down;

            // Dead zone to prevent small jitter
            final float deadZone = 0.1f;
            if (Math.abs(horizontal) < deadZone) horizontal = 0.0f;
            if (Math.abs(vertical) < deadZone) vertical = 0.0f;

            final float sensitivity = 10.0f;
            final float deltaX = horizontal * sensitivity;
            final float deltaY = vertical * sensitivity;

            this.screen.moveViewport(
                    // Flip horizontal delta because increasing "pageLeft" moves the viewport left,
                    // but positive horizontal input should move the viewport right.
                    deltaX * -1,
                    deltaY
            );
        }

        private void handleViewportScale(@NotNull ControllerEntity controller) {
            final boolean isUp = isPressed(scaleSkillTreeUp, controller);
            final boolean isDown = isPressed(scaleSkillTreeDown, controller);
            if (isUp || isDown) {
                if (isUp) {
                    this.screen.scaleUp();
                } else {
                    this.screen.scaleDown();
                }
                playClackSound();
            }
        }

        private void handleTreeNavigation(@NotNull ControllerEntity controller) {
            final boolean isNext = isPressed(navigateSkillTreeNext, controller);
            final boolean isPrev = isPressed(navigateSkillTreePrev, controller);

            if (isNext || isPrev) {
                final boolean navigated = this.screen.navigateTreePage(isNext);
                if (navigated) {
                    SkillTreeScreen.playSkillTreeDownSound(Minecraft.getInstance().getSoundManager());
                }
            }
        }

        private void handleOtherActions(@NotNull ControllerEntity controller) {
            if (isPressed(openSkillEditor, controller)) {
                this.screen.openSkillEditorScreen();
                playClackSound();
            } else if (isPressed(convertXpToAbilityPoint, controller)) {
                this.screen.getExpConversionButton().convert();
            }
        }

        private boolean isPressed(@NotNull InputBindingSupplier binding, @NotNull ControllerEntity controller) {
            return binding.on(controller).guiPressed().get();
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
                playClackSound();
            }
            super.handleButtons(controller);
        }

        // The Skill info screen has a single actionable button (the "equip skill" button).
        // Controller navigation and focus are disabled, and only the primary controller
        // button (e.g., X on DualSense) is used to trigger the action.

        @Override
        protected void setInitialFocus() {
            // Intentionally empty. Do NOT call super.setInitialFocus().
        }

        @Override
        protected void handleComponentNavigation(ControllerEntity controller) {
            // Intentionally empty. Do NOT call super.handleComponentNavigation().
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