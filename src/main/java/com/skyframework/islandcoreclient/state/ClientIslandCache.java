package com.skyframework.islandcoreclient.state;

import com.skyframework.islandcoreclient.network.admin.dimension.DimensionDetailS2C;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionListS2C;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDetailS2C;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandListS2C;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnBuildProtectionStatusS2C;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnStatusS2C;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetListS2C;
import com.skyframework.islandcoreclient.network.biome.BiomeTiersS2C;
import com.skyframework.islandcoreclient.network.flag.ExceptionGroupsStatusS2C;
import com.skyframework.islandcoreclient.network.flag.FlagsStatusS2C;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotS2C;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusS2C;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import org.jetbrains.annotations.Nullable;

public final class ClientIslandCache {
	// The snapshot protocol only ever describes the requesting player's OWN island (the server
	// resolves it via getIslandByOwner(player)) — there is no network query yet for an island the
	// player is merely a CO_OWNER/MEMBER of, so "owner" is always true whenever exists == true.
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

	// Real, populated from IslandSnapshotS2C#incomingInvite (see applySnapshot below) — an invite
	// where the local player is the target, not the island owner (contrast PENDING_INVITES above,
	// which lists invites the player's OWN island sent out).
	@Nullable
	private static volatile ClientIncomingInviteView incomingInvite = null;

	// Real, populated from FlagsStatusS2C/ExceptionGroupsStatusS2C — see SettingsScreen, which
	// replaced the old 3-toggle legacy IslandSetting view entirely (the server already treats
	// firespread/pvp/mobdamage as aliases of 3 of these 9 flags, so nothing is lost).
	private static volatile List<ClientFlagView> flags = List.of();
	private static volatile List<ClientExceptionGroupView> exceptionGroups = List.of();

	private static volatile List<ClientBiomeTierView> biomeTiers = List.of();

	// currentBiomeId now comes from the real IslandSnapshotS2C (populated in applySnapshot below).
	// BiomeScreen also writes it optimistically right after a successful biome-change request, for
	// immediate feedback before the next snapshot reconciles it — see BiomeScreen wiring notes.
	// biomeCooldownEndMillis follows the same pattern: BiomeScreen sets it optimistically right
	// after a successful change, and applySnapshot reconciles it from the real
	// biomeCooldownRemainingSeconds field on every fresh snapshot (the server value always wins).
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

	// Admin network block: all real, populated from AdminIslandListS2C/AdminIslandDetailS2C/
	// SpawnStatusS2C/DimensionListS2C/DimensionDetailS2C/VanillaResetListS2C. Empty/default until
	// the first real reply lands — see ClientPacketHandlers' refreshFromNetwork() wiring for each.
	private static volatile List<ClientAdminIslandSummaryView> adminIslands = List.of();
	private static volatile int adminIslandsTotalPages = 0;
	private static volatile int adminIslandsCurrentPage = 0;

	@Nullable
	private static volatile UUID adminIslandDetailUuid = null;
	@Nullable
	private static volatile ClientAdminIslandDetailView adminIslandDetail = null;

	private static volatile boolean spawnExists = false;
	private static volatile int spawnSize = 0;
	@Nullable
	private static volatile BlockPos spawnHomeLocation = null;

	// true (matching BUILD_PROTECTION's own server-side default) until the first real
	// SpawnBuildProtectionStatusS2C arrives — see SpawnManagerScreen's constructor.
	private static volatile boolean spawnBuildProtectionEnabled = true;
	private static volatile List<ClientMemberView> spawnAuthorizedPlayers = List.of();

	private static volatile List<ClientDimensionView> dimensions = new ArrayList<>();

