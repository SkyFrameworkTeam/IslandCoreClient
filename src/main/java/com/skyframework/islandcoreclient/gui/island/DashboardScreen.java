package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientConnectionState;
import com.skyframework.islandcoreclient.state.ClientIncomingInviteView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;
import com.skyframework.islandcoreclient.state.DebugSimulationHelpers;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Root screen of the IslandCore Client menu. Content below the top bar only appears once the
 * handshake connected; while UNKNOWN/UNSUPPORTED it just reflects that state (see Sprint 1).
 */
public class DashboardScreen extends BaseMenuScreen {
	private static final int ACTION_BUTTON_WIDTH = 80;
	private static final int ACTION_BUTTON_HEIGHT = 20;
	private static final int ACTION_BUTTON_GAP = 4;

	private static final int INVITE_BANNER_Y = TOP_BAR_HEIGHT + 8;
	private static final int INVITE_BANNER_HEIGHT = 24;
	private static final int CONTENT_START_Y = INVITE_BANNER_Y + INVITE_BANNER_HEIGHT + 8;
	private static final int LINE_HEIGHT = 11;

	private ButtonWidget settingsButton;
	private ButtonWidget membersButton;
	private ButtonWidget biomeButton;
	private ButtonWidget limitsButton;
	private ButtonWidget teleportsButton;
	private ButtonWidget deleteIslandButton;

	private ButtonWidget acceptInviteButton;
	private ButtonWidget ignoreInviteButton;

	public DashboardScreen() {
		super(Text.literal("Dashboard"), null);
	}

	@Override
	protected void initContent() {
		int totalWidth = ACTION_BUTTON_WIDTH * 3 + ACTION_BUTTON_GAP * 2;
		int startX = this.width / 2 - totalWidth / 2;
		int row1Y = this.height - 92;
		int row2Y = this.height - 68;

		this.settingsButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.settings_button"),
						button -> this.client.setScreen(new SettingsScreen(this)))
				.dimensions(startX, row1Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());
		this.membersButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.members_button"),
						button -> this.client.setScreen(new MembersScreen(this)))
				.dimensions(startX + (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP), row1Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());
		this.biomeButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.biome_button"),
						button -> this.client.setScreen(new BiomeScreen(this)))
				.dimensions(startX + (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP) * 2, row1Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());

		this.limitsButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.limits_button"),
						button -> this.client.setScreen(new LimitsScreen(this)))
				.dimensions(startX, row2Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());
		this.teleportsButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.teleports_button"),
						button -> this.client.setScreen(new TeleportsScreen(this)))
				.dimensions(startX + (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP), row2Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());
		this.deleteIslandButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.delete_button").formatted(Formatting.RED),
						button -> this.client.setScreen(new DeleteIslandScreen(this)))
				.dimensions(startX + (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP) * 2, row2Y, ACTION_BUTTON_WIDTH, ACTION_BUTTON_HEIGHT)
				.build());

		int ignoreX = this.width - 16 - 66;
		int acceptX = ignoreX - 4 - 66;
		int inviteButtonY = INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - 16) / 2;
		this.acceptInviteButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.invite_accept"),
						button -> ClientIslandCache.setIncomingInvite(null))
				.dimensions(acceptX, inviteButtonY, 66, 16)
				.build());
		this.ignoreInviteButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.invite_ignore"),
						button -> ClientIslandCache.setIncomingInvite(null))
				.dimensions(ignoreX, inviteButtonY, 66, 16)
				.build());

		// DEBUG - quitar cuando haya snapshot real.
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Invitación"),
						button -> DebugSimulationHelpers.toggleIncomingInviteDebug())
				.dimensions(8, this.height - 20, 120, 16)
				.build());
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Cooldown bioma"),
						button -> DebugSimulationHelpers.toggleBiomeCooldownDebug())
				.dimensions(132, this.height - 20, 140, 16)
				.build());
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Forzar conectado"),
						button -> DebugSimulationHelpers.forceConnectedDebug())
				.dimensions(276, this.height - 20, 130, 16)
				.build());
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Cooldowns TP"),
						button -> DebugSimulationHelpers.toggleTeleportCooldownsDebug())
				.dimensions(410, this.height - 20, 120, 16)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean connected = ClientConnectionState.getStatus() == ClientConnectionState.Status.CONNECTED;
		// Every island action only makes sense once the handshake actually connected.
		this.settingsButton.visible = connected;
		this.settingsButton.active = connected;
		this.membersButton.visible = connected;
		this.membersButton.active = connected;
		this.biomeButton.visible = connected;
		this.biomeButton.active = connected;
		this.limitsButton.visible = connected;
		this.limitsButton.active = connected;
		this.teleportsButton.visible = connected;
		this.teleportsButton.active = connected;
		this.deleteIslandButton.visible = connected;
		this.deleteIslandButton.active = connected;

		ClientIncomingInviteView invite = ClientIslandCache.getIncomingInvite();
		boolean hasInvite = connected && invite != null;
		this.acceptInviteButton.visible = hasInvite;
		this.acceptInviteButton.active = hasInvite;
		this.ignoreInviteButton.visible = hasInvite;
		this.ignoreInviteButton.active = hasInvite;

		if (!connected) {
			int centerX = this.width / 2;
			int centerY = this.height / 2;
			if (ClientConnectionState.getStatus() == ClientConnectionState.Status.UNKNOWN) {
				context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Conectando..."), centerX, centerY, 0xFFFFFF);
			} else {
				context.drawCenteredTextWithShadow(this.textRenderer,
						Text.literal("Este servidor no tiene soporte de IslandCore GUI."), centerX, centerY, 0xAAAAAA);
			}
			return;
		}

		if (hasInvite) {
			context.fill(16, INVITE_BANNER_Y, this.width - 16, INVITE_BANNER_Y + INVITE_BANNER_HEIGHT, 0xC0224488);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.dashboard.invite_banner", invite.fromName()),
					20, INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
		}

		int x = 16;
		int y = CONTENT_START_Y;

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_size", ClientIslandCache.getSize(), ClientIslandCache.getMaxSize()),
				x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_type", ClientIslandCache.getIslandType()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_state", ClientIslandCache.getState()), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		Text homeText = ClientIslandCache.isHomeSet()
				? Text.translatable("islandcoreclient.dashboard.summary_home_set")
				: Text.translatable("islandcoreclient.dashboard.summary_home_not_set");
		context.drawTextWithShadow(this.textRenderer, homeText, x, y, 0xFFFFFF);
		y += LINE_HEIGHT + 6;

		context.drawTextWithShadow(this.textRenderer, Text.translatable("islandcoreclient.dashboard.members_heading"), x, y, 0xFFFFFF);
		y += LINE_HEIGHT;
		for (ClientMemberView member : ClientIslandCache.getMembers()) {
			Text line = Text.literal(member.name() + " ").append(member.role().label());
			context.drawTextWithShadow(this.textRenderer, line, x, y, 0xDDDDDD);
			y += LINE_HEIGHT;
		}
	}
}
