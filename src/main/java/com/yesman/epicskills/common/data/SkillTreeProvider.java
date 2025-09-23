package com.yesman.epicskills.common.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.math.Vec2i;
import yesman.epicfight.skill.Skill;

public abstract class SkillTreeProvider implements DataProvider {
	protected final PackOutput.PathProvider skillTreePagePathProvider;
	protected final PackOutput.PathProvider skillTreeEntryPathProvider;
	
	public SkillTreeProvider(PackOutput pOutput) {
		this.skillTreePagePathProvider = pOutput.createPathProvider(PackOutput.Target.DATA_PACK, "epicskills/tree");
		this.skillTreeEntryPathProvider = pOutput.createPathProvider(PackOutput.Target.DATA_PACK, "epicskills/entry");
	}
	
	@Override
	public CompletableFuture<?> run(CachedOutput pOutput) {
		Set<ResourceLocation> registeredNames = new HashSet<> ();
		List<CompletableFuture<?>> tasks = new ArrayList<>();
		
		this.buildSkillTreePages(skillTreePageBuilder -> {
			if (!registeredNames.add(skillTreePageBuilder.name)) {
				throw new IllegalStateException("Duplicate skill tree page: " + skillTreePageBuilder.name);
			} else {
				tasks.add(DataProvider.saveStable(pOutput, skillTreePageBuilder.serializePage(), this.skillTreePagePathProvider.json(skillTreePageBuilder.name)));
				tasks.add(DataProvider.saveStable(pOutput, skillTreePageBuilder.serializeNodes(), this.skillTreeEntryPathProvider.json(skillTreePageBuilder.name)));
			}
		});
		
		return CompletableFuture.allOf(tasks.toArray((size) -> {
			return new CompletableFuture[size];
		}));
	}
	
	protected abstract void buildSkillTreePages(Consumer<SkillTreePageBuilder> writer);
	
	@Override
	public String getName() {
		return "Skill Trees";
	}
	
	protected static SkillTreePageBuilder newPage(String modid, String name) {
		return new SkillTreePageBuilder(ResourceLocation.fromNamespaceAndPath(modid, name));
	}
	
	protected static class SkillTreePageBuilder {
		final ResourceLocation name;
		final Map<Skill, SkillTreeNodeBuilder> nodes = new LinkedHashMap<> ();
		
		Vec3i menuBarColor = new Vec3i(255, 255, 255);
		@Nullable
		EntityPredicate conditions = null;
		@Nullable
		String unlockTipTranslationKey;
		boolean locked = false;
		boolean hiddenWhenLocked = false;
		
		public SkillTreePageBuilder(ResourceLocation name) {
			this.name = name;
		}
		
		public JsonObject serializePage() {
			JsonObject jsonObject = new JsonObject();
			JsonArray menuColor = new JsonArray();
			menuColor.add(this.menuBarColor.getX());
			menuColor.add(this.menuBarColor.getY());
			menuColor.add(this.menuBarColor.getZ());
			
			jsonObject.add("menu_color", menuColor);
			jsonObject.addProperty("locked", this.locked);
			jsonObject.addProperty("hidden", this.hiddenWhenLocked);
			
			if (this.conditions != null) {
				jsonObject.add("conditions", this.conditions.serializeToJson());
			}
			
			if (this.unlockTipTranslationKey != null) jsonObject.addProperty("unlock_tip", this.unlockTipTranslationKey);
			
			return jsonObject;
		}
		
		public JsonObject serializeNodes() {
			JsonObject jsonObject = new JsonObject();
			JsonArray nodes = new JsonArray();
			
			for (Map.Entry<Skill, SkillTreeNodeBuilder> entry : this.nodes.entrySet()) {
				JsonObject node = new JsonObject();
				
				node.addProperty("skill", entry.getKey().getRegistryName().toString());
				
				if (!entry.getValue().parentSkill.isEmpty()) {
					JsonArray parents = new JsonArray();
					
					for (Pair<Skill, List<Vec2i>> parent : entry.getValue().parentSkill) {
						JsonObject parentObj = new JsonObject();
						parentObj.addProperty("skill", parent.getFirst().getRegistryName().toString());
						
						if (!parent.getSecond().isEmpty()) {
							JsonArray controlPoints = new JsonArray();
							
							for (Vec2i pos : parent.getSecond()) {
								JsonArray position = new JsonArray();
								position.add(pos.x);
								position.add(pos.y);
								controlPoints.add(position);
							}
							
							parentObj.add("control_points", controlPoints);
						}
						
						parents.add(parentObj);
					}
					
					node.add("parents", parents);
				}
				
				if (entry.getValue().unlockCondition != null) {
					node.add("conditions", entry.getValue().unlockCondition.serializeToJson());
					node.addProperty("custom_condition", entry.getValue().hasCustomUnlockCondition);
					if (entry.getValue().unlockTipTranslationKey != null) node.addProperty("unlock_tip", entry.getValue().unlockTipTranslationKey);
				}
				
				node.addProperty("ability_points", entry.getValue().abilityPointsRequirement);
				
				JsonArray positionInScreen = new JsonArray();
				positionInScreen.add(entry.getValue().positionInScreen.x);
				positionInScreen.add(entry.getValue().positionInScreen.y);
				node.add("position_in_screen", positionInScreen);
				node.addProperty("hidden", entry.getValue().hiddenWhenLocked);
				if (entry.getValue().importFrom != null) node.addProperty("import", entry.getValue().importFrom.toString());
				
				nodes.add(node);
			}
			
			jsonObject.add("nodes", nodes);
			
			return jsonObject;
		}
		
