package com.skyframework.islandcoreclient.network;

import com.skyframework.islandcoreclient.network.handshake.ClientHandshakeC2S;
import com.skyframework.islandcoreclient.network.handshake.ServerHandshakeS2C;
import com.skyframework.islandcoreclient.state.ClientConnectionState;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class ClientPacketHandlers {
	private ClientPacketHandlers() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(ClientHandshakeC2S.ID, ClientHandshakeC2S.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHandshakeS2C.ID, ServerHandshakeS2C.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(ServerHandshakeS2C.ID, (payload, context) ->
				ClientConnectionState.onServerHandshakeReceived(payload.protocolVersion(), payload.isOperator()));

		// The server may not implement this protocol at all (e.g. IslandCore hasn't shipped its
		// networking yet): sending is safe regardless, the packet is simply dropped if unhandled.
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ClientConnectionState.onHandshakeSent();
			ClientPlayNetworking.send(new ClientHandshakeC2S(ClientHandshakeC2S.CURRENT_PROTOCOL_VERSION));
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientConnectionState.reset());

		ClientTickEvents.END_CLIENT_TICK.register(client -> ClientConnectionState.tickTimeout());
	}
}
