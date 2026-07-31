package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.biome.BiomeTiersS2C;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotS2C;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusS2C;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

public final class ClientIslandCache {
	// The 3 settings IslandCore currently supports; IslandSnapshotS2C.SettingEntry#key carries the
	// server's IslandSetting enum CONSTANT NAME (e.g. "FIRE_SPREAD"), while IslandSettingsUpdateC2S
	// expects IslandSetting#getId() ("firespread") — this maps between the two, same ids
	// IslandCommand's settings argument and this list's own keys already used before real network
	// existed.
	private static final Map<String, String> SETTING_ENUM_NAME_TO_ID = Map.of(
			"FIRE_SPREAD", "firespread",
			"PVP_DAMAGE", "pvp",
			"MOB_DAMAGE", "mobdamage"
	);

	private static final List<ClientIslandSettingView> SETTINGS = List.of(
			new ClientIslandSettingView("firespread", Text.translatable("islandcoreclient.settings.firespread"), false),
			new ClientIslandSettingView("pvp", Text.translatable("islandcoreclient.settings.pvp"), false),
			new ClientIslandSettingView("mobdamage", Text.translatable("islandcoreclient.settings.mobdamage"), false)
	);

	// The snapshot protocol only ever describes the requesting player's OWN island (the server
	// resolves it via getIslandByOwner(player)) — there is no network query yet for an island the
	// player is merely a TRUSTED/MEMBER of, so "owner" is always true whenever exists == true.
	private static volatile boolean owner = false;
	private static volatile boolean hasIsland = false;

	private static volatile int size = 0;
	private static volatile int maxSize = 0;
	// IslandType id (server-side always "plains" for now) — a distinct, mostly-unused concept
	// from the current biome; do not confuse with BiomeScreen's currentBiomeId below.
	private static volatile String islandType = "";
	private static volatile boolean homeSet = false;
	private static volatile String islandState = "";

	private static final List<ClientMemberView> MEMBERS = new ArrayList<>();
	private static final List<ClientPendingInviteView> PENDING_INVITES = new ArrayList<>();

	// No IslandSnapshotS2C field (nor any other packet) carries incoming invites (invites where
	// the local player is the target, not the island owner) — this stays local-only/simulated,
	// see DebugSimulationHelpers#toggleIncomingInviteDebug.
	@Nullable
	private static volatile ClientIncomingInviteView incomingInvite = null;

	private static volatile List<ClientBiomeTierView> biomeTiers = List.of();

	// No packet exposes "what biome is the island currently in" or "how many seconds are left on
	// the biome-change cooldown" either — both stay local/optimistic, set only from the outcome of
	// a change the player themselves just made this session. See BiomeScreen wiring notes.
	@Nullable
	private static volatile String currentBiomeId = null;
	private static volatile long biomeCooldownEndMillis = 0L;

	private static final Map<ClientTeleportType, ClientTeleportState> TELEPORT_STATES = new EnumMap<>(ClientTeleportType.class);

	static {
		for (ClientTeleportType type : ClientTeleportType.values()) {
			// Disabled/no-cooldown until the first real TeleportStatusS2C arrives.
			TELEPORT_STATES.put(type, new ClientTeleportState(false, 0L, null));
		}
	}

	// Block C (Admin tab) simulated fixture data — untouched, out of scope for this sprint
	// (the admin/Dimension Manager/vanilla reset protocol doesn't exist on the server yet).
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

	// Replaces every real-data field below from a fresh IslandSnapshotS2C. Called on handshake
	// connect and whenever a screen that needs fresh data opens or completes an action.
	public static void applySnapshot(IslandSnapshotS2C snapshot) {
		// IslandDeletionServiceImpl#confirmDeletion doesn't remove the island from the registry
		// synchronously — it marks it DELETING and only actually deletes it once the incremental
		// block-clearing job finishes, ticks later. exists() stays true for that whole window, so
		// treating a DELETING island as "no island" here (not just exists()) is what makes the
		// Dashboard flip to the dimmed/"Crear isla" state right after a successful
		// IslandDeleteConfirmC2S, instead of only once the block clearing eventually completes.
		boolean deleting = "DELETING".equals(snapshot.state());
		owner = snapshot.exists() && !deleting;
		hasIsland = snapshot.exists() && !deleting;
		size = snapshot.size();
		maxSize = snapshot.maxSize();
		islandType = snapshot.type();
		homeSet = snapshot.home().isPresent();
		islandState = snapshot.state();

		MEMBERS.clear();
		for (IslandSnapshotS2C.MemberEntry entry : snapshot.members()) {
			MEMBERS.add(new ClientMemberView(entry.uuid(), entry.name(), ClientMemberView.Role.valueOf(entry.role())));
		}

		PENDING_INVITES.clear();
		for (IslandSnapshotS2C.PendingInviteEntry entry : snapshot.pendingInvites()) {
			PENDING_INVITES.add(new ClientPendingInviteView(entry.targetName(), entry.expiresInSeconds()));
		}

		for (IslandSnapshotS2C.SettingEntry entry : snapshot.settings()) {
			String id = SETTING_ENUM_NAME_TO_ID.get(entry.key());
			if (id != null) {
				updateSetting(id, entry.value());
			}
		}
	}

