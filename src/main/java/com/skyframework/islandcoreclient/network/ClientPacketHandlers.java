package com.skyframework.islandcoreclient.network;

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

public final class ClientPacketHandlers {
	private ClientPacketHandlers() {
	}

	public static void register() {
		registerPayloadTypes();

		ClientPlayNetworking.registerGlobalReceiver(ServerHandshakeS2C.ID, (payload, context) -> {
			ClientConnectionState.onServerHandshakeReceived(payload.protocolVersion(), payload.isOperator());
			// The handshake just confirmed this server speaks the IslandCore protocol: fetch the
			// player's own island (or the empty snapshot) right away so the Dashboard has real
			// data as soon as it's first opened, instead of waiting for a manual refresh.
			ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
		});

		ClientPlayNetworking.registerGlobalReceiver(IslandSnapshotS2C.ID, (payload, context) ->
				ClientIslandCache.applySnapshot(payload));

		ClientPlayNetworking.registerGlobalReceiver(TeleportStatusS2C.ID, (payload, context) ->
				ClientIslandCache.applyTeleportStatus(payload));

		ClientPlayNetworking.registerGlobalReceiver(BiomeTiersS2C.ID, (payload, context) ->
				ClientIslandCache.applyBiomeTiers(payload));

		ClientPlayNetworking.registerGlobalReceiver(ActionResultS2C.ID, (payload, context) ->
				PendingActionTracker.onActionResult(payload));

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
	}
}
