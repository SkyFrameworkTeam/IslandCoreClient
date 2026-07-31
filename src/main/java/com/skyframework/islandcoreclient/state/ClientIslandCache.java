package com.skyframework.islandcoreclient.state;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

public final class ClientIslandCache {
	// Simulated Sprint 2 / Block A data: the server doesn't implement an island snapshot packet
	// yet. Replace with the real IslandSnapshotS2C contents once that lands.
	private static final List<ClientIslandSettingView> SETTINGS = List.of(
			new ClientIslandSettingView("firespread", Text.translatable("islandcoreclient.settings.firespread"), false),
			new ClientIslandSettingView("pvp", Text.translatable("islandcoreclient.settings.pvp"), false),
			new ClientIslandSettingView("mobdamage", Text.translatable("islandcoreclient.settings.mobdamage"), false)
	);

	private static volatile boolean owner = true;
	// True by default so the already-tested Blocks A/B behavior (Dashboard fully lit up) doesn't
	// change unless something explicitly flips this, e.g. the [DEBUG] toggle.
	private static volatile boolean hasIsland = true;

	private static volatile int size = 45;
	private static final int MAX_SIZE = 60;
	private static final String ISLAND_TYPE = "PLAINS";
	private static final boolean HOME_SET = true;
	private static final String ISLAND_STATE = "ACTIVE";
	private static final int HOME_COOLDOWN_SECONDS = 600;

	private static final List<ClientMemberView> MEMBERS = new ArrayList<>(List.of(
			new ClientMemberView(currentPlayerUuid(), currentPlayerName(), ClientMemberView.Role.OWNER),
			new ClientMemberView(UUID.randomUUID(), "Fulanito", ClientMemberView.Role.MEMBER),
			new ClientMemberView(UUID.randomUUID(), "Menganita", ClientMemberView.Role.TRUSTED)
	));

	private static final int DEFAULT_INVITE_EXPIRY_SECONDS = 300;
	private static final List<ClientPendingInviteView> PENDING_INVITES =
			new ArrayList<>(List.of(new ClientPendingInviteView("Zutanito", DEFAULT_INVITE_EXPIRY_SECONDS)));

	@Nullable
	private static volatile ClientIncomingInviteView incomingInvite = null;

	private static final List<ClientBiomeTierView> BIOME_TIERS = List.of(
			new ClientBiomeTierView("base", null, true, List.of(
					new ClientBiomeView("minecraft:plains", Text.translatable("islandcoreclient.biome.plains")),
					new ClientBiomeView("minecraft:desert", Text.translatable("islandcoreclient.biome.desert")),
					new ClientBiomeView("minecraft:forest", Text.translatable("islandcoreclient.biome.forest"))
			)),
			new ClientBiomeTierView("adventurer", Text.translatable("islandcoreclient.biome.tier.adventurer.permission"), false, List.of(
					new ClientBiomeView("minecraft:swamp", Text.translatable("islandcoreclient.biome.swamp")),
					new ClientBiomeView("minecraft:jungle", Text.translatable("islandcoreclient.biome.jungle"))
			)),
			new ClientBiomeTierView("legendary", Text.translatable("islandcoreclient.biome.tier.legendary.permission"), false, List.of(
					new ClientBiomeView("minecraft:cherry_grove", Text.translatable("islandcoreclient.biome.cherry_grove")),
					new ClientBiomeView("minecraft:lush_caves", Text.translatable("islandcoreclient.biome.lush_caves"))
			))
	);

	private static volatile String currentBiomeId = "minecraft:plains";
	// Absolute deadline rather than a per-tick countdown, same reasoning as pending invites.
	private static volatile long biomeCooldownEndMillis = 0L;

	private static final long TELEPORT_REQUEST_COOLDOWN_SECONDS = 600L;
	private static final Map<ClientTeleportType, ClientTeleportState> TELEPORT_STATES = new EnumMap<>(Map.of(
			ClientTeleportType.HOME, new ClientTeleportState(true, 0L, null),
			ClientTeleportType.SPAWN, new ClientTeleportState(true, 200L, null),
			ClientTeleportType.RTP, new ClientTeleportState(false, 0L, "islandcoreclient.error.rtp_disabled_dimension"),
			ClientTeleportType.FARMING, new ClientTeleportState(false, 0L, "islandcoreclient.error.farming_disabled_config")
	));