	public static void applyTeleportStatus(TeleportStatusS2C status) {
		applyTeleportStatusEntry(ClientTeleportType.HOME, status.home());
		applyTeleportStatusEntry(ClientTeleportType.SPAWN, status.spawn());
		applyTeleportStatusEntry(ClientTeleportType.RTP, status.rtp());
		applyTeleportStatusEntry(ClientTeleportType.FARMING, status.farming());
	}

	private static void applyTeleportStatusEntry(ClientTeleportType type, TeleportStatusS2C.StatusEntry entry) {
		// reasonKey from the server is a raw ActionReason id (e.g. "rtp_disabled"); pre-resolving
		// it to a full translation key here means TeleportsScreen's existing
		// Text.translatable(state.reasonKey()) call needs no change.
		String translationKey = entry.reasonKey().map(reason -> "islandcoreclient.reason." + reason).orElse(null);
		TELEPORT_STATES.put(type, new ClientTeleportState(entry.enabled(), entry.cooldownRemainingSeconds(), translationKey));
	}

	public static void applyBiomeTiers(BiomeTiersS2C tiers) {
		List<ClientBiomeTierView> mapped = new ArrayList<>();
		for (BiomeTiersS2C.TierEntry tier : tiers.tiers()) {
			Text permissionLabel = tier.permissionRequired().isPresent()
					? Text.translatable("islandcoreclient.biome.tier." + tier.tierId() + ".permission")
					: null;

			List<ClientBiomeView> biomes = new ArrayList<>();
			for (BiomeTiersS2C.BiomeEntry biome : tier.biomes()) {
				biomes.add(new ClientBiomeView(biome.biomeId(), localizedBiomeLabel(biome)));
			}

			mapped.add(new ClientBiomeTierView(tier.tierId(), permissionLabel, tier.unlocked(), biomes));
		}
		biomeTiers = List.copyOf(mapped);
	}

	// Prefer an existing Spanish translation for biomes IslandCoreClient already knows about
	// (the ones in the default biome_tiers.json config); fall back to the server's generic
	// English label (derived from the biome's Identifier path) for anything else, so a custom
	// server config doesn't render a raw/untranslated key.
	private static Text localizedBiomeLabel(BiomeTiersS2C.BiomeEntry biome) {
		String path = biome.biomeId().contains(":") ? biome.biomeId().substring(biome.biomeId().indexOf(':') + 1) : biome.biomeId();
		String translationKey = "islandcoreclient.biome." + path;
		if (KNOWN_BIOME_LABEL_PATHS.contains(path)) {
			return Text.translatable(translationKey);
		}
		return Text.literal(biome.label());
	}

	private static final java.util.Set<String> KNOWN_BIOME_LABEL_PATHS = java.util.Set.of(
			"plains", "desert", "forest", "swamp", "jungle", "cherry_grove", "lush_caves"
	);

	public static List<ClientIslandSettingView> getSettings() {
		return SETTINGS;
	}

	public static boolean isOwner() {
		return owner;
	}

	public static boolean hasIsland() {
		return hasIsland;
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
		return maxSize;
	}

	public static String getIslandType() {
		return islandType;
	}

	public static boolean isHomeSet() {
		return homeSet;
	}

	public static String getState() {
		return islandState;
	}

	public static void setSize(int value) {
		size = value;
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
		PENDING_INVITES.add(new ClientPendingInviteView(targetName, 300));
	}

	@Nullable
	public static ClientIncomingInviteView getIncomingInvite() {
		return incomingInvite;
	}

	public static void setIncomingInvite(@Nullable ClientIncomingInviteView invite) {
		incomingInvite = invite;
	}

	public static List<ClientBiomeTierView> getBiomeTiers() {
		return biomeTiers;
	}

	@Nullable
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
}
