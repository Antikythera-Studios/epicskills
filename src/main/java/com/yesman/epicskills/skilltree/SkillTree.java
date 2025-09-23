package com.yesman.epicskills.skilltree;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.util.JsonUtil;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public record SkillTree(Vec3i menuBarColor, @Nullable EntityPredicate conditions, @Nullable Component unlockTip, boolean locked, boolean hiddenWhenLocked, boolean disabled, int priority) {
	public static final ResourceKey<Registry<SkillTree>> SKILL_TREE_REGISTRY_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(EpicSkills.MODID, "tree"));
	
	public static final Codec<SkillTree> CODEC = RecordCodecBuilder.create(instance -> 
		instance.group(
			Vec3i.CODEC.optionalFieldOf("menu_color").forGetter(skilltree -> Optional.ofNullable(skilltree.menuBarColor())),
			ExtraCodecs.JSON.optionalFieldOf("conditions").forGetter(skilltree -> {
				JsonElement serialized = skilltree.conditions() == null ? null : JsonUtil.removeNullElements(skilltree.conditions().serializeToJson());
				return Optional.ofNullable(serialized);
			}),
			Codec.STRING.optionalFieldOf("unlock_tip").forGetter(node -> node.unlockTip() == null ? Optional.empty() : Optional.of(((TranslatableContents)((MutableComponent)node.unlockTip()).getContents()).getKey())),
			Codec.BOOL.optionalFieldOf("locked").forGetter(skilltree -> Optional.ofNullable(skilltree.locked())),
			Codec.BOOL.optionalFieldOf("hidden").forGetter(skilltree -> Optional.ofNullable(skilltree.hiddenWhenLocked())),
			Codec.BOOL.optionalFieldOf("disabled").forGetter(skilltree -> Optional.ofNullable(skilltree.disabled())),
			Codec.INT.optionalFieldOf("priority").forGetter(skilltree -> Optional.ofNullable(skilltree.priority()))
		)
		.apply(instance, (menuBarColorOpt, conditions, unlockTip, lockedOpt, hiddenWhenLockedOpt, disabledOpt, priorityOpt) -> {
			JsonElement entityPredicatesJson = conditions.orElse(null);
			EntityPredicate entityPredicates = entityPredicatesJson == null ? null : EntityPredicate.fromJson(entityPredicatesJson);
			return new SkillTree(menuBarColorOpt.orElse(new Vec3i(255, 255, 255)), entityPredicates, unlockTip.isPresent() ? Component.translatable(unlockTip.get()) : null, lockedOpt.orElse(false), hiddenWhenLockedOpt.orElse(false), disabledOpt.orElse(false), priorityOpt.orElse(100));
		})
	);
	
	public static String toDescriptionId(ResourceKey<SkillTree> resourceKey) {
		return String.format("skill_tree.%s.%s", resourceKey.location().getNamespace(), resourceKey.location().getPath());
	}
	
	public boolean noUnlcokConditions() {
		return this.conditions() == null;
	}
}
