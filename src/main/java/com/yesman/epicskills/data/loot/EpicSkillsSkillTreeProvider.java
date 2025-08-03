package com.yesman.epicskills.data.loot;

import java.util.function.Consumer;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.common.data.SkillTreeProvider;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.PlayerPredicate;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.gameasset.EpicFightSkills;

public class EpicSkillsSkillTreeProvider extends SkillTreeProvider {
	public EpicSkillsSkillTreeProvider(PackOutput pOutput) {
		super(pOutput);
	}

	@Override
	protected void buildSkillTreePages(Consumer<SkillTreePageBuilder> writer) {
		writer.accept(
			newPage(EpicSkills.MODID, "battleborn")
				.menuBarColor(37, 27, 18)
				.newNode(EpicFightSkills.ROLL)
					.position(20, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.PHANTOM_ASCENT)
					.addParent(EpicFightSkills.ROLL)
					.position(20, 280)
					.abilityPointsRequirement(5)
				.done()
				.newNode(EpicFightSkills.STEP)
					.position(110, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.TECHNICIAN)
					.addParent(EpicFightSkills.STEP)
					.position(110, 130)
					.abilityPointsRequirement(2)
				.done()
				.newNode(EpicFightSkills.EMERGENCY_ESCAPE)
					.addParent(EpicFightSkills.ROLL, new Vec2i(20, 210))
					.addParent(EpicFightSkills.TECHNICIAN, new Vec2i(65, 130))
					.position(65, 210)
					.abilityPointsRequirement(4)
				.done()
				.newNode(EpicFightSkills.VENGEANCE)
					.position(200, 130)
					.abilityPointsRequirement(2)
				.done()
				.newNode(EpicFightSkills.BLOODLUST)
					.addParent(EpicFightSkills.VENGEANCE, new Vec2i(155, 130))
					.addParent(EpicFightSkills.TECHNICIAN, new Vec2i(155, 130))
					.position(155, 210)
					.abilityPointsRequirement(3)
				.done()
				.newNode(EpicFightSkills.SWORD_MASTER)
					.position(240, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.GUARD)
					.position(300, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.PARRYING)
					.addParent(EpicFightSkills.SWORD_MASTER)
					.addParent(EpicFightSkills.GUARD)
					.position(270, 130)
					.abilityPointsRequirement(3)
				.done()
				.newNode(EpicFightSkills.REVELATION)
					.addParent(EpicFightSkills.TECHNICIAN, new Vec2i(110, 280), new Vec2i(190, 280))
					.addParent(EpicFightSkills.PARRYING, new Vec2i(270, 280), new Vec2i(190, 280))
					.position(190, 330)
					.abilityPointsRequirement(6)
				.done()
				.newNode(EpicFightSkills.BERSERKER)
					.position(360, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.ADRENALINE_FIEND)
					.position(420, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.ENDURANCE)
					.addParent(EpicFightSkills.GUARD)
					.addParent(EpicFightSkills.BERSERKER)
					.position(330, 130)
					.abilityPointsRequirement(2)
				.done()
				.newNode(EpicFightSkills.IMPACT_GUARD)
					.addParent(EpicFightSkills.ENDURANCE)
					.position(330, 230)
					.abilityPointsRequirement(3)
				.done()
				.newNode(EpicFightSkills.HYPERVITALITY)
					.addParent(EpicFightSkills.BERSERKER)
					.addParent(EpicFightSkills.ADRENALINE_FIEND)
					.position(390, 130)
					.abilityPointsRequirement(3)
				.done()
				.newNode(EpicFightSkills.DEMOLITION_LEAP)
					.addParent(EpicFightSkills.HYPERVITALITY)
					.position(390, 230)
					.abilityPointsRequirement(5)
				.done()
				.newNode(EpicFightSkills.BONEBREAKER)
					.addParent(EpicFightSkills.HYPERVITALITY)
					.position(450, 230)
					.abilityPointsRequirement(3)
				.done()
				.newNode(EpicFightSkills.METEOR_STRIKE)
					.addParent(EpicFightSkills.IMPACT_GUARD, new Vec2i(330, 330))
					.addParent(EpicFightSkills.DEMOLITION_LEAP, new Vec2i(390, 330))
					.unlockCondition(
						EntityPredicate.Builder.entity()
							.subPredicate(
								PlayerPredicate.Builder.player()
									.addStat(Stats.ENTITY_KILLED.get(EntityType.ENDER_DRAGON), MinMaxBounds.Ints.atLeast(1))
								.build()
							)
						.build()
					)
					.unlockTipTranslationKey("unlock_tip.epicskills.battleborn.meteor_slam")
					.position(360, 330)
					.abilityPointsRequirement(6)
				.done()
		);
		
		writer.accept(
			newPage(EpicSkills.MODID, "infernal_might")
				.menuBarColor(81, 0, 1)
				.setLocked(
					EntityPredicate.Builder.entity()
						.located(
							LocationPredicate.Builder.location()
								.setDimension(Level.NETHER)
							.build()
						)
					.build()
				)
				.newNode(EpicFightSkills.STAMINA_PILLAGER)
					.position(40, 30)
					.abilityPointsRequirement(1)
				.done()
				.newNode(EpicFightSkills.DEATH_HARVEST)
					.addParent(EpicFightSkills.STAMINA_PILLAGER)
					.position(40, 110)
					.abilityPointsRequirement(2)
				.done()
				.newNode(EpicFightSkills.FORBIDDEN_STRENGTH)
					.position(120, 30)
					.abilityPointsRequirement(2)
				.done()
				.newNode(EpicFightSkills.ENDURANCE)
					.position(180, 30)
					.importFrom(ResourceLocation.fromNamespaceAndPath(EpicSkills.MODID, "battleborn"))
				.done()
				.newNode(EpicFightSkills.ADAPTIVE_SKIN)
					.addParent(EpicFightSkills.FORBIDDEN_STRENGTH)
					.addParent(EpicFightSkills.ENDURANCE)
					.position(150, 110)
					.abilityPointsRequirement(3)
				.done()
		);
	}
}