	// Block C (Admin tab) simulated fixture data. Populated once at class init, same style as
	// the rest of this file.
	private static final List<ClientAdminIslandSummaryView> ADMIN_ISLANDS = new ArrayList<>(List.of(
			new ClientAdminIslandSummaryView(UUID.randomUUID(), "Notch_Fan99", 60, 60, "PLAINS", "ACTIVE", 3),
			new ClientAdminIslandSummaryView(UUID.randomUUID(), "Steve123", 30, 45, "DESERT", "ACTIVE", 1),
			new ClientAdminIslandSummaryView(UUID.randomUUID(), "AlexBuilder", 50, 50, "FOREST", "ACTIVE", 5),
			new ClientAdminIslandSummaryView(UUID.randomUUID(), "Grumpy_Cat", 20, 45, "SWAMP", "DELETING", 2),
			new ClientAdminIslandSummaryView(UUID.randomUUID(), "SkyQueen", 45, 60, "PLAINS", "ACTIVE", 4)
	));
	private static final Map<UUID, ClientAdminIslandDetailView> ADMIN_ISLAND_DETAILS = buildAdminIslandDetails();

	private static volatile boolean spawnExists = true;
	private static volatile int spawnSize = 25;
	@Nullable
	private static volatile BlockPos spawnHomeLocation = new BlockPos(120, 68, -45);

	private static final List<ClientDimensionView> DIMENSIONS = new ArrayList<>(List.of(
			new ClientDimensionView("islandcore:islands", "Islands", ClientDimensionStyle.VOID_FLAT, 8362472L, "ACTIVE"),
			new ClientDimensionView("islandcore:mining_world", "Mundo de Minería", ClientDimensionStyle.OVERWORLD_LIKE, -1928374651L, "ACTIVE"),
			new ClientDimensionView("islandcore:the_abyss", "El Abismo", ClientDimensionStyle.NETHER_LIKE, 445566778L, "ACTIVE")
	));

	private static final Map<ClientResetDimension, ClientVanillaResetState> VANILLA_RESET_STATES = new EnumMap<>(Map.of(
			ClientResetDimension.OVERWORLD, new ClientVanillaResetState(false, null, null),
			ClientResetDimension.NETHER, new ClientVanillaResetState(true, ClientVanillaResetState.SeedMode.RANDOM, null),
			ClientResetDimension.END, new ClientVanillaResetState(false, null, null)
	));

	private ClientIslandCache() {
	}

	public static List<ClientIslandSettingView> getSettings() {
		return SETTINGS;
	}

	public static boolean isOwner() {
		return owner;
	}

	public static boolean hasIsland() {
		return hasIsland;
	}

	public static void setHasIsland(boolean value) {
		hasIsland = value;
	}

	public static void updateSetting(String key, boolean value) {
		for (ClientIslandSettingView setting : SETTINGS) {
			if (setting.key().equals(key)) {
				setting.setValue(value);
				return;
			}
		}
	}

	public static int getSize() {
		return size;
	}

	public static int getMaxSize() {
		return MAX_SIZE;
	}

	public static String getIslandType() {
		return ISLAND_TYPE;
	}

	public static boolean isHomeSet() {
		return HOME_SET;
	}

	public static String getState() {
		return ISLAND_STATE;
	}

	public static int getHomeCooldownSeconds() {
		return HOME_COOLDOWN_SECONDS;
	}

	public static void upgradeIslandSize() {
		size = MAX_SIZE;
	}

	public static List<ClientMemberView> getMembers() {
		return MEMBERS;
	}

	public static void promoteToTrusted(UUID uuid) {
		for (ClientMemberView member : MEMBERS) {
			if (member.uuid().equals(uuid)) {
				member.setRole(ClientMemberView.Role.TRUSTED);
				return;
			}
		}
	}

	public static void removeMember(UUID uuid) {
		MEMBERS.removeIf(member -> member.uuid().equals(uuid));
	}

	public static List<ClientPendingInviteView> getPendingInvites() {
		PENDING_INVITES.removeIf(ClientPendingInviteView::isExpired);
		return PENDING_INVITES;
	}

	public static void addPendingInvite(String targetName) {
		PENDING_INVITES.add(new ClientPendingInviteView(targetName, DEFAULT_INVITE_EXPIRY_SECONDS));
	}

	@Nullable
	public static ClientIncomingInviteView getIncomingInvite() {
		return incomingInvite;
	}

	public static void setIncomingInvite(@Nullable ClientIncomingInviteView invite) {
		incomingInvite = invite;
	}

