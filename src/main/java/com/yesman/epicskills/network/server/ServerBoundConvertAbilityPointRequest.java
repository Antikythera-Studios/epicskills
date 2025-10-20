package com.yesman.epicskills.network.server;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ServerBoundConvertAbilityPointRequest() implements ManagedCustomPacketPayload {
	public static final StreamCodec<RegistryFriendlyByteBuf, ServerBoundConvertAbilityPointRequest> STREAM_CODEC =
		StreamCodec.of(
            (buf, payload) -> {},
            buf -> new ServerBoundConvertAbilityPointRequest()
        );
}
