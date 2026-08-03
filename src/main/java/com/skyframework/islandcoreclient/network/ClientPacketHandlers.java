package com.skyframework.islandcoreclient.network;

import com.skyframework.islandcoreclient.gui.admin.AdminIslandDetailScreen;
import com.skyframework.islandcoreclient.gui.admin.AdminIslandListScreen;
import com.skyframework.islandcoreclient.gui.admin.DimensionManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.SpawnManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.VanillaResetScreen;
import com.skyframework.islandcoreclient.gui.island.BiomeScreen;
import com.skyframework.islandcoreclient.gui.island.TeleportsScreen;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionCreateC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionDeleteC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionDeleteConfirmC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionDetailRequestC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionDetailS2C;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionListRequestC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionListS2C;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionRegenerateC2S;
import com.skyframework.islandcoreclient.network.admin.dimension.DimensionRegenerateConfirmC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDeleteC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDeleteConfirmC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDetailRequestC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDetailS2C;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandListRequestC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandListS2C;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandCreateC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandResizeC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnIslandSetHomeC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnStatusRequestC2S;
import com.skyframework.islandcoreclient.network.admin.spawn.SpawnStatusS2C;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetCancelC2S;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetConfirmC2S;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetListRequestC2S;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetListS2C;
import com.skyframework.islandcoreclient.network.admin.vanilla.VanillaResetQueueC2S;
import com.skyframework.islandcoreclient.network.biome.BiomeTiersRequestC2S;
import com.skyframework.islandcoreclient.network.biome.BiomeTiersS2C;
import com.skyframework.islandcoreclient.network.handshake.ClientHandshakeC2S;
import com.skyframework.islandcoreclient.network.handshake.ServerHandshakeS2C;
import com.skyframework.islandcoreclient.network.island.IslandBiomeChangeC2S;
import com.skyframework.islandcoreclient.network.island.IslandCreateC2S;
import com.skyframework.islandcoreclient.network.island.IslandDeleteConfirmC2S;
import com.skyframework.islandcoreclient.network.island.IslandDeleteRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandSettingsUpdateC2S;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotS2C;
import com.skyframework.islandcoreclient.network.island.IslandUpgradeC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteAcceptC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteC2S;
import com.skyframework.islandcoreclient.network.member.MemberRemoveC2S;
import com.skyframework.islandcoreclient.network.member.MemberTrustC2S;
import com.skyframework.islandcoreclient.network.teleport.TeleportRequestC2S;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusRequestC2S;
import com.skyframework.islandcoreclient.network.teleport.TeleportStatusS2C;
import com.skyframework.islandcoreclient.state.ClientConnectionState;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import net.minecraft.client.MinecraftClient;

public final class ClientPacketHandlers {
	private ClientPacketHandlers() {
	}

