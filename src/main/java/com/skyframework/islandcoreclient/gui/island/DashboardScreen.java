package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientConnectionState;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Root screen of the IslandCore Client menu. This sprint only reflects the handshake state;
 * the real dashboard content lands once the server implements its networking.
 */
public class DashboardScreen extends BaseMenuScreen {
	public DashboardScreen() {
		super(Text.literal("Dashboard"), null);
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		int centerX = this.width / 2;
		int centerY = this.height / 2;

		switch (ClientConnectionState.getStatus()) {
			case UNKNOWN -> context.drawCenteredTextWithShadow(
					this.textRenderer, Text.literal("Conectando..."), centerX, centerY, 0xFFFFFF);
			case UNSUPPORTED -> context.drawCenteredTextWithShadow(
					this.textRenderer,
					Text.literal("Este servidor no tiene soporte de IslandCore GUI."),
					centerX, centerY, 0xAAAAAA);
			case CONNECTED -> {
				context.drawCenteredTextWithShadow(
						this.textRenderer,
						Text.literal("Conectado a IslandCore (versión de protocolo: "
								+ ClientConnectionState.getProtocolVersion()
								+ "). Esperando implementación de pantallas."),
						centerX, centerY - 6, 0xFFFFFF);
				Text operatorText = ClientConnectionState.isOperator()
						? Text.literal("Permisos de administrador detectados")
						: Text.literal("Jugador estándar");
				context.drawCenteredTextWithShadow(this.textRenderer, operatorText, centerX, centerY + 6, 0xAAAAAA);
			}
		}
	}
}
