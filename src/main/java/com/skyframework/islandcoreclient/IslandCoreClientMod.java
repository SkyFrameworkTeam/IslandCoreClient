package com.skyframework.islandcoreclient;

import com.skyframework.islandcoreclient.keybind.OpenMenuKeybind;
import com.skyframework.islandcoreclient.network.ClientPacketHandlers;

import net.fabricmc.api.ClientModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IslandCoreClientMod implements ClientModInitializer {
	public static final String MOD_ID = "islandcoreclient";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ClientPacketHandlers.register();
		OpenMenuKeybind.register();

		LOGGER.info("IslandCore Client initialized.");
	}
}
