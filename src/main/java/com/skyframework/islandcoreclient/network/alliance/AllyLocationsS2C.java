package com.skyframework.islandcoreclient.network.alliance;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Mirrors the server record exactly. Pushed periodically (not requested by this client) — see
// ClientAllyLocationsCache, the only consumer, and AllyHudRenderer, the only reader of that cache.
public record AllyLocationsS2C(List<Entry> entries) implements CustomPayload {
	public static final CustomPayload.Id<AllyLocationsS2C> ID = new CustomPayload.Id<>(Identifier.of("islandcore", "ally_locations_s2c"));

	private static final PacketCodec<RegistryByteBuf, List<Entry>> ENTRY_LIST_CODEC =
			PacketCodecs.collection(ArrayList::new, Entry.CODEC);

	public static final PacketCodec<RegistryByteBuf, AllyLocationsS2C> CODEC = PacketCodec.tuple(
			ENTRY_LIST_CODEC, AllyLocationsS2C::entries,
			AllyLocationsS2C::new
	);

	@Override
	public CustomPayload.Id<AllyLocationsS2C> getId() {
		return ID;
	}

	public record Entry(UUID uuid, String name, double x, double y, double z) {
		public static final PacketCodec<RegistryByteBuf, Entry> CODEC = PacketCodec.tuple(
				Uuids.PACKET_CODEC, Entry::uuid,
				PacketCodecs.STRING, Entry::name,
				PacketCodecs.DOUBLE, Entry::x,
				PacketCodecs.DOUBLE, Entry::y,
				PacketCodecs.DOUBLE, Entry::z,
				Entry::new
		);
	}
}
