package com.yesman.epicskills.server.commands;

import java.util.Collection;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundReloadSkillTree;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.world.capability.AbilityPoints;
import com.yesman.epicskills.world.capability.SkillTreeProgression;
import com.yesman.epicskills.world.capability.SkillTreeProgression.NodeState;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import yesman.epicfight.api.utils.MutableBoolean;
import yesman.epicfight.server.commands.arguments.SkillArgument;
import yesman.epicfight.skill.Skill;

public class PlayerSkillTreeCommand {
	private static final SimpleCommandExceptionType EXCEPTION_NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("commands." + EpicSkills.MODID + ".failed.no_players"));
	private static final SimpleCommandExceptionType EXCEPTION_UNABLE_TO_UNLOCK = new SimpleCommandExceptionType(Component.translatable("commands." + EpicSkills.MODID + ".skilltree_progression.unlock.failed"));
	private static final SimpleCommandExceptionType EXCEPTION_UNABLE_TO_LOCK = new SimpleCommandExceptionType(Component.translatable("commands." + EpicSkills.MODID + ".skilltree_progression.lock.failed"));
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext pContext) {
		dispatcher.register(
			Commands
				.literal("skilltree")
				.then(
					Commands
						.literal("reset")
						.then(
							Commands
								.argument("targets", EntityArgument.players())
								.executes(command -> {
									return proccessSkillTreeCommand(command, EntityArgument.getPlayers(command, "targets"), null, null, Action.RESET, true, false);
								})
						)
						.executes(command -> {
							return proccessSkillTreeCommand(command, ImmutableList.of(command.getSource().getPlayerOrException()), null, null, Action.RESET, true, false);
						})
				)
				.then(
					Commands
						.literal("unlock")
						.then(
							Commands
								.argument("targets", EntityArgument.players())
								.then(
									Commands
										.argument("skilltree", ResourceArgument.resource(pContext, SkillTree.SKILL_TREE_REGISTRY_KEY))
										.then(
											Commands
												.argument("skill", SkillArgument.skill())
												.then(
													Commands
														.argument("force", BoolArgumentType.bool())
														.executes(command -> {
															return proccessSkillTreeCommand(
																command,
																EntityArgument.getPlayers(command, "targets"),
																ResourceArgument.getResource(command, "skilltree", SkillTree.SKILL_TREE_REGISTRY_KEY),
																SkillArgument.getSkill(command, "skill"),
																Action.UNLOCK,
																BoolArgumentType.getBool(command, "force"),
																false
															);
														})
												)
												.executes(command -> {
													return proccessSkillTreeCommand(
														command,
														EntityArgument.getPlayers(command, "targets"),
														ResourceArgument.getResource(command, "skilltree", SkillTree.SKILL_TREE_REGISTRY_KEY),
														SkillArgument.getSkill(command, "skill"),
														Action.UNLOCK,
														false,
														false
													);
												})
										)
								)
						)
				)
				.then(
					Commands
						.literal("lock")
						.then(
							Commands
								.argument("targets", EntityArgument.players())
								.then(
									Commands
										.argument("skilltree", ResourceArgument.resource(pContext, SkillTree.SKILL_TREE_REGISTRY_KEY))
										.then(
											Commands
												.argument("skill", SkillArgument.skill())
												.then(
													Commands
														.argument("force", BoolArgumentType.bool())
														.then(
															Commands
																.argument("unequip", BoolArgumentType.bool())
																.executes(command -> {
																	return proccessSkillTreeCommand(
																		command,
																		EntityArgument.getPlayers(command, "targets"),
																		ResourceArgument.getResource(command, "skilltree", SkillTree.SKILL_TREE_REGISTRY_KEY),
																		SkillArgument.getSkill(command, "skill"),
																		Action.LOCK,
																		BoolArgumentType.getBool(command, "force"),
																		BoolArgumentType.getBool(command, "unequip")
																	);
																})
														)
														.executes(command -> {
															return proccessSkillTreeCommand(
																command,
																EntityArgument.getPlayers(command, "targets"),
																ResourceArgument.getResource(command, "skilltree", SkillTree.SKILL_TREE_REGISTRY_KEY),
																SkillArgument.getSkill(command, "skill"),
																Action.LOCK,
																BoolArgumentType.getBool(command, "force"),
																false
															);
														})
												)
												.executes(command -> {
													return proccessSkillTreeCommand(
														command,
														EntityArgument.getPlayers(command, "targets"),
														ResourceArgument.getResource(command, "skilltree", SkillTree.SKILL_TREE_REGISTRY_KEY),
														SkillArgument.getSkill(command, "skill"),
														Action.LOCK,
														false,
														false
													);
												})
										)
								)
						)
				)
		);
	}
	
	private static int proccessSkillTreeCommand(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, Action action, boolean force, boolean unequip) throws CommandSyntaxException {
		int done = 0;
		
		if (players.isEmpty()) {
			throw EXCEPTION_NO_PLAYERS_FOUND.create();
		}
		
		switch (action) {
		case RESET -> {
			for (ServerPlayer player : players) {
				if (resetTree(player)) {
					done++;
				}
			}
		}
		case UNLOCK -> {
			for (ServerPlayer player : players) {
				if (unlockTreeNode(player, skillTree, skill, force)) {
					done++;
				}
			}
		}
		case LOCK -> {
			for (ServerPlayer player : players) {
				if (lockTreeNode(player, skillTree, skill, force, unequip)) {
					done++;
				}
			}
		}
		}
		
		if (done == 0) {
			switch (action) {
			case RESET -> {
				throw EXCEPTION_NO_PLAYERS_FOUND.create();
			}
			case UNLOCK -> {
				throw EXCEPTION_UNABLE_TO_UNLOCK.create();
			}
			case LOCK -> {
				throw EXCEPTION_UNABLE_TO_LOCK.create();
			}
			}
		} else {
			if (done == 1) {
				command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".skilltree_progression." + action.id + ".success.single", action.singleTranslatableKeyParameters.parameters(command, players, skillTree, skill, done))), true);
			} else {
				for (ServerPlayer serverplayer : players) {
					if (command.getSource().getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
						serverplayer.sendSystemMessage(Component.translatable("commands." + EpicSkills.MODID + ".skilltree_progression." + action.id + ".success.other", action.multipleTranslatableKeyParameters.parameters(command, players, skillTree, skill, done)));
					}
				}
				
				command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".skilltree_progression." + action.id + ".success.other", action.multipleTranslatableKeyParameters.parameters(command, players, skillTree, skill, done))), true);
			}
		}
		
		return done;
	}
	
	private static boolean resetTree(ServerPlayer player) {
		player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			skillTreeProgression.reload(false);
			NetworkManager.sendToPlayer(new ClientBoundReloadSkillTree(false), player);
		});
		
		return player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).isPresent();
	}
	
	private static boolean unlockTreeNode(ServerPlayer player, Holder.Reference<SkillTree> skillTree, Skill skill, boolean force) {
		MutableBoolean succeess = new MutableBoolean(false);
		
		player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			player.getCapability(AbilityPoints.ABILITY_POINTS).ifPresent(abilityPoints -> {
				if (force || skillTreeProgression.canUnlockNode(skillTree, skill, abilityPoints, false)) {
					skillTreeProgression.unlockNode(skillTree, skill);
					NetworkManager.sendToPlayer(new ClientBoundUnlockNode(skillTree.key(), skill, NodeState.UNLOCKED, false, false, false), player);
					succeess.set(true);
				}
			});
		});
		
		return succeess.value();
	}
	
	private static boolean lockTreeNode(ServerPlayer player, Holder.Reference<SkillTree> skillTree, Skill skill, boolean force, boolean unequip) {
		MutableBoolean succeess = new MutableBoolean(false);
		
		player.getCapability(SkillTreeProgression.SKILL_TREE_PROGRESSION).ifPresent(skillTreeProgression -> {
			if (force || skillTreeProgression.canLockNode(skillTree, skill)) {
				skillTreeProgression.lockNode(skillTree, skill, unequip);
				NetworkManager.sendToPlayer(new ClientBoundUnlockNode(skillTree.key(), skill, NodeState.LOCKED, false, unequip, false), player);
				succeess.set(true);
			}
		});
		
		return succeess.value();
	}
	
	private static <T> Supplier<T> wrap(T value) {
		return () -> value;
	}
	
	public enum Action {
		RESET(
			"reset",
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { players.iterator().next().getDisplayName() };
			},
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { String.valueOf(successCount) };
			}
		),
		UNLOCK(
			"unlock",
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { Component.translatable(skill.getTranslationKey()).getString(),  Component.translatable(SkillTree.toDescriptionId(skillTree.key())).getString(), players.iterator().next().getDisplayName() };
			},
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { Component.translatable(skill.getTranslationKey()).getString(),  Component.translatable(SkillTree.toDescriptionId(skillTree.key())).getString(), String.valueOf(successCount) };
			}
		),
		LOCK(
			"lock",
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { Component.translatable(skill.getTranslationKey()).getString(),  Component.translatable(SkillTree.toDescriptionId(skillTree.key())).getString(), players.iterator().next().getDisplayName() };
			},
			(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount) -> {
				return new Object[] { Component.translatable(skill.getTranslationKey()).getString(),  Component.translatable(SkillTree.toDescriptionId(skillTree.key())).getString(), String.valueOf(successCount) };
			}
		);
		
		TranslatableKeyParameters singleTranslatableKeyParameters;
		TranslatableKeyParameters multipleTranslatableKeyParameters;
		String id;
		
		private Action(String id, TranslatableKeyParameters singleTranslatableKeyParameters, TranslatableKeyParameters multipleTranslatableKeyParameters) {
			this.id = id;
			this.singleTranslatableKeyParameters = singleTranslatableKeyParameters;
			this.multipleTranslatableKeyParameters = multipleTranslatableKeyParameters;
		}
	}
	
	@FunctionalInterface
	public interface TranslatableKeyParameters {
		public Object[] parameters(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, @Nullable Holder.Reference<SkillTree> skillTree, @Nullable Skill skill, int successCount);
	}
}
