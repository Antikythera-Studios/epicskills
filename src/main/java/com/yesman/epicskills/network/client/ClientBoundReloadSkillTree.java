package com.yesman.epicskills.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ClientBoundReloadSkillTree(boolean readOldData) implements ManagedCustomPacketPayload {
	public static final StreamCodec<ByteBuf, ClientBoundReloadSkillTree> STREAM_CODEC =
		StreamCodec.composite(
	        ByteBufCodecs.BOOL,
	        ClientBoundReloadSkillTree::readOldData,
	        ClientBoundReloadSkillTree::new
	    );
}
