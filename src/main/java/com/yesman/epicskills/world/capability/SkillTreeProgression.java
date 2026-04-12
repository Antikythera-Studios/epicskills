package com.yesman.epicskills.world.capability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.client.gui.components.toasts.SkillTreeNodeToast;
import com.yesman.epicskills.client.gui.components.toasts.SkillTreeToast;
import com.yesman.epicskills.client.gui.screen.SkillInfoScreen;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundDeallocateAbilityPoints;
import com.yesman.epicskills.network.client.ClientBoundSetTreeState;
import com.yesman.epicskills.network.client.ClientBoundUnlockAchievedNode;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.registry.entry.EpicSkillsSkillTrees;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.skilltree.SkillTreeEntry;
import com.yesman.epicskills.skilltree.SkillTreeEntry.Node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import yesman.epicfight.api.data.reloader.SkillManager;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillTreeProgression {
	public static final Capability<SkillTreeProgression> SKILL_TREE_PROGRESSION = CapabilityManager.get(new CapabilityToken<> () {});
	
	private final RegistryAccess registryAccess;
	private final Map<Holder.Reference<SkillTree>, TreeState> treeStates = new HashMap<> ();
	private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> nodes = new HashMap<> ();
	private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> rootNodes = new HashMap<> ();
	private final List<Pair<Holder.Reference<SkillTree>, TopDownTreeNode>> unlockAwaitingNodes = new LinkedList<> ();
	
	/// Nodes that custom conditions are unlocked
	private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> achievedNodes = new HashMap<> ();
	
	private final Player player;
	
	public SkillTreeProgression(RegistryAccess registryAccess, Player player) {
		this.registryAccess = registryAccess;
		this.player = player;
		
		this.reload(false);
	}
	
	public void reload(boolean readOldData) {
		CompoundTag compound = null;
		
		if (readOldData) {
			compound = new CompoundTag();
			this.serializeTo(compound);
		} else {
			this.achievedNodes.clear();
		}
		
		this.treeStates.clear();
		this.nodes.clear();
		this.rootNodes.clear();
		this.unlockAwaitingNodes.clear();
		
		HolderLookup<SkillTree> skillTreeRegistry = this.registryAccess.lookupOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
		
		skillTreeRegistry.listElements().forEach(skillTree -> {
			this.treeStates.put(skillTree, skillTree.get().locked() ? TreeState.LOCKED : TreeState.UNLOCKED);
			this.nodes.put(skillTree, new LinkedHashMap<> ());
			this.rootNodes.put(skillTree, new HashMap<> ());
			this.achievedNodes.putIfAbsent(skillTree, new HashMap<>());
		});
		
		List<ImportedNode> importedNodes = new ArrayList<> ();
		
		this.registryAccess.registryOrThrow(SkillTreeEntry.SKILL_TREE_ENTRY_REGISTRY_KEY)
			.holders()
			.sorted((h1, h2) -> SkillTreeEntry.comparator(h1.get(), h2.get()))
			.forEach(skillTreeEntryHolder -> {
				Holder.Reference<SkillTree> skillTree = skillTreeRegistry.get(ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, skillTreeEntryHolder.key().location())).orElse(null);
				
				if (skillTree == null) {
					EpicSkills.LOGGER.warn("Unknown skill tree id: " + skillTreeEntryHolder.key().location());
					return;
				}
				
				if (skillTree.get().disabled()) {
					EpicSkills.LOGGER.info(skillTreeEntryHolder.key().location() + " is disabled.");
					return;
				}
				
				Map<Skill, TopDownTreeNode> nodesBySkill = this.nodes.get(skillTree);
				
				skillTreeEntryHolder.get().nodes().forEach(treeNode -> {
					if (!treeNode.skill().getCategory().learnable()) {
						EpicSkills.LOGGER.warn("Skill doesn't belong to a learnable skill category!" + treeNode.skill() + " in " + skillTree.key().location() + ". ignored.");
						return;
					}
					
					if (nodesBySkill.containsKey(treeNode.skill())) {
						EpicSkills.LOGGER.warn("Duplicated skill declaration! " + treeNode.skill() + " in " + skillTree.key().location() + ". ignored.");
						return;
					}
					
					TopDownTreeNode node;
					
					if (treeNode.importFrom() != null) {
						ImportedNode importedNode = new ImportedNode(skillTree, treeNode);
						importedNodes.add(importedNode);
						node = importedNode;
					} else {
						node = new CommonTreeNode(skillTree, treeNode);
					}
					
					if (treeNode.parents() != null) {
						treeNode.parents().forEach(parentLink -> {
							if (nodesBySkill.containsKey(parentLink.parentSkill())) {
								TopDownTreeNode parentNode = nodesBySkill.get(parentLink.parentSkill());
								parentNode.children().add(node);
								node.parents().add(parentNode);
							} else {
								EpicSkills.LOGGER.warn("Can't find parent skill " + parentLink.parentSkill() + " in " + skillTree.key().location() + ". ignored.");
							}
						});
					} else {
						if (node.nodeInfo.noUnlockConditions() || this.achievedNodes.get(skillTree).containsKey(node.nodeInfo.skill())) {
							node.setNodeState(NodeState.UNLOCKABLE, false, false);
						}
						
						this.rootNodes.get(skillTree).put(treeNode.skill(), node);
					}
					
					nodesBySkill.put(treeNode.skill(), node);
				});
			});
		
		importedNodes.forEach(importedNode -> {
			if (!this.nodes.containsKey(importedNode.getImportedTree())) {
				EpicSkills.LOGGER.warn("No skill tree page: " + importedNode.getImportedTree());
				return;
			}
			
			Map<Skill, TopDownTreeNode> pageNodes = this.nodes.get(importedNode.getImportedTree());
			
			if (!pageNodes.containsKey(importedNode.nodeInfo().skill())) {
				EpicSkills.LOGGER.warn("No skill " + importedNode.nodeInfo().skill() + " in skill tree page: " + importedNode.getImportedTree());
				return;
			}
			
			TopDownTreeNode importedOriginalNode = pageNodes.get(importedNode.nodeInfo().skill());
			
			importedOriginalNode.children().addAll(importedNode.children());
			importedNode.children().forEach(importedNodeChild -> {
				importedNodeChild.parents().add(importedOriginalNode);
			});
		});
		
		if (readOldData) {
			this.deserializeFrom(compound);
		}
	}
	
	/**
	 * Make all nodes locked (exclude conditional lock) and return all ability points
	 */
	public void deallocateAbilityPoints(boolean unequipSkills) {
        int allocatedPoints = 0;

        for (Map<Skill, TopDownTreeNode> pageNodes : this.nodes.values()) {
            for (TopDownTreeNode node : pageNodes.values()) {
                if (!node.isImported() && node.nodeState() == NodeState.UNLOCKED) {
                    allocatedPoints += node.nodeInfo().requiredAbilityPoints();
                    node.setNodeState(NodeState.LOCKED, false, unequipSkills);
                }
            }
        }

        if (allocatedPoints > 0) {
            AbilityPoints abilityPoints = AbilityPoints.getAbilityPoints(player).orElse(null);

            if (abilityPoints != null) {
                abilityPoints.setAbilityPoints(abilityPoints.getAbilityPoints() + allocatedPoints);
            }
        }
        
        if (!player.level().isClientSide()) {
        	NetworkManager.sendToPlayer(new ClientBoundDeallocateAbilityPoints(true), (ServerPlayer)player);
        }
	}
	
	public void tick() {
		if (this.player.level().isClientSide()) {
			return;
		}
		
		ServerPlayer serverplayer = (ServerPlayer)this.player;
		
		this.treeStates.forEach((tree, state) -> {
			if (state == TreeState.LOCKED && !tree.get().noUnlcokConditions()) {
				if (tree.get().conditions().matches(serverplayer, serverplayer)) {
					this.treeStates.put(tree, TreeState.UNLOCKED);
					NetworkManager.sendToPlayer(new ClientBoundSetTreeState(tree.key(), TreeState.UNLOCKED, false), serverplayer);
				}
			}
		});
		
		this.unlockAwaitingNodes.removeIf(pair -> {
			boolean meets = pair.getSecond().nodeInfo().unlockCondition().matches(serverplayer, serverplayer);
			
			if (meets) {
				this.achievedNodes.get(pair.getFirst()).put(pair.getSecond().nodeInfo().skill(), pair.getSecond());
				pair.getSecond().setNodeState(NodeState.UNLOCKABLE, true, false);
				NetworkManager.sendToPlayer(new ClientBoundUnlockAchievedNode(pair.getFirst().key(), pair.getSecond().nodeInfo().skill(), NodeState.UNLOCKABLE, true, false, false, false), serverplayer);
			}
			
			return meets;
		});
	}
	
	public void unlockTree(Holder.Reference<SkillTree> skillTree) {
		this.treeStates.put(skillTree, TreeState.UNLOCKED);
		
		if (!this.player.level().isClientSide()) {
			NetworkManager.sendToPlayer(new ClientBoundSetTreeState(skillTree.key(), TreeState.UNLOCKED, true), (ServerPlayer)this.player);
		}
	}
	
	public boolean canLockTree(Holder.Reference<SkillTree> skillTree) {
		boolean anyUnlocked = false;
		
		for (TopDownTreeNode node : this.rootNodes.get(skillTree).values()) {
			anyUnlocked |= node.nodeState() == NodeState.UNLOCKED;
		}
		
		return !anyUnlocked;
	}
	
	public void lockTree(Holder.Reference<SkillTree> skillTree, boolean unequip) {
		this.treeStates.put(skillTree, TreeState.LOCKED);
		
		this.rootNodes.get(skillTree).values().forEach(node -> {
			this.lockNode(skillTree, node.nodeInfo.skill(), unequip);
		});
		
		if (!this.player.level().isClientSide()) {
			NetworkManager.sendToPlayer(new ClientBoundSetTreeState(skillTree.key(), TreeState.LOCKED, unequip), (ServerPlayer)this.player);
		}
	}
	
	public boolean canUnlockNode(Holder.Reference<SkillTree> skillTree, Skill skill, AbilityPoints abilityPoints, boolean consume) {
		if (this.treeStates.get(skillTree) != TreeState.UNLOCKED) {
			return false;
		}
		
		Map<Skill, TopDownTreeNode> treeNodes = this.nodes.get(skillTree);
		
		if (treeNodes != null) {
			TopDownTreeNode treeNode = treeNodes.get(skill);
			
			if (treeNode.nodeState() != NodeState.UNLOCKABLE) {
				return treeNode.nodeState() == NodeState.UNLOCKED;
			}
			
			boolean hasEnoughAP = abilityPoints.getAbilityPoints() >= treeNode.nodeInfo().requiredAbilityPoints();
			
			if (hasEnoughAP && consume) {
				abilityPoints.setAbilityPoints(abilityPoints.getAbilityPoints() - treeNode.nodeInfo().requiredAbilityPoints());
			}
			
			abilityPoints.markDirty();
			
			return hasEnoughAP;
		}
		
		return false;
	}
	
	public void unlockNode(Holder.Reference<SkillTree> skillTree, Skill skill) {
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(skill);
		node.setNodeState(NodeState.UNLOCKED, true, false);
		
		if (!node.nodeInfo().noUnlockConditions()) {
            this.achievedNodes.get(skillTree).put(skill, node);
        }
	}
	
	public boolean canLockNode(Holder.Reference<SkillTree> skillTree, Skill skill) {
		if (this.treeStates.get(skillTree) != TreeState.UNLOCKED) {
			return false;
		}
		
		Map<Skill, TopDownTreeNode> treeNodes = this.nodes.get(skillTree);
		
		if (treeNodes != null) {
			TopDownTreeNode treeNode = treeNodes.get(skill);
			
			if (treeNode.nodeState() == NodeState.LOCKED) {
				return false;
			}
			
			boolean childAllLocked = true;
			
			for (TopDownTreeNode childNode : treeNode.children) {
				childAllLocked &= (childNode.nodeState() == NodeState.LOCKED || childNode.nodeState() == NodeState.UNLOCKABLE);
			}
			
			return childAllLocked;
		}
		
		return false;
	}
	
	public void lockNode(Holder.Reference<SkillTree> skillTree, Skill skill, boolean unequip) {
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(skill);
		node.setNodeState(NodeState.LOCKED, true, unequip);
	}
	
	@OnlyIn(Dist.CLIENT)
	public void processSyncPacket(ClientBoundSetTreeState packet) {
		Registry<SkillTree> registry = this.registryAccess.registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
		Holder.Reference<SkillTree> skillTree = registry.getHolderOrThrow(packet.skillTree());
		
		if (this.treeStates.get(skillTree) == TreeState.LOCKED && packet.treeState() == TreeState.UNLOCKED) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
			Minecraft.getInstance().getToasts().addToast(new SkillTreeToast(skillTree));
		} else if (this.treeStates.get(skillTree) == TreeState.UNLOCKED && packet.treeState() == TreeState.LOCKED) {
			this.rootNodes.get(skillTree).values().forEach(node -> {
				this.lockNode(skillTree, node.nodeInfo.skill(), packet.unequip());
			});
		}
		
		this.treeStates.put(skillTree, packet.treeState());
	}
	
	@OnlyIn(Dist.CLIENT)
	public void processSyncPacket(ClientBoundUnlockNode packet) {
		Registry<SkillTree> registry = this.registryAccess.registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
		Holder.Reference<SkillTree> skillTree = registry.getHolderOrThrow(packet.skillTree());
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(packet.skill());
		
		node.setNodeState(packet.nodeState(), true, packet.unequip());
		
		if (packet.nodeState() == NodeState.UNLOCKED && packet.unlockAlarm()) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
			Minecraft.getInstance().getToasts().addToast(new SkillTreeNodeToast(packet.skill()));
		}
		
		if (Minecraft.getInstance().screen instanceof SkillInfoScreen skillInfoScreen) {
			skillInfoScreen.onSyncPacketArrived(packet);
		}
	}
	
	@OnlyIn(Dist.CLIENT)
	public void processSyncPacket(ClientBoundUnlockAchievedNode packet) {
		Registry<SkillTree> registry = this.registryAccess.registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
		Holder.Reference<SkillTree> skillTree = registry.getHolderOrThrow(packet.skillTree());
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(packet.skill());
		
		node.setNodeState(packet.nodeState(), true, packet.unequip());
		
		if (packet.nodeState() == NodeState.UNLOCKED && packet.unlockAlarm()) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
			Minecraft.getInstance().getToasts().addToast(new SkillTreeNodeToast(packet.skill()));
		}
		
		if (Minecraft.getInstance().screen instanceof SkillInfoScreen skillInfoScreen) {
			skillInfoScreen.onSyncPacketArrived(packet);
		}
		
		this.achievedNodes.get(skillTree).put(packet.skill(), node);
	}
	
	public TreeState getTreeState(Holder.Reference<SkillTree> skillTree) {
		return this.treeStates.get(skillTree);
	}
	
	public NodeState getNodeState(Holder.Reference<SkillTree> skillTree, Skill skill) {
		Map<Skill, TopDownTreeNode> treeNodes = this.nodes.get(skillTree);
		
		if (!treeNodes.containsKey(skill)) {
			throw new NoSuchElementException("The skill " + skill + " doesn't exist in the skill tree " + skillTree.key().location());
		}
		
		return treeNodes.get(skill).nodeState();
	}
	
	public Map<Skill, TopDownTreeNode> getNodes(Holder<SkillTree> skillTree) {
		return this.nodes.get(skillTree);
	}
	
	public Map<Skill, TopDownTreeNode> getAchievedNodes(Holder<SkillTree> skillTree) {
		return this.achievedNodes.get(skillTree);
	}
	
	/**********************************************************************************************************
	 * State checking methods by ResourceKey
	 * @param skillTreeId {@link EpicSkillsSkillTrees} or a custom constant class contains skill tree pages' id
	 **********************************************************************************************************/
	/**
	 * Return true when no skills unlocked in the given skill tree's id
	 */
	public boolean canLockTree(ResourceKey<SkillTree> skillTreeId) {
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		return this.canLockTree(holder);
	}
	
	/**
	 * Check player's ability points, and states of parent nodes
	 */
	public boolean canUnlockNode(ResourceKey<SkillTree> skillTreeId, Skill skill, AbilityPoints abilityPoints, boolean consume) {
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		return this.canUnlockNode(holder, skill, abilityPoints, consume);
	}
	
	/**
	 * Check states of child nodes
	 */
	public boolean canLockNode(ResourceKey<SkillTree> skillTreeId, Skill skill) {
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		return this.canLockNode(holder, skill);
	}
	/*****************************
	 * State checking methods end
	 *****************************/
	
	/*******************************************************************************************************************
	 * Synchronization methods to handle node's states in a skill tree
	 * 
	 * @param skillTreeId {@link EpicSkillsSkillTrees} or a custom constant class contains skill tree pages' id
	 * @param serverplayer to be sure this method is called in server side
	 * @throws IllegalStateException throws exception when it can't find matching skill tree for the given skill tree id
	 *******************************************************************************************************************/
	public void unlockTree(ResourceKey<SkillTree> skillTreeId, ServerPlayer serverplayer) throws IllegalStateException {
		Holder.Reference<SkillTree> holder = serverplayer.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		this.unlockTree(holder);
	}
	
	/**
	 * @param unequip: Remove all skills belong to the tree from skill containers if the skill is equipped
	 */
	public void lockTree(ResourceKey<SkillTree> skillTreeId, boolean unequip, ServerPlayer serverplayer) throws IllegalStateException {
		Holder.Reference<SkillTree> holder = serverplayer.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		this.lockTree(holder, unequip);
	}
	
	public void unlockNode(ResourceKey<SkillTree> skillTreeId, Skill skill, ServerPlayer serverplayer) throws IllegalStateException {
		Holder.Reference<SkillTree> holder = serverplayer.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		this.unlockNode(holder, skill);

		SkillTreeProgression.TopDownTreeNode treeNode = this.getNodes(holder).get(skill);

        if (treeNode.nodeInfo().noUnlockConditions()) {
        	NetworkManager.sendToPlayer(new ClientBoundUnlockNode(skillTreeId, skill, NodeState.UNLOCKED, false, false, false, false), serverplayer);
        } else {
        	NetworkManager.sendToPlayer(new ClientBoundUnlockAchievedNode(skillTreeId, skill, NodeState.UNLOCKED, false, false, false, false), serverplayer);
        }
	}
	
	/**
	 * @param unequip: Remove skill from a skill container if the skill is equipped
	 */
	public void lockNode(ResourceKey<SkillTree> skillTreeId, Skill skill, boolean unequip, ServerPlayer serverplayer) throws IllegalStateException {
		Holder.Reference<SkillTree> holder = serverplayer.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(skillTreeId);
		
		this.lockNode(holder, skill, unequip);
		NetworkManager.sendToPlayer(new ClientBoundUnlockNode(skillTreeId, skill, NodeState.LOCKED, false, unequip, false, false), serverplayer);
	}
	/*****************
	 * Sync methods end
	 *****************/
	
	public abstract class TopDownTreeNode {
		protected final Holder.Reference<SkillTree> belongedSkillTree;
		protected final List<TopDownTreeNode> parent = new ArrayList<> ();
		protected final List<TopDownTreeNode> children = new ArrayList<> ();
		protected final Node nodeInfo;
		
		public TopDownTreeNode(Holder.Reference<SkillTree> belongedSkillTree, Node nodeInfo) {
			this.belongedSkillTree = belongedSkillTree;
			this.nodeInfo = nodeInfo;
		}
		
		public List<TopDownTreeNode> parents() {
			return this.parent;
		}
		
		public List<TopDownTreeNode> children() {
			return this.children;
		}
		
		public Node nodeInfo() {
			return this.nodeInfo;
		}
		
		public abstract boolean isImported();
		
		public abstract NodeState nodeState();
		
		protected abstract void setNodeState(NodeState nodeState, boolean propagateChildState, boolean modifyEquip);
	}
	
	public class CommonTreeNode extends TopDownTreeNode {
		private NodeState nodeState;
		
		public CommonTreeNode(Reference<SkillTree> belongedSkillTree, Node nodeInfo) {
			super(belongedSkillTree, nodeInfo);
			
			this.nodeState = NodeState.LOCKED;
		}
		
		@Override
		public boolean isImported() {
			return false;
		}
		
		@Override
		public void setNodeState(NodeState nodeState, boolean propagateState, boolean modifyEquip) {
			if (nodeState == this.nodeState) {
				return;
			}
			
			if (propagateState && nodeState == NodeState.UNLOCKED) {
				this.parents().forEach(parentNode -> {
					if (parentNode.nodeState() == NodeState.UNLOCKABLE || parentNode.nodeState() == NodeState.LOCKED) {
						parentNode.setNodeState(NodeState.UNLOCKED, true, modifyEquip);
					}
				});
			}
			
			if (nodeState == NodeState.UNLOCKABLE || nodeState == NodeState.LOCKED) {
				
			}
			
			if (nodeState == NodeState.LOCKED) {
				boolean parentAllUnlocked = true;
				
				if (!this.parents().isEmpty()) {
					for (TopDownTreeNode parentNode : this.parents()) {
						parentAllUnlocked &= parentNode.nodeState() == NodeState.UNLOCKED;
					}
				}
				
				if (parentAllUnlocked) {
					if (this.nodeInfo().noUnlockConditions() || SkillTreeProgression.this.achievedNodes.get(this.belongedSkillTree).containsKey(this.nodeInfo.skill())) {
						this.nodeState = NodeState.UNLOCKABLE;
					} else {
						this.nodeState = NodeState.LOCKED;
						if (!this.nodeInfo().hasCustomUnlockCondition()) SkillTreeProgression.this.unlockAwaitingNodes.add(Pair.of(this.belongedSkillTree, this));
					}
				} else {
					this.nodeState = NodeState.LOCKED;
				}
				
				if (modifyEquip) {
					EpicFightCapabilities.<Player, PlayerPatch<Player>>getParameterizedEntityPatch(player, Player.class, PlayerPatch.class).ifPresent(playerptach -> {
						playerptach.getSkillContainerFor(this.nodeInfo().skill()).ifPresent(container -> {
							container.setSkill(null);
						});
					});
				}
			} else {
				this.nodeState = nodeState;
			}
			
			if (propagateState) {
				switch (nodeState) {
				case LOCKED -> {
					this.children().forEach(childNode -> {
						boolean parentAllUnlocked = true;
						
						if (!childNode.parents().isEmpty()) {
							for (TopDownTreeNode parentNode : childNode.parents()) {
								parentAllUnlocked &= parentNode.nodeState() == NodeState.UNLOCKED;
							}
						}
						
						if (parentAllUnlocked) {
							if (childNode.nodeInfo().noUnlockConditions() || SkillTreeProgression.this.achievedNodes.get(childNode.belongedSkillTree).containsKey(childNode.nodeInfo.skill())) {
								childNode.setNodeState(NodeState.UNLOCKABLE, true, modifyEquip);
							} else {
								childNode.setNodeState(NodeState.LOCKED, true, modifyEquip);
								if (!childNode.nodeInfo().hasCustomUnlockCondition()) SkillTreeProgression.this.unlockAwaitingNodes.add(Pair.of(childNode.belongedSkillTree, childNode));
							}
						} else {
							childNode.setNodeState(NodeState.LOCKED, true, modifyEquip);
						}
					});
				}
				case UNLOCKED -> {
					this.children().forEach(childNode -> {
						boolean parentAllUnlocked = true;
						
						if (!childNode.parents().isEmpty()) {
							for (TopDownTreeNode parentNode : childNode.parents()) {
								parentAllUnlocked &= parentNode.nodeState() == NodeState.UNLOCKED;
							}
						}
						
						if (parentAllUnlocked) {
							if (childNode.nodeInfo.noUnlockConditions() || SkillTreeProgression.this.achievedNodes.get(childNode.belongedSkillTree).containsKey(childNode.nodeInfo.skill())) {
								childNode.setNodeState(NodeState.UNLOCKABLE, true, modifyEquip);
							} else if (!childNode.nodeInfo.hasCustomUnlockCondition()) {
								if (!childNode.nodeInfo().hasCustomUnlockCondition()) SkillTreeProgression.this.unlockAwaitingNodes.add(Pair.of(childNode.belongedSkillTree, childNode));
							}
						}
					});
				}
				case UNLOCKABLE -> {
					this.parents().forEach(parentNode -> {
						if (parentNode.nodeState() != NodeState.UNLOCKED) {
							parentNode.setNodeState(NodeState.UNLOCKED, true, modifyEquip);
						}
					});
					
					this.children().forEach(childNode -> {
						if (childNode.nodeState() != NodeState.LOCKED) {
							childNode.setNodeState(NodeState.LOCKED, true, modifyEquip);
						}
					});
				}
				}
			}
		}
		
		@Override
		public NodeState nodeState() {
			return this.nodeState;
		}
	}
	
	public class ImportedNode extends TopDownTreeNode {
		private final Holder.Reference<SkillTree> importedFrom;
		
		public ImportedNode(Reference<SkillTree> belongedSkillTree, Node nodeInfo) {
			super(belongedSkillTree, nodeInfo);
			
			this.importedFrom = SkillTreeProgression.this.registryAccess.lookupOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, nodeInfo.importFrom()));
		}
		
		@Override
		public boolean isImported() {
			return true;
		}
		
		public Holder.Reference<SkillTree> getImportedTree() {
			return this.importedFrom;
		}
		
		@Override
		public NodeState nodeState() {
			return SkillTreeProgression.this.getNodeState(this.importedFrom, this.nodeInfo().skill());
		}
		
		@Override
		public void setNodeState(NodeState nodeState, boolean propagateChildState, boolean modifyEquip) {
			// do nothing
		}
	}
	
	public enum TreeState {
		LOCKED, UNLOCKED
	}
	
	public enum NodeState {
		LOCKED(Component.translatable("gui." + EpicSkills.MODID + ".skillinfo.locked")),
		UNLOCKABLE(Component.translatable("gui." + EpicSkills.MODID + ".skillinfo.unlock")),
		UNLOCKED(Component.translatable("gui." + EpicSkills.MODID + ".skillinfo.equip"));
		
		Component displayOnButton;
		
		private NodeState(Component displayOnButton) {
			this.displayOnButton = displayOnButton;
		}
		
		public Component displayedOnButton() {
			return this.displayOnButton;
		}
	}
	
	public static void addNodesRecursively(TopDownTreeNode topDownNode, ListTag listTag) {
		CompoundTag compound = new CompoundTag();
		compound.putString("name", topDownNode.nodeInfo().skill().getRegistryName().toString());
		compound.putString("state", ParseUtil.toLowerCase(topDownNode.nodeState().name()));
		ListTag children = new ListTag();
		
		topDownNode.children().forEach(childNode -> {
			if (topDownNode.belongedSkillTree.equals(childNode.belongedSkillTree)) {
				addNodesRecursively(childNode, children);
			}
		});
		
		if (!children.isEmpty()) {
			compound.put("children", children);
		}
		
		listTag.add(compound);
	}
	
	public void deserializeRecursively(Holder.Reference<SkillTree> skilltree, Skill skill, TopDownTreeNode node, CompoundTag nodeCompound, boolean root) {
		if (!node.nodeInfo().skill().equals(skill)) {
			return;
		}
		
		NodeState treeState;
		
		try {
			treeState = NodeState.valueOf(ParseUtil.toUpperCase(nodeCompound.getString("state")));
		} catch (IllegalArgumentException e) {
			return;
		}
		
		node.setNodeState(treeState, false, false);
		
		if (treeState == NodeState.LOCKED) {
			boolean parentAllUnlocked = true;
			
			if (!node.parents().isEmpty()) {
				for (TopDownTreeNode parentNode : node.parents()) {
					parentAllUnlocked &= parentNode.nodeState() == NodeState.UNLOCKED;
				}
			}
			
			if (parentAllUnlocked && node.nodeInfo().unlockCondition() != null && !node.nodeInfo().hasCustomUnlockCondition()) {
				this.unlockAwaitingNodes.add(Pair.of(node.belongedSkillTree, node));
			}
		}
		
		ListTag children = nodeCompound.getList("children", Tag.TAG_COMPOUND);
		
		if (!node.children().isEmpty() && !children.isEmpty()) {
			node.children().forEach(childNode -> {
				for (Tag tag : children) {
					CompoundTag childCompound = (CompoundTag)tag;
					Skill childSkill = SkillManager.getSkill(childCompound.getString("name"));
					
					if (childNode.nodeInfo().skill().equals(childSkill)) {
						this.deserializeRecursively(skilltree, childSkill, childNode, childCompound, false);
						break;
					}
				}
			});
		}
	}
	
	public void serializeTo(CompoundTag compound) {
		for (Map.Entry<Holder.Reference<SkillTree>, TreeState> entry : this.treeStates.entrySet()) {
			CompoundTag treeCompound = new CompoundTag();
			treeCompound.putString("state", ParseUtil.toLowerCase(entry.getValue().name()));
			compound.put(entry.getKey().key().location().toString(), treeCompound);
			
			Collection<TopDownTreeNode> rootNodes = this.rootNodes.get(entry.getKey()).values();
			ListTag children = new ListTag();
			
			rootNodes.forEach(rootNode -> {
				addNodesRecursively(rootNode, children);
			});
			
			treeCompound.put("nodes", children);
			ListTag achievedNodes = new ListTag();
			
			if (!this.achievedNodes.get(entry.getKey()).isEmpty()) {
				for (TopDownTreeNode node : this.achievedNodes.get(entry.getKey()).values()) {
					achievedNodes.add(StringTag.valueOf(node.nodeInfo().skill().getRegistryName().toString()));
				}
			}
			
			if (!achievedNodes.isEmpty()) {
				treeCompound.put("achieved", achievedNodes);
			}
		}
	}
	
	public void deserializeFrom(CompoundTag compound) {
		HolderLookup<SkillTree> holderLookup = this.registryAccess.lookupOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
		
		for (String treeId : compound.getAllKeys()) {
			Holder.Reference<SkillTree> skilltree = holderLookup.get(ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, ResourceLocation.parse(treeId))).orElse(null);
			
			if (skilltree == null) {
				EpicSkills.LOGGER.warn("Unknown skill tree id " + treeId);
				continue;
			}
			
			CompoundTag treeCompound = compound.getCompound(treeId);
			
			if (treeCompound.contains("state", Tag.TAG_STRING)) {
				try {
					TreeState treeState = TreeState.valueOf(ParseUtil.toUpperCase(treeCompound.getString("state")));
					this.treeStates.put(skilltree, treeState);
				} catch (IllegalArgumentException e) {
				}
			}
			
			if (treeCompound.contains("nodes", Tag.TAG_LIST)) {
				ListTag unlockedSkills = treeCompound.getList("nodes", Tag.TAG_COMPOUND);
				
				unlockedSkills.forEach(nodeTag -> {
					Map<Skill, TopDownTreeNode> treeRootNodes = this.rootNodes.get(skilltree);
					CompoundTag nodeCompound = (CompoundTag)nodeTag;
					Skill skill = SkillManager.getSkill(nodeCompound.getString("name"));
					
					if (skill == null) {
						EpicSkills.LOGGER.warn("Skill tree deserialization failed: Unknown root skill id: " + nodeCompound.getString("name"));
						return;
					}
					
					if (treeRootNodes.containsKey(skill)) {
						this.deserializeRecursively(skilltree, skill, treeRootNodes.get(skill), nodeCompound, true);
					}
				});
			}
			
			if (treeCompound.contains("achieved", Tag.TAG_LIST)) {
				ListTag listTag = treeCompound.getList("achieved", Tag.TAG_STRING);
				
				for (Tag tag : listTag) {
					Skill skill = SkillManager.getSkill(tag.getAsString());
					
					Map<Skill, TopDownTreeNode> achievedNodes = this.achievedNodes.get(skilltree);
					if (this.nodes.get(skilltree).containsKey(skill)) achievedNodes.put(skill, this.nodes.get(skilltree).get(skill));
				}
			}
		}
	}

    /*********************
     *   Provider part   *
     *********************/
    private static final ResourceLocation SKILL_TREE_PROGRESSION_CAPABILITY_KEY = EpicSkills.identifier("skill_tree_progression");
	
	public static void epicskills$attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject().getType() == EntityType.PLAYER && !event.getCapabilities().containsKey(SKILL_TREE_PROGRESSION_CAPABILITY_KEY)) {
			event.addCapability(SKILL_TREE_PROGRESSION_CAPABILITY_KEY, new SkillTreeProgression.Provider(new SkillTreeProgression(event.getObject().level().registryAccess(), (Player)event.getObject())));
		}
	}
	
	public static class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
		private final LazyOptional<SkillTreeProgression> lazyOptional;
		private final SkillTreeProgression skillTreeProgression;
		
		public Provider(@NonNull SkillTreeProgression skillTreeProgression) {
			this.lazyOptional = LazyOptional.of(() -> skillTreeProgression);
			this.skillTreeProgression = skillTreeProgression;
		}
		
		@Override
		public CompoundTag serializeNBT() {
			CompoundTag compound = new CompoundTag();
			this.skillTreeProgression.serializeTo(compound);
			
			return compound;
		}
		
		@Override
		public void deserializeNBT(CompoundTag compound) {
			this.skillTreeProgression.deserializeFrom(compound);
		}
		
		@Override
		public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			return cap == SKILL_TREE_PROGRESSION ? this.lazyOptional.cast() : LazyOptional.empty();
		}
	}
}
