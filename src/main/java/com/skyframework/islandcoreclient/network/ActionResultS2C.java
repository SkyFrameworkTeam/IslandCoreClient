package com.skyframework.islandcoreclient.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.Optional;

// Mirrors the server's net.ActionResultS2C exactly (fields, order, codec, channel id): generic
// reply for every action C2S packet (create, upgrade, delete request/confirm, settings, biome,
// invite/accept/trust/remove, teleport). reasonKey is one of IslandCore's ActionReason constants,
// present only when success == false. Carries no numeric extra data (e.g. no exact cooldown
// seconds on a rejected biome change) — the server-side ActionOutcome#data() is not forwarded.
public record ActionResultS2C(boolean success, Optional<String> reasonKey) implements CustomPayload {

	public static final CustomPayload.Id<ActionResultS2C> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "action_result_s2c"));

	private static final PacketCodec<ByteBuf, Optional<String>> REASON_CODEC = PacketCodecs.optional(PacketCodecs.STRING);

	public static final PacketCodec<RegistryByteBuf, ActionResultS2C> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, ActionResultS2C::success,
			REASON_CODEC, ActionResultS2C::reasonKey,
			ActionResultS2C::new
	);

	@Override
	public CustomPayload.Id<ActionResultS2C> getId() {
		return ID;
	}
}
