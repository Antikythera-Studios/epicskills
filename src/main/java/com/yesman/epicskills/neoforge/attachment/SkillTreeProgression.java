package com.yesman.epicskills.neoforge.attachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.mojang.datafixers.util.Pair;
import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.client.gui.components.toasts.SkillTreeNodeToast;
import com.yesman.epicskills.client.gui.components.toasts.SkillTreeToast;
import com.yesman.epicskills.client.gui.screen.SkillInfoScreen;
import com.yesman.epicskills.network.client.ClientBoundSetTreeState;
import com.yesman.epicskills.network.client.ClientBoundUnlockAchievedNode;
import com.yesman.epicskills.network.client.ClientBoundUnlockNode;
import com.yesman.epicskills.skilltree.SkillTree;
import com.yesman.epicskills.skilltree.SkillTreeEntry;
import com.yesman.epicskills.skilltree.SkillTreeEntry.Node;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
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
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillTreeProgression {
	private final RegistryAccess registryAccess;
	private final Map<Holder.Reference<SkillTree>, TreeState> treeStates = new HashMap<> ();
	private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> nodes = new HashMap<> ();
	private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> rootNodes = new HashMap<> ();
	private final List<Pair<Holder.Reference<SkillTree>, TopDownTreeNode>> unlockAwaitingNodes = new LinkedList<> ();

    /// Nodes that custom conditions are unlocked
    private final Map<Holder.Reference<SkillTree>, Map<Skill, TopDownTreeNode>> achievedNodes = new HashMap<> ();

    private final Player player;
	
	public SkillTreeProgression(IAttachmentHolder attachmentHolder) {
		if (attachmentHolder instanceof Player player) {
			this.registryAccess = player.registryAccess();
			this.player = player;
		} else {
			throw new IllegalArgumentException(attachmentHolder + " is not a subtype of Player");
		}
		
		this.reload(false);
	}

    /// @param readOldData  Reload all skills after resetting the skill tree. Used by syncing datapack registry changes.
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
			this.treeStates.put(skillTree, skillTree.value().locked() ? TreeState.LOCKED : TreeState.UNLOCKED);
			this.nodes.put(skillTree, new LinkedHashMap<> ());
			this.rootNodes.put(skillTree, new HashMap<> ());
            this.achievedNodes.putIfAbsent(skillTree, new HashMap<>());
		});
		
		List<ImportedNode> importedNodes = new ArrayList<> ();
		
		this.registryAccess.registryOrThrow(SkillTreeEntry.SKILL_TREE_ENTRY_REGISTRY_KEY)
			.holders()
			.sorted((h1, h2) -> SkillTreeEntry.comparator(h1.value(), h2.value()))
			.forEach(skillTreeEntryHolder -> {
				Holder.Reference<SkillTree> skillTree = skillTreeRegistry.get(ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, skillTreeEntryHolder.key().location())).orElse(null);
				
				if (skillTree == null) {
					EpicSkills.LOGGER.warn("Unknown skill tree id: " + skillTreeEntryHolder.key().location());
					return;
				}
				
				if (skillTree.value().disabled()) {
					EpicSkills.LOGGER.info(skillTreeEntryHolder.key().location() + " is disabled.");
					return;
				}
				
				Map<Skill, TopDownTreeNode> nodesBySkill = this.nodes.get(skillTree);
				
				skillTreeEntryHolder.value().nodes().forEach(treeNode -> {
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

    /// Make all nodes locked (exclude conditional lock) and return all ability points
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
    }


    public void tick() {
		if (this.player.level().isClientSide()) {
			return;
		}
		
		ServerPlayer serverplayer = (ServerPlayer)this.player;
		
		EpicFightNetworkManager.PayloadBundleBuilder payloadsbuilder = EpicFightNetworkManager.PayloadBundleBuilder.create();
		
		this.treeStates.forEach((tree, state) -> {
			if (state == TreeState.LOCKED && !tree.value().noUnlcokConditions()) {
				if (tree.value().conditions().matches(serverplayer, serverplayer)) {
					this.treeStates.put(tree, TreeState.UNLOCKED);
					payloadsbuilder.and(new ClientBoundSetTreeState(tree.key(), TreeState.UNLOCKED, false));
				}
			}
		});

		this.unlockAwaitingNodes.removeIf(pair -> {
			boolean meets = pair.getSecond().nodeInfo().unlockCondition().matches(serverplayer, serverplayer);

            if (meets) {
                this.achievedNodes.get(pair.getFirst()).put(pair.getSecond().nodeInfo().skill(), pair.getSecond());
                pair.getSecond().setNodeState(NodeState.UNLOCKABLE, true, false);
				payloadsbuilder.and(new ClientBoundUnlockAchievedNode(pair.getFirst().key(), pair.getSecond().nodeInfo().skill().holder(), NodeState.UNLOCKABLE, true, false, false, false));
            }

			return meets;
		});
		
		payloadsbuilder.send((first, others) -> {
			EpicFightNetworkManager.sendToPlayer(first, serverplayer, others);
		});
	}
	
	public void unlockTree(ResourceLocation id) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		
		this.unlockTree(holder);
	}
	
	public void unlockTree(Holder.Reference<SkillTree> skillTree) {
		this.treeStates.put(skillTree, TreeState.UNLOCKED);
		
		if (!this.player.level().isClientSide()) {
			EpicFightNetworkManager.sendToPlayer(new ClientBoundSetTreeState(skillTree.key(), TreeState.UNLOCKED, true), (ServerPlayer)this.player);
		}
	}
	
	public void canLockTree(ResourceLocation id) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		this.canLockTree(holder);
	}
	
	public boolean canLockTree(Holder.Reference<SkillTree> skillTree) {
		boolean anyUnlocked = false;
		
		for (TopDownTreeNode node : this.rootNodes.get(skillTree).values()) {
			anyUnlocked |= node.nodeState() == NodeState.UNLOCKED;
		}
		
		return !anyUnlocked;
	}
	
	public void lockTree(ResourceLocation id, boolean unequip) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		this.lockTree(holder, unequip);
	}
	
	public void lockTree(Holder.Reference<SkillTree> skillTree, boolean unequip) {
		this.treeStates.put(skillTree, TreeState.LOCKED);
		
		this.rootNodes.get(skillTree).values().forEach(node -> {
			this.lockNode(skillTree, node.nodeInfo.skill(), unequip);
		});
		
		if (!this.player.level().isClientSide()) {
			EpicFightNetworkManager.sendToPlayer(new ClientBoundSetTreeState(skillTree.key(), TreeState.LOCKED, unequip), (ServerPlayer)this.player);
		}
	}
	
	public boolean canUnlockNode(ResourceLocation id, Skill skill, AbilityPoints abilityPoints, boolean consume) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		
		return this.canUnlockNode(holder, skill, abilityPoints, consume);
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
	
	public void unlockNode(ResourceLocation id, Skill skill) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		
		this.unlockNode(holder, skill);
	}
	
	public void unlockNode(Holder.Reference<SkillTree> skillTree, Skill skill) {
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(skill);
		node.setNodeState(NodeState.UNLOCKED, true, false);

        if (!node.nodeInfo().noUnlockConditions()) {
            this.achievedNodes.get(skillTree).put(skill, node);
        }
	}
	
	public boolean canLockNode(ResourceLocation id, Skill skill) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		
		return this.canLockNode(holder, skill);
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
	
	public void lockNode(ResourceLocation id, Skill skill, boolean unequip) {
		ResourceKey<SkillTree> rk = ResourceKey.create(SkillTree.SKILL_TREE_REGISTRY_KEY, id);
		Holder.Reference<SkillTree> holder = this.player.level().holderLookup(SkillTree.SKILL_TREE_REGISTRY_KEY).getOrThrow(rk);
		
		this.lockNode(holder, skill, unequip);
	}
	
	public void lockNode(Holder.Reference<SkillTree> skillTree, Skill skill, boolean unequip) {
		Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
		TopDownTreeNode node =  nodes.get(skill);
		node.setNodeState(NodeState.LOCKED, true, unequip);
	}

    @OnlyIn(Dist.CLIENT)
    public void processSyncPacket(ClientBoundUnlockAchievedNode packet) {
        Registry<SkillTree> registry = this.registryAccess.registryOrThrow(SkillTree.SKILL_TREE_REGISTRY_KEY);
        Holder.Reference<SkillTree> skillTree = registry.getHolderOrThrow(packet.skillTree());
        Map<Skill, TopDownTreeNode> nodes = this.nodes.get(skillTree);
        TopDownTreeNode node =  nodes.get(packet.skill().value());

        node.setNodeState(packet.nodeState(), true, packet.unequip());

        if (packet.nodeState() == NodeState.UNLOCKED && packet.unlockAlarm()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
            Minecraft.getInstance().getToasts().addToast(new SkillTreeNodeToast(packet.skill()));
        }

        if (Minecraft.getInstance().screen instanceof SkillInfoScreen skillInfoScreen) {
            skillInfoScreen.onSyncPacketArrived(packet);
        }

        this.achievedNodes.get(skillTree).put(packet.skill().value(), node);
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
		TopDownTreeNode node =  nodes.get(packet.skill().value());
		
		node.setNodeState(packet.nodeState(), true, packet.unequip());
		
		if (packet.nodeState() == NodeState.UNLOCKED && packet.unlockAlarm()) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F));
			Minecraft.getInstance().getToasts().addToast(new SkillTreeNodeToast(packet.skill()));
		}
		
		if (Minecraft.getInstance().screen instanceof SkillInfoScreen skillInfoScreen) {
			skillInfoScreen.onSyncPacketArrived(packet);
		}
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
		
		public abstract void setNodeState(NodeState nodeState, boolean propagateChildState, boolean modifyEquip);
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
                                if (!this.nodeInfo().hasCustomUnlockCondition()) SkillTreeProgression.this.unlockAwaitingNodes.add(Pair.of(childNode.belongedSkillTree, childNode));
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

                            System.out.println(SkillTreeProgression.this.achievedNodes.get(childNode.belongedSkillTree));

							if (childNode.nodeInfo.noUnlockConditions() || SkillTreeProgression.this.achievedNodes.get(childNode.belongedSkillTree).containsKey(childNode.nodeInfo.skill())) {
								childNode.setNodeState(NodeState.UNLOCKABLE, true, modifyEquip);
							} else if (!childNode.nodeInfo.hasCustomUnlockCondition()) {
                                if (!this.nodeInfo().hasCustomUnlockCondition()) SkillTreeProgression.this.unlockAwaitingNodes.add(Pair.of(childNode.belongedSkillTree, childNode));
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
					Skill childSkill = EpicFightRegistries.SKILL.get(ResourceLocation.parse(childCompound.getString("name"))); 
					
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
					Skill skill = EpicFightRegistries.SKILL.get(ResourceLocation.parse(nodeCompound.getString("name")));
					
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
                    Skill skill = EpicFightRegistries.SKILL.get(ResourceLocation.parse(tag.getAsString()));

                    Map<Skill, TopDownTreeNode> achievedNodes = this.achievedNodes.get(skilltree);
                    achievedNodes.put(skill, this.nodes.get(skilltree).get(skill));
                }
            }
		}
	}
}