	public static List<ClientBiomeTierView> getBiomeTiers() {
		return BIOME_TIERS;
	}

	public static String getCurrentBiomeId() {
		return currentBiomeId;
	}

	public static void setCurrentBiomeId(String biomeId) {
		currentBiomeId = biomeId;
	}

	public static long getBiomeCooldownRemainingSeconds() {
		return Math.max(0L, (biomeCooldownEndMillis - System.currentTimeMillis()) / 1000L);
	}

	public static void startBiomeCooldown(long durationSeconds) {
		biomeCooldownEndMillis = System.currentTimeMillis() + durationSeconds * 1000L;
	}

	public static void clearBiomeCooldown() {
		biomeCooldownEndMillis = 0L;
	}

	public static ClientTeleportState getTeleportState(ClientTeleportType type) {
		return TELEPORT_STATES.get(type);
	}

	// TODO: replace with sending TeleportRequestC2S and awaiting ActionResultS2C once IslandCore
	// implements the teleport protocol. TeleportsScreen should not need to change when that
	// happens.
	public static void simulateTeleportRequest(ClientTeleportType type) {
		ClientTeleportState state = TELEPORT_STATES.get(type);
		if (state != null) {
			state.startCooldown(TELEPORT_REQUEST_COOLDOWN_SECONDS);
		}
	}

	public static List<ClientAdminIslandSummaryView> getAdminIslands() {
		return ADMIN_ISLANDS;
	}

	@Nullable
	public static ClientAdminIslandDetailView getAdminIslandDetail(UUID ownerUuid) {
		return ADMIN_ISLAND_DETAILS.get(ownerUuid);
	}

	public static void removeAdminIsland(UUID ownerUuid) {
		ADMIN_ISLANDS.removeIf(summary -> summary.ownerUuid().equals(ownerUuid));
		ADMIN_ISLAND_DETAILS.remove(ownerUuid);
	}

	public static boolean spawnExists() {
		return spawnExists;
	}

	public static void setSpawnExists(boolean value) {
		spawnExists = value;
	}

	public static int getSpawnSize() {
		return spawnSize;
	}

	public static void setSpawnSize(int value) {
		spawnSize = value;
	}

	@Nullable
	public static BlockPos getSpawnHomeLocation() {
		return spawnHomeLocation;
	}

	public static void setSpawnHomeLocation(BlockPos pos) {
		spawnHomeLocation = pos;
	}

	public static List<ClientDimensionView> getDimensions() {
		return DIMENSIONS;
	}

	public static void addDimension(ClientDimensionView dimension) {
		DIMENSIONS.add(dimension);
	}

	public static void removeDimension(String id) {
		DIMENSIONS.removeIf(dimension -> dimension.id().equals(id));
	}

	public static ClientVanillaResetState getVanillaResetState(ClientResetDimension dimension) {
		return VANILLA_RESET_STATES.get(dimension);
	}

	private static Map<UUID, ClientAdminIslandDetailView> buildAdminIslandDetails() {
		Map<UUID, ClientAdminIslandDetailView> details = new HashMap<>();
		int gridIndex = 0;
		for (ClientAdminIslandSummaryView summary : ADMIN_ISLANDS) {
			List<ClientMemberView> members = new ArrayList<>();
			members.add(new ClientMemberView(summary.ownerUuid(), summary.ownerName(), ClientMemberView.Role.OWNER));
			for (int i = 1; i < summary.memberCount(); i++) {
				members.add(new ClientMemberView(UUID.randomUUID(), "Miembro" + i, ClientMemberView.Role.MEMBER));
			}

			String slug = summary.ownerName().toLowerCase(Locale.ROOT);
			details.put(summary.ownerUuid(), new ClientAdminIslandDetailView(
					"island-" + slug, summary.ownerUuid(), summary.ownerName(), "islandcore:island/" + slug,
					gridIndex * 500, gridIndex * 500,
					summary.size(), summary.maxSize(), summary.maxSize() + 20,
					members, summary.state(),
					"12 mayo 2026, 18:03", "30 julio 2026, 09:15",
					new ClientAdminIslandDetailView.EntityCounts(members.size(), 4, 12, 2, 30, 1)));
			gridIndex++;
		}
		return details;
	}

	private static UUID currentPlayerUuid() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player != null ? player.getUuid() : UUID.randomUUID();
	}

	private static String currentPlayerName() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player != null ? player.getGameProfile().getName() : "Tú";
	}
}
