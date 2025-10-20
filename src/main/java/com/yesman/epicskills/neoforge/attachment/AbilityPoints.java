package com.yesman.epicskills.neoforge.attachment;

import com.mojang.serialization.JsonOps;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import yesman.epicfight.network.EpicFightNetworkManager;

public class AbilityPoints {
	public static final int INIT_EXP_REQUIREMENT = 7;
	
	/**
	 * Vanilla copy from {@link net.minecraft.world.entity.player.Player#getXpNeededForNextLevel()}
	 */
	public static int getXpNeededForNextLevel(int expLevel) {
		if (expLevel >= 30) {
			return 112 + (expLevel - 30) * 9;
		} else {
			return expLevel >= 15 ? 37 + (expLevel - 15) * 5 : 7 + expLevel * 2;
		}
	}
	
	private int abilityPoint = 0;
	private int requiredExp = INIT_EXP_REQUIREMENT;
	private int requiredExpLevel = 0;
	private boolean dirty = false;
	private final Player player;
	
	public AbilityPoints(IAttachmentHolder attachmentHolder) {
		if (attachmentHolder instanceof Player player) {
			this.player = player;
		} else {
			throw new IllegalArgumentException(attachmentHolder + " is not a subtype of Player");
		}
	}
	
	public boolean hasEnoughExp() {
		return this.player.totalExperience >= this.requiredExp;
	}
	
	public boolean convertExpToAbilityPoints() {
		if (!this.hasEnoughExp()) {
			return false;
		}
		
		this.player.giveExperiencePoints(-this.requiredExp);;
		this.abilityPoint++;
		
		if (this.requiredExpLevel < 15) {
			this.requiredExp += 2;
		} else if (this.requiredExpLevel < 30) {
			this.requiredExp += 4;
		} else {
			this.requiredExp += 6;
		}
		
		while (this.requiredExp > getXpNeededForNextLevel(this.requiredExpLevel)) {
			this.requiredExpLevel++;
		}
		
		return true;
	}
	
	public void setRequiredExp(int requiredExp) {
		this.requiredExp = Mth.clamp(requiredExp, 0, Integer.MAX_VALUE);
		this.requiredExpLevel = 0;
		
		int totalExp = this.requiredExp;
		int requiredExpForNextLevel = getXpNeededForNextLevel(this.requiredExpLevel);
		
		while (totalExp >= requiredExpForNextLevel) {
			totalExp -= requiredExpForNextLevel;
			requiredExpForNextLevel = getXpNeededForNextLevel(++this.requiredExpLevel);
		}
	}
	
	public int getRequiredExp() {
		return this.requiredExp;
	}
	
	public void setAbilityPoints(int abilityPoint) {
		this.abilityPoint = Mth.clamp(abilityPoint, 0, Integer.MAX_VALUE);
	}
	
	public int getAbilityPoints() {
		return this.abilityPoint;
	}
	
	public void markDirty() {
		this.dirty = true;
	}
	
	public void sendChanges() {
		if (this.player.level().isClientSide()) {
			return;
		}
		
		ServerPlayer serverplayer = (ServerPlayer)this.player;
		
		if (this.dirty) {
			EpicFightNetworkManager.sendToPlayer(new ClientBoundSetAbilityPoints(false, this.abilityPoint, this.requiredExp), serverplayer);
			this.dirty = false;
		}
		
		EntityPredicate.CODEC.decode(JsonOps.INSTANCE, null);
	}
	
	public void serializeTo(CompoundTag compound) {
		compound.putInt("abilityPoint", this.abilityPoint);
		compound.putInt("requiredExp", this.requiredExp);
		compound.putInt("requiredExpLevel", this.requiredExpLevel);
	}

	public void deserializeFrom(CompoundTag nbt) {
		if (nbt.contains("abilityPoint", Tag.TAG_INT)) {
			this.abilityPoint = nbt.getInt("abilityPoint");
		}
		
		if (nbt.contains("requiredExp", Tag.TAG_INT)) {
			this.requiredExp = nbt.getInt("requiredExp");
		}
		
		if (nbt.contains("requiredExpLevel", Tag.TAG_INT)) {
			this.requiredExpLevel = nbt.getInt("requiredExpLevel");
		}
	}
}
