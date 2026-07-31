package com.skyframework.islandcoreclient.network.island;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

// Mirrors the server record exactly. settingId must be IslandSetting#getId() on the server side
// (e.g. "firespread"/"pvp"/"mobdamage") — NOT the enum constant name IslandSnapshotS2C.SettingEntry
// carries (see ClientIslandCache#SERVER_SETTING_ENUM_TO_ID for the mapping between the two).
public record IslandSettingsUpdateC2S(String settingId, boolean value) implements CustomPayload {
	public static final CustomPayload.Id<IslandSettingsUpdateC2S> ID =
			new CustomPayload.Id<>(Identifier.of("islandcore", "island_settings_update_c2s"));

	public static final PacketCodec<RegistryByteBuf, IslandSettingsUpdateC2S> CODEC = PacketCodec.tuple(
			PacketCodecs.STRING, IslandSettingsUpdateC2S::settingId,
			PacketCodecs.BOOL, IslandSettingsUpdateC2S::value,
			IslandSettingsUpdateC2S::new
	);

	@Override
	public CustomPayload.Id<IslandSettingsUpdateC2S> getId() {
		return ID;
	}
}
