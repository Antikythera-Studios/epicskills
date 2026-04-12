package com.yesman.epicskills.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import yesman.epicfight.network.ManagedCustomPacketPayload;

public record ClientBoundDeallocateAbilityPoints(boolean unequipSkills) implements ManagedCustomPacketPayload {
    public static final StreamCodec<ByteBuf, ClientBoundDeallocateAbilityPoints> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClientBoundDeallocateAbilityPoints::unequipSkills,
            ClientBoundDeallocateAbilityPoints::new
        );
}