	private static final Map<ClientResetDimension, ClientVanillaResetState> VANILLA_RESET_STATES = new EnumMap<>(Map.of(
			ClientResetDimension.OVERWORLD, new ClientVanillaResetState(false, null, null),
			ClientResetDimension.NETHER, new ClientVanillaResetState(false, null, null),
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
		currentBiomeId = snapshot.currentBiomeId();
		homeSet = snapshot.home().isPresent();
		islandState = snapshot.state();

		// Real value from the server is now the single source of truth, reconciling whatever
		// BiomeScreen may have set optimistically right after a change this session (see its own
		// startBiomeCooldown call) on every fresh snapshot.
		if (snapshot.biomeCooldownRemainingSeconds() > 0) {
			startBiomeCooldown(snapshot.biomeCooldownRemainingSeconds());
		} else {
			clearBiomeCooldown();
		}

		incomingInvite = snapshot.incomingInvite()
				.map(entry -> new ClientIncomingInviteView(entry.inviterName(), entry.expiresInSeconds()))
				.orElse(null);

		MEMBERS.clear();
		for (IslandSnapshotS2C.MemberEntry entry : snapshot.members()) {
			MEMBERS.add(new ClientMemberView(entry.uuid(), entry.name(), ClientMemberView.Role.valueOf(entry.role())));
		}

		PENDING_INVITES.clear();
		for (IslandSnapshotS2C.PendingInviteEntry entry : snapshot.pendingInvites()) {
			PENDING_INVITES.add(new ClientPendingInviteView(entry.targetName(), entry.expiresInSeconds()));
		}
		// snapshot.settings() (the legacy IslandSetting list) is no longer consumed client-side —
		// SettingsScreen reads flags/exceptionGroups below instead.
	}

	public static void applyFlagsStatus(FlagsStatusS2C status) {
		List<ClientFlagView> mapped = new ArrayList<>();
		for (FlagsStatusS2C.FlagEntry entry : status.flags()) {
			ClientFlagView.Category category = ClientFlagView.Category.valueOf(entry.category());
			List<ClientFlagView.RoleValue> resolvedByRole = new ArrayList<>();
			for (FlagsStatusS2C.RoleValueEntry roleEntry : entry.resolvedByRole()) {
				resolvedByRole.add(new ClientFlagView.RoleValue(roleEntry.role(), "ALLOW".equals(roleEntry.value())));
			}
			mapped.add(new ClientFlagView(entry.flagId(), category, entry.resolvedValue(), resolvedByRole,
					ClientTriState.fromWire(entry.islandOverride()), entry.currentPreset(), entry.missingRequiredPermission()));
		}
		flags = List.copyOf(mapped);
	}

	public static List<ClientFlagView> getFlags() {
		return flags;
	}

	// Optimistic update for TriStateRow's immediate cycle, reverted by SettingsScreen on failure —
	// same pattern SettingsScreen#onSettingToggled (now removed) used to use for IslandSettingsUpdateC2S.
	public static void updateFlagOverride(String flagId, ClientTriState newOverride) {
		for (ClientFlagView flag : flags) {
			if (flag.flagId().equals(flagId)) {
				flag.setIslandOverride(newOverride);
				return;
			}
		}
	}

	// Optimistic update for FlagPresetRow's immediate highlight. Only currentPreset is predicted
	// here — unlike updateFlagOverride above, resolvedByRole/islandOverride are NOT guessed, since
	// a preset's exact resolved values depend on server/code defaults this client doesn't
	// replicate; SettingsScreen always refetches FlagsStatusRequestC2S after a successful preset
	// change instead (see its onFlagPresetChanged).
	public static void updateFlagPreset(String flagId, String preset) {
		for (ClientFlagView flag : flags) {
			if (flag.flagId().equals(flagId)) {
				flag.setCurrentPreset(preset);
				return;
			}
		}
	}

	public static void applyExceptionGroupsStatus(ExceptionGroupsStatusS2C status) {
		List<ClientExceptionGroupView> mapped = new ArrayList<>();
		for (ExceptionGroupsStatusS2C.GroupEntry entry : status.groups()) {
			List<ClientFlagView.RoleValue> resolvedByRole = new ArrayList<>();
			for (FlagsStatusS2C.RoleValueEntry roleEntry : entry.resolvedByRole()) {
				resolvedByRole.add(new ClientFlagView.RoleValue(roleEntry.role(), "ALLOW".equals(roleEntry.value())));
			}
			mapped.add(new ClientExceptionGroupView(entry.groupId(), entry.category(), resolvedByRole,
					entry.currentPreset(), entry.ownerConfigurable()));
		}
		exceptionGroups = List.copyOf(mapped);
	}

	public static List<ClientExceptionGroupView> getExceptionGroups() {
		return exceptionGroups;
	}

	// Optimistic update for FlagPresetRow's immediate highlight — exact mirror of updateFlagPreset
	// above, same "don't guess resolvedByRole, always refetch on success" reasoning.
	public static void updateExceptionGroupPreset(String groupId, String preset) {
		for (ClientExceptionGroupView group : exceptionGroups) {
			if (group.groupId().equals(groupId)) {
				group.setCurrentPreset(preset);
				return;
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
		return getKnownBiomeLabel(biome.biomeId()).orElseGet(() -> Text.literal(biome.label()));
	}

	// Public: also used by DashboardScreen for the current-biome summary line, which has no
	// BiomeTiersS2C.BiomeEntry (with its server-computed English fallback label) to work from —
	// only the raw currentBiomeId string from IslandSnapshotS2C. Empty if this isn't one of the
	// ids IslandCoreClient has its own translation for; callers fall back to something else (a
	// server-provided label, or the raw id) in that case.
	public static Optional<Text> getKnownBiomeLabel(String biomeId) {
		String path = biomeId.contains(":") ? biomeId.substring(biomeId.indexOf(':') + 1) : biomeId;
		if (!KNOWN_BIOME_LABEL_PATHS.contains(path)) {
			return Optional.empty();
		}
		return Optional.of(Text.translatable("islandcoreclient.biome." + path));
	}

	// Every biome across all 3 tiers of the default biome_tiers.json (base/adventurer/legendary) —
	// see BiomeTierRegistryImpl#writeDefault for the canonical list this mirrors.
	private static final java.util.Set<String> KNOWN_BIOME_LABEL_PATHS = java.util.Set.of(
			// base
			"plains", "desert", "dark_forest", "the_void", "forest", "savanna", "snowy_plains", "beach",
			// adventurer
			"swamp", "jungle", "badlands", "taiga", "snowy_taiga", "mushroom_fields", "ice_spikes",
			// legendary
			"cherry_grove", "mangrove_swamp", "lush_caves", "dripstone_caves", "deep_dark",
			"warped_forest", "crimson_forest", "soul_sand_valley"
	);

	public static boolean isOwner() {
		return owner;
	}

	public static boolean hasIsland() {
		return hasIsland;
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

	// Used after a confirmed MemberTrustC2S (MEMBER<->CO_OWNER toggle — see MembersScreen#onTrustClicked)
	// to reflect the new role without waiting on a full snapshot refetch.
	public static void setMemberRole(UUID uuid, ClientMemberView.Role role) {
		for (ClientMemberView member : MEMBERS) {
			if (member.uuid().equals(uuid)) {
				member.setRole(role);
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

	public static void applyAdminIslandList(AdminIslandListS2C snapshot) {
		List<ClientAdminIslandSummaryView> mapped = new ArrayList<>();
		for (AdminIslandListS2C.IslandEntry entry : snapshot.islands()) {
			mapped.add(new ClientAdminIslandSummaryView(entry.ownerUuid(), entry.ownerName(), entry.size(),
					entry.maxSize(), entry.type(), entry.currentBiomeId(), entry.state(), entry.memberCount(),
					entry.isSpawnIsland()));
		}
		adminIslands = List.copyOf(mapped);
		adminIslandsTotalPages = snapshot.totalPages();
		adminIslandsCurrentPage = snapshot.currentPage();
	}

	public static List<ClientAdminIslandSummaryView> getAdminIslands() {
		return adminIslands;
	}

	public static int getAdminIslandsTotalPages() {
		return adminIslandsTotalPages;
	}

	public static int getAdminIslandsCurrentPage() {
		return adminIslandsCurrentPage;
	}

	public static void applyAdminIslandDetail(AdminIslandDetailS2C snapshot) {
		List<ClientMemberView> members = new ArrayList<>();
		for (IslandSnapshotS2C.MemberEntry entry : snapshot.members()) {
			members.add(new ClientMemberView(entry.uuid(), entry.name(), ClientMemberView.Role.valueOf(entry.role())));
		}

		ClientAdminIslandDetailView.EntityCounts entities = new ClientAdminIslandDetailView.EntityCounts(
				snapshot.entities().players(), snapshot.entities().hostile(), snapshot.entities().passive(),
				snapshot.entities().cobblemon(), snapshot.entities().items(), snapshot.entities().other());

		adminIslandDetailUuid = snapshot.ownerUuid();
		adminIslandDetail = new ClientAdminIslandDetailView(
				snapshot.islandId().toString(), snapshot.ownerUuid(), snapshot.ownerName(), snapshot.dimension(),
				snapshot.gridX(), snapshot.gridZ(), snapshot.center(), snapshot.boundsMin(), snapshot.boundsMax(),
				snapshot.plotBoundsMin(), snapshot.plotBoundsMax(), snapshot.islandSize(), snapshot.maxSize(), snapshot.plotSize(),
				snapshot.islandType(), snapshot.homeLocation(), members, snapshot.state(),
				snapshot.createdAt(), snapshot.updatedAt(), entities);
	}

	// Only returns a cached detail if it's actually for ownerUuid — a stale detail for a
	// PREVIOUSLY viewed island must never be shown for a newly opened one before its own real
	// AdminIslandDetailS2C arrives (see AdminIslandDetailScreen's refreshFromNetwork wiring).
	@Nullable
	public static ClientAdminIslandDetailView getAdminIslandDetail(UUID ownerUuid) {
		return ownerUuid.equals(adminIslandDetailUuid) ? adminIslandDetail : null;
	}

	public static void applySpawnStatus(SpawnStatusS2C status) {
		spawnExists = status.exists();
		spawnSize = status.size();
		spawnHomeLocation = status.homeLocation().orElse(null);
	}

	public static boolean spawnExists() {
		return spawnExists;
	}

	public static int getSpawnSize() {
		return spawnSize;
	}

	@Nullable
	public static BlockPos getSpawnHomeLocation() {
		return spawnHomeLocation;
	}

	public static void applySpawnBuildProtectionStatus(SpawnBuildProtectionStatusS2C status) {
		spawnBuildProtectionEnabled = status.enabled();
		List<ClientMemberView> mapped = new ArrayList<>();
		for (SpawnBuildProtectionStatusS2C.AuthorizedPlayerEntry entry : status.authorizedPlayers()) {
			mapped.add(new ClientMemberView(entry.uuid(), entry.name(), ClientMemberView.Role.valueOf(entry.role())));
		}
		spawnAuthorizedPlayers = List.copyOf(mapped);
	}

	public static boolean getSpawnBuildProtectionEnabled() {
		return spawnBuildProtectionEnabled;
	}

	// Optimistic update for ToggleRow's immediate flip, reverted by SpawnManagerScreen on failure —
	// same pattern SettingsScreen#onSettingToggled already uses for IslandSettingsUpdateC2S.
	public static void setSpawnBuildProtectionEnabled(boolean enabled) {
		spawnBuildProtectionEnabled = enabled;
	}

	public static List<ClientMemberView> getSpawnAuthorizedPlayers() {
		return spawnAuthorizedPlayers;
	}

	public static void applyDimensionList(DimensionListS2C snapshot) {
		List<ClientDimensionView> mapped = new ArrayList<>();
		for (DimensionListS2C.DimensionEntry entry : snapshot.dimensions()) {
			mapped.add(new ClientDimensionView(entry.id(), entry.displayName(),
					ClientDimensionStyle.valueOf(entry.style()), entry.seed(), entry.state()));
		}
		dimensions = List.copyOf(mapped);
	}

	public static List<ClientDimensionView> getDimensions() {
		return dimensions;
	}

	// Populates createdAt/updatedAt onto the matching cached row from the list (DimensionEntry
	// doesn't carry them, only DimensionDetailS2C does) — a no-op if the dimension isn't in the
	// last fetched list for any reason (e.g. deleted between the list and detail replies).
	public static void applyDimensionDetail(DimensionDetailS2C detail) {
		for (ClientDimensionView dimension : dimensions) {
			if (dimension.id().equals(detail.id())) {
				dimension.applyDetail(detail.createdAt(), detail.updatedAt());
				return;
			}
		}
	}

	public static void applyVanillaResetList(VanillaResetListS2C snapshot) {
		for (ClientResetDimension dimension : ClientResetDimension.values()) {
			VANILLA_RESET_STATES.get(dimension).cancel();
		}
		for (VanillaResetListS2C.QueueEntry entry : snapshot.queue()) {
			ClientResetDimension dimension = ClientResetDimension.valueOf(entry.dimensionKey().toUpperCase(Locale.ROOT));
			// entry.seedMode() is now the real, persisted mode the server decided at confirm time
			// (PendingVanillaReset#seedMode) — no longer inferred from whether entry.seed() is
			// present, which couldn't tell a resolved RANDOM seed apart from a CUSTOM one. Client's
			// own enum spells the third case SPECIFIED rather than CUSTOM; everything else matches
			// by name.
			ClientVanillaResetState.SeedMode seedMode = switch (entry.seedMode()) {
				case "RANDOM" -> ClientVanillaResetState.SeedMode.RANDOM;
				case "CUSTOM" -> ClientVanillaResetState.SeedMode.SPECIFIED;
				default -> ClientVanillaResetState.SeedMode.KEEP;
			};
			VANILLA_RESET_STATES.get(dimension).queue(seedMode, entry.seed().orElse(null));
		}
	}

	public static ClientVanillaResetState getVanillaResetState(ClientResetDimension dimension) {
		return VANILLA_RESET_STATES.get(dimension);
	}
}
