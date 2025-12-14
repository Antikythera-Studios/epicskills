package com.yesman.epicskills.skilltree;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.util.JsonUtil;

import net.minecraft.Util;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.skill.Skill;

public record SkillTreeEntry(List<Node> nodes, int workPriority) {
	public static final Codec<Vec2i> VEC2_INT_CODEC = Codec.INT.listOf()
		.comapFlatMap(instance -> {
			return Util.fixedSize(instance, 2).map(result -> {
				return new Vec2i(result.get(0), result.get(1));
			});
		},
		vec2 -> {
			return List.of(vec2.x, vec2.y);
		}
	);
	
	public static int comparator(SkillTreeEntry e1, SkillTreeEntry e2) {
		return Integer.compare(e2.workPriority(), e1.workPriority());
	}
	
	public static final ResourceKey<Registry<SkillTreeEntry>> SKILL_TREE_ENTRY_REGISTRY_KEY = ResourceKey.createRegistryKey(EpicSkills.identifier("entry"));
	
	public static final Codec<SkillTreeEntry> CODEC = RecordCodecBuilder.create(instance -> 
		instance.group(
			Node.CODEC.listOf().optionalFieldOf("nodes").forGetter(skillTreeEntry -> Optional.ofNullable(skillTreeEntry.nodes())),
			Codec.INT.optionalFieldOf("priority").forGetter(skillTreeEntry -> Optional.of(skillTreeEntry.workPriority()))
		)
		.apply(instance, (nodes, workPriorityOpt) -> {
			return new SkillTreeEntry(nodes.orElse(List.of()), workPriorityOpt.orElse(0));
		})
	);
	
	public static record ParentLink(Skill parentSkill, @Nullable List<Vec2i> controlPoints) {
		public static final Codec<ParentLink> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				SkillManager.CODEC.fieldOf("skill").forGetter(ParentLink::parentSkill),
				SkillTreeEntry.VEC2_INT_CODEC.listOf().optionalFieldOf("control_points").forGetter(object -> Optional.ofNullable(object.controlPoints()))
			)
			.apply(instance, (skill, controlPointsOpt) -> new ParentLink(skill, controlPointsOpt.orElse(null)))
		);
	}
	
	public static record Node(Skill skill, @Nullable List<ParentLink> parents, @Nullable EntityPredicate unlockCondition, boolean hasCustomUnlockCondition, @Nullable Component unlockTip, int requiredAbilityPoints, Vec2i positionInScreen, boolean hidden, @Nullable ResourceLocation importFrom) {
		public static final Codec<Node> CODEC = RecordCodecBuilder.create(instance ->
			instance.group(
				SkillManager.CODEC.fieldOf("skill").forGetter(Node::skill),
				ParentLink.CODEC.listOf().optionalFieldOf("parents").forGetter(node -> Optional.ofNullable(node.parents())),
				ExtraCodecs.JSON.optionalFieldOf("conditions").forGetter(node -> {
					JsonElement serialized = node.unlockCondition() == null ? null : JsonUtil.removeNullElements(node.unlockCondition().serializeToJson());
					return Optional.ofNullable(serialized);
				}),
				Codec.BOOL.optionalFieldOf("custom_condition").forGetter(node -> Optional.ofNullable(node.hasCustomUnlockCondition())),
				Codec.STRING.optionalFieldOf("unlock_tip").forGetter(node -> node.unlockTip() == null ? Optional.empty() : Optional.of(((TranslatableContents)((MutableComponent)node.unlockTip()).getContents()).getKey())),
				Codec.INT.optionalFieldOf("ability_points").forGetter(node -> Optional.ofNullable(node.requiredAbilityPoints())),
				VEC2_INT_CODEC.fieldOf("position_in_screen").forGetter(Node::positionInScreen),
				Codec.BOOL.optionalFieldOf("hidden").forGetter(node -> Optional.ofNullable(node.hidden())),
				ResourceLocation.CODEC.optionalFieldOf("import").forGetter(node -> Optional.ofNullable(node.importFrom()))
			)
			.apply(instance, (skill, parentOpt, conditions, hasCustomCondition, unlockTip, requiredAbilityPoints, positionInScreen, hiddenOpt, importFromOpt) -> {
				JsonElement entityPredicatesJson = conditions.orElse(null);
				EntityPredicate entityPredicates = entityPredicatesJson == null ? null : EntityPredicate.fromJson(entityPredicatesJson);
				return new Node(skill, parentOpt.orElse(null), entityPredicates, hasCustomCondition.orElse(false), unlockTip.isPresent() ? Component.translatable(unlockTip.get()) : null, requiredAbilityPoints.orElse(0), positionInScreen, hiddenOpt.orElse(false), importFromOpt.orElse(null));
			})
		);
		
		public boolean is(Skill skill) {
			return this.skill.equals(skill);
		}
		
		public boolean noUnlockConditions() {
			return this.unlockCondition == null && !this.hasCustomUnlockCondition;
		}
	}
}