		/**
		 * A color of menu bar in the left side of the skill tree screen
		 */
		public SkillTreePageBuilder menuBarColor(int r, int g, int b) {
			this.menuBarColor = new Vec3i(r, g, b);
			return this;
		}
		
		/**
		 * A player predicate to unlock this page
		 */
		public SkillTreePageBuilder setLocked(EntityPredicate entityPredicate) {
			this.conditions = entityPredicate;
			this.locked = true;
			return this;
		}
		
		/**
		 * A .lang key to give a hint to unlock this node
		 */
		public SkillTreePageBuilder unlockTipTranslationKey(String unlockTipTranslationKey) {
			this.unlockTipTranslationKey = unlockTipTranslationKey;
			return this;
		}
		
		/**
		 * A page button will be hidden if this page is locked
		 */
		public SkillTreePageBuilder hiddenWhenLocked(boolean hiddenWhenLocked) {
			this.hiddenWhenLocked = hiddenWhenLocked;
			return this;
		}
		
		/**
		 * Make new entry, call {@link SkillTreeNodeBuilder#done} to get back to building page
		 */
		public SkillTreeNodeBuilder newNode(Skill skill) {
			SkillTreeNodeBuilder nodeBuilder = new SkillTreeNodeBuilder(skill);
			this.nodes.put(skill, nodeBuilder);
			return nodeBuilder;
		}
		
		public class SkillTreeNodeBuilder {
			final Skill skill;
			final List<Pair<Skill, List<Vec2i>>> parentSkill = new ArrayList<> ();
			@Nullable
			EntityPredicate unlockCondition;
			boolean hasCustomUnlockCondition;
			@Nullable
			String unlockTipTranslationKey;
			Vec2i positionInScreen;
			int abilityPointsRequirement = 1;
			boolean hiddenWhenLocked = false;
			@Nullable
			ResourceLocation importFrom;
			
			private SkillTreeNodeBuilder(Skill skill) {
				if (SkillTreePageBuilder.this.nodes.containsKey(skill)) {
					throw new IllegalStateException(skill.getRegistryName() + " already exists");
				}
				
				this.skill = skill;
				SkillTreePageBuilder.this.nodes.put(skill, this);
			}
			
			/**
			 * A player predicate to unlock this node
			 */
			public SkillTreeNodeBuilder unlockCondition(EntityPredicate unlockCondition) {
				this.unlockCondition = unlockCondition;
				return this;
			}
			
			/**
			 * Set true this if you want to unlock this node manually
			 */
			public SkillTreeNodeBuilder hasCustomUnlockCondition(boolean hasCustomUnlockCondition) {
				this.hasCustomUnlockCondition = hasCustomUnlockCondition;
				return this;
			}
			
			/**
			 * A .lang key to give a hint to unlock this node
			 */
			public SkillTreeNodeBuilder unlockTipTranslationKey(String unlockTipTranslationKey) {
				this.unlockTipTranslationKey = unlockTipTranslationKey;
				return this;
			}
			
			/**
			 * A position in the skill tree screen
			 */
			public SkillTreeNodeBuilder position(int x, int y) {
				this.positionInScreen = new Vec2i(x, y);
				return this;
			}
			
			/**
			 * An amount of ability points to unlock this node
			 */
			public SkillTreeNodeBuilder abilityPointsRequirement(int abilityPointsRequirement) {
				this.abilityPointsRequirement = abilityPointsRequirement;
				return this;
			}
			
			/**
			 * Determines if this node should be hidden when locked
			 */
			public SkillTreeNodeBuilder hiddenWhenLocked(boolean hiddenWhenLocked) {
				this.hiddenWhenLocked = hiddenWhenLocked;
				return this;
			}
			
			/**
			 * Give a tree page id if this node is from another skill tree page
			 */
			public SkillTreeNodeBuilder importFrom(ResourceLocation importFrom) {
				this.importFrom = importFrom;
				return this;
			}
			
			/**
			 * Add a parent node that should be unlocked first
			 */
			public SkillTreeNodeBuilder addParent(Skill skill, Vec2i... controlPoints) {
				if (this.skill == skill) {
					throw new IllegalArgumentException("Can't connect to myself");
				}
				
				if (!SkillTreePageBuilder.this.nodes.containsKey(skill)) {
					throw new IllegalStateException("Tried connecting to a " + skill.getRegistryName() + " node but nothing has found.");
				}
				
				this.parentSkill.add(Pair.of(skill, List.of(controlPoints)));
				
				return this;
			}
			
			/**
			 * Return to the page builder
			 */
			public SkillTreePageBuilder done() {
				return SkillTreePageBuilder.this;
			}
		}
	}
}
