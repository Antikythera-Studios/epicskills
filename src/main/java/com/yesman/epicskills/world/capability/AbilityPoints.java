package com.yesman.epicskills.world.capability;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.yesman.epicskills.EpicSkills;
import com.yesman.epicskills.network.NetworkManager;
import com.yesman.epicskills.network.client.ClientBoundSetAbilityPoints;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public class AbilityPoints {
	public static final Capability<AbilityPoints> ABILITY_POINTS = CapabilityManager.get(new CapabilityToken<> () {});
	
	public static final int INIT_EXP_REQUIREMENT = 7;
	
	/**
	 * Returns {@link AbilityPoints} data belongs to a player
     */
    public static LazyOptional<AbilityPoints> getAbilityPoints(Player player) {
        return player.getCapability(ABILITY_POINTS);
    }
	
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
	
	public AbilityPoints(Player player) {
		this.player = player;
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
			NetworkManager.sendToPlayer(new ClientBoundSetAbilityPoints(false, this.abilityPoint, this.requiredExp), serverplayer);
			this.dirty = false;
		}
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

    private static final ResourceLocation ABILITY_POINTS_CAPABILITY_KEY = EpicSkills.identifier("ability_points");
	
	public static void epicskills$attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject().getType() == EntityType.PLAYER && !event.getCapabilities().containsKey(ABILITY_POINTS_CAPABILITY_KEY)) {
			event.addCapability(ABILITY_POINTS_CAPABILITY_KEY, new AbilityPoints.Provider(new AbilityPoints((Player)event.getObject())));
		}
	}
	
	public static class Provider implements ICapabilityProvider, ICapabilitySerializable<CompoundTag> {
		private final LazyOptional<AbilityPoints> lazyOptional;
		private final AbilityPoints abilityPoints;
		
		public Provider(@NonNull AbilityPoints abilityPoints) {
			this.lazyOptional = LazyOptional.of(() -> abilityPoints);
			this.abilityPoints = abilityPoints;
		}
		
		@Override
		public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
			return cap == ABILITY_POINTS ? this.lazyOptional.cast() : LazyOptional.empty();
		}

		@Override
		public CompoundTag serializeNBT() {
			CompoundTag compound = new CompoundTag();
			this.abilityPoints.serializeTo(compound);
			
			return compound;
		}

		@Override
		public void deserializeNBT(CompoundTag compound) {
			this.abilityPoints.deserializeFrom(compound);
		}
	}
}
