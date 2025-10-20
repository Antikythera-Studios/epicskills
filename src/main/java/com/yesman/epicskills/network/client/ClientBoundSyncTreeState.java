package com.yesman.epicskills.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ClientBoundSyncTreeState(CompoundTag compound) implements ManagedCustomPacketPayload {
	public static final StreamCodec<ByteBuf, ClientBoundSyncTreeState> STREAM_CODEC =
		StreamCodec.composite(
	        ByteBufCodecs.COMPOUND_TAG,
	        ClientBoundSyncTreeState::compound,
	        ClientBoundSyncTreeState::new
	    );
}
