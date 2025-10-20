package com.yesman.epicskills.server.commands;

import java.util.Collection;
import java.util.function.Supplier;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.neoforge.attachment.AbilityPoints;
import com.yesman.epicskills.registry.entry.EpicSkillsAttachmentTypes;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

public class PlayerAbilityPointsCommand {
	private static final SimpleCommandExceptionType EXCEPTION_NO_PLAYERS_FOUND = new SimpleCommandExceptionType(Component.translatable("commands." + EpicSkills.MODID + ".failed.no_players"));
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands
				.literal("abilitypoints")
				.requires(commandSourceStack -> commandSourceStack.hasPermission(2))
				.then(
					Commands
						.literal("get")
							.then(
								Commands
									.argument(
										"target",
										EntityArgument.player()
									)
									.executes(command -> {
										ServerPlayer player = EntityArgument.getPlayer(command, "target");
										AbilityPoints abilityPoints = player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).orElse(null);
										
										if (abilityPoints != null) {
											command.getSource().sendSuccess(() -> {
												return Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.value.get.success", player.getName(), String.valueOf(abilityPoints.getAbilityPoints()));
											}, false);
											
											return abilityPoints.getAbilityPoints();
										} else {
											command.getSource().sendFailure(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.value.get.fail"));
											return 0;
										}
									})
							)
				)
				.then(
					Commands
						.literal("set")
						.then(
							Commands
								.argument(
									"targets",
									EntityArgument.players()
								)
								.then(
									Commands
										.argument(
											"value",
											IntegerArgumentType.integer(0)
										)
										.executes(command -> {
											return processAbilityPointValueSet(command, EntityArgument.getPlayers(command, "targets"), IntegerArgumentType.getInteger(command, "value"), "set");
										})
								)
						)
				)
				.then(
					Commands
						.literal("add")
						.then(
							Commands
								.argument(
									"targets",
									EntityArgument.players()
								)
								.then(
									Commands
										.argument(
											"value",
											IntegerArgumentType.integer()
										)
										.executes(command -> {
											return processAbilityPointValueSet(command, EntityArgument.getPlayers(command, "targets"), IntegerArgumentType.getInteger(command, "value"), "add");
										})
								)
						)
				)
				.then(
					Commands
						.literal("reset_exp_require")
						.then(
							Commands
								.argument(
									"targets",
									EntityArgument.players()
								)
								.executes(command -> {
									return processExpReset(command, EntityArgument.getPlayers(command, "targets"));
								})
						)
				)
		);
	}
	
	private static int processExpReset(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players) throws CommandSyntaxException {
		int done = 0;
		
		for (ServerPlayer player : players) {
			if (resetExpRequirement(player)) {
				done++;
			}
		}
		
		if (done == 0) {
			throw EXCEPTION_NO_PLAYERS_FOUND.create();
		} else {
			if (done == 1) {
				command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.reset_exp_requirement.success.single", players.iterator().next().getDisplayName())), true);
			}
			else {
				for (ServerPlayer serverplayer : players) {
					if (command.getSource().getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
						serverplayer.sendSystemMessage(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.reset_exp_requirement.success.other", String.valueOf(done)));
					}
				}
				
				command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.reset_exp_requirement.success.other", String.valueOf(done))), true);
			}
		}
		
		return done;
	}
	
	private static int processAbilityPointValueSet(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, int value, String operation) throws CommandSyntaxException {
		int done = 0;
		
		for (ServerPlayer player : players) {
			switch (operation) {
			case "set" -> {
				if (setAbilityPoints(player, value)) {
					done++;
				}
			}
			case "add" -> {
				if (addAbilityPoints(player, value)) {
					done++;
				}
			}
			}
		}
		
		if (done == 0) {
			throw EXCEPTION_NO_PLAYERS_FOUND.create();
		} else {
			if (done == 1) {
				players.iterator().next().getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilitypoints -> {
					command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.success.single", players.iterator().next().getDisplayName(), String.valueOf(abilitypoints.getAbilityPoints()))), true);
				});
			} else {
				for (ServerPlayer serverplayer : players) {
					if (command.getSource().getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
						serverplayer.sendSystemMessage(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.success.other", String.valueOf(done)));
					}
				}
				
				command.getSource().sendSuccess(wrap(Component.translatable("commands." + EpicSkills.MODID + ".abilitypoints.success.other", String.valueOf(done))), true);
			}
		}
		
		return done;
	}
	
	private static boolean setAbilityPoints(ServerPlayer player, int value) {
		player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilitypoints -> {
			abilitypoints.setAbilityPoints(value);
			abilitypoints.markDirty();
		});
		
		return player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).isPresent();
	}
	
	private static boolean addAbilityPoints(ServerPlayer player, int value) {
		player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilitypoints -> {
			abilitypoints.setAbilityPoints(abilitypoints.getAbilityPoints() + value);
			abilitypoints.markDirty();
		});
		
		return player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).isPresent();
	}
	
	public static boolean resetExpRequirement(ServerPlayer player) {
		player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).ifPresent(abilitypoints -> {
			abilitypoints.setRequiredExp(AbilityPoints.INIT_EXP_REQUIREMENT);
			abilitypoints.markDirty();
		});
		
		return player.getExistingData(EpicSkillsAttachmentTypes.ABILITY_POINTS).isPresent();
	}
	
	private static <T> Supplier<T> wrap(T value) {
		return () -> value;
	}
}