	public static void register() {
		registerPayloadTypes();

		ClientPlayNetworking.registerGlobalReceiver(ServerHandshakeS2C.ID, (payload, context) -> {
			ClientConnectionState.onServerHandshakeReceived(payload.protocolVersion(), payload.protocolCompatible(), payload.isOperator());
			if (payload.protocolCompatible()) {
				// The handshake just confirmed this server speaks a compatible IslandCore protocol:
				// fetch the player's own island (or the empty snapshot) right away so the Dashboard
				// has real data as soon as it's first opened, instead of waiting for a manual refresh.
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			}
			// else: protocolCompatible=false — DashboardScreen shows a mismatch message and renders
			// nothing else, so there's no screen that would need this data anyway; requesting it
			// would just be a packet built against a wire format the server may not actually emit.
		});

		ClientPlayNetworking.registerGlobalReceiver(IslandSnapshotS2C.ID, (payload, context) ->
				ClientIslandCache.applySnapshot(payload));

		ClientPlayNetworking.registerGlobalReceiver(TeleportStatusS2C.ID, (payload, context) -> {
			ClientIslandCache.applyTeleportStatus(payload);
			// TeleportsScreen.initContent() sends the request but builds its buttons
			// synchronously from whatever was already cached — on the very first visit this
			// session that's the disabled-by-default placeholder, since this reply hasn't
			// landed yet. Rebuild the screen once real data arrives so those buttons don't
			// stay stuck inactive until the player leaves and reopens the screen.
			if (MinecraftClient.getInstance().currentScreen instanceof TeleportsScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(BiomeTiersS2C.ID, (payload, context) -> {
			ClientIslandCache.applyBiomeTiers(payload);
			// Same race as TeleportStatusS2C/TeleportsScreen above: rebuild the screen once real
			// tier data arrives so it doesn't stay blank until the player leaves and reopens it.
			if (MinecraftClient.getInstance().currentScreen instanceof BiomeScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(ActionResultS2C.ID, (payload, context) ->
				PendingActionTracker.onActionResult(payload));

		registerAdminHandlers();

		// The server may not implement this protocol at all (e.g. IslandCore hasn't shipped its
		// networking yet): sending is safe regardless, the packet is simply dropped if unhandled.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ClientConnectionState.onHandshakeSent();
			ClientPlayNetworking.send(new ClientHandshakeC2S(ClientHandshakeC2S.CURRENT_PROTOCOL_VERSION));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientConnectionState.reset();
			PendingActionTracker.reset();
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			ClientConnectionState.tickTimeout();
			PendingActionTracker.tick();
		});
	}

	private static void registerPayloadTypes() {
		PayloadTypeRegistry.playC2S().register(ClientHandshakeC2S.ID, ClientHandshakeC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHandshakeS2C.ID, ServerHandshakeS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(IslandSnapshotRequestC2S.ID, IslandSnapshotRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(IslandSnapshotS2C.ID, IslandSnapshotS2C.CODEC);

		PayloadTypeRegistry.playS2C().register(ActionResultS2C.ID, ActionResultS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(IslandCreateC2S.ID, IslandCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandUpgradeC2S.ID, IslandUpgradeC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandDeleteRequestC2S.ID, IslandDeleteRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandDeleteConfirmC2S.ID, IslandDeleteConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandSettingsUpdateC2S.ID, IslandSettingsUpdateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(IslandBiomeChangeC2S.ID, IslandBiomeChangeC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(MemberInviteC2S.ID, MemberInviteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberInviteAcceptC2S.ID, MemberInviteAcceptC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberTrustC2S.ID, MemberTrustC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(MemberRemoveC2S.ID, MemberRemoveC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(TeleportRequestC2S.ID, TeleportRequestC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(TeleportStatusRequestC2S.ID, TeleportStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(TeleportStatusS2C.ID, TeleportStatusS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(BiomeTiersRequestC2S.ID, BiomeTiersRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(BiomeTiersS2C.ID, BiomeTiersS2C.CODEC);

		PayloadTypeRegistry.playC2S().register(AdminIslandListRequestC2S.ID, AdminIslandListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AdminIslandListS2C.ID, AdminIslandListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDetailRequestC2S.ID, AdminIslandDetailRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(AdminIslandDetailS2C.ID, AdminIslandDetailS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDeleteC2S.ID, AdminIslandDeleteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(AdminIslandDeleteConfirmC2S.ID, AdminIslandDeleteConfirmC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(SpawnStatusRequestC2S.ID, SpawnStatusRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(SpawnStatusS2C.ID, SpawnStatusS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandCreateC2S.ID, SpawnIslandCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandResizeC2S.ID, SpawnIslandResizeC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(SpawnIslandSetHomeC2S.ID, SpawnIslandSetHomeC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(DimensionListRequestC2S.ID, DimensionListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(DimensionListS2C.ID, DimensionListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDetailRequestC2S.ID, DimensionDetailRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(DimensionDetailS2C.ID, DimensionDetailS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionCreateC2S.ID, DimensionCreateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDeleteC2S.ID, DimensionDeleteC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionDeleteConfirmC2S.ID, DimensionDeleteConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionRegenerateC2S.ID, DimensionRegenerateC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(DimensionRegenerateConfirmC2S.ID, DimensionRegenerateConfirmC2S.CODEC);

		PayloadTypeRegistry.playC2S().register(VanillaResetListRequestC2S.ID, VanillaResetListRequestC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(VanillaResetListS2C.ID, VanillaResetListS2C.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetQueueC2S.ID, VanillaResetQueueC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetConfirmC2S.ID, VanillaResetConfirmC2S.CODEC);
		PayloadTypeRegistry.playC2S().register(VanillaResetCancelC2S.ID, VanillaResetCancelC2S.CODEC);
	}

	// Admin network block: each S2C handler applies the real data to ClientIslandCache, then
	// rebuilds the currently open screen if it's the one waiting on that exact reply — same
	// TeleportStatusS2C/BiomeTiersS2C race-avoidance pattern used above, applied to all 5 new
	// screens from the start instead of shipping them with the same blank-on-first-visit bug.
	private static void registerAdminHandlers() {
		ClientPlayNetworking.registerGlobalReceiver(AdminIslandListS2C.ID, (payload, context) -> {
			ClientIslandCache.applyAdminIslandList(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof AdminIslandListScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(AdminIslandDetailS2C.ID, (payload, context) -> {
			ClientIslandCache.applyAdminIslandDetail(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof AdminIslandDetailScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(SpawnStatusS2C.ID, (payload, context) -> {
			ClientIslandCache.applySpawnStatus(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof SpawnManagerScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(DimensionListS2C.ID, (payload, context) -> {
			ClientIslandCache.applyDimensionList(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof DimensionManagerScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(DimensionDetailS2C.ID, (payload, context) -> {
			ClientIslandCache.applyDimensionDetail(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof DimensionManagerScreen screen) {
				screen.refreshFromNetwork();
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(VanillaResetListS2C.ID, (payload, context) -> {
			ClientIslandCache.applyVanillaResetList(payload);
			if (MinecraftClient.getInstance().currentScreen instanceof VanillaResetScreen screen) {
				screen.refreshFromNetwork();
			}
		});
	}
}
