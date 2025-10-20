package com.yesman.epicskills.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ClientBoundSetAbilityPoints(boolean success, int abilityPoint, int requiredExp) implements ManagedCustomPacketPayload {
	public static final StreamCodec<ByteBuf, ClientBoundSetAbilityPoints> STREAM_CODEC =
		StreamCodec.composite(
	        ByteBufCodecs.BOOL,
	        ClientBoundSetAbilityPoints::success,
	        ByteBufCodecs.INT,
	        ClientBoundSetAbilityPoints::abilityPoint,
	        ByteBufCodecs.INT,
	        ClientBoundSetAbilityPoints::requiredExp,
	        ClientBoundSetAbilityPoints::new
	    );
}
