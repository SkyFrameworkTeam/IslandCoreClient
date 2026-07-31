package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.admin.AdminIslandListScreen;
import com.skyframework.islandcoreclient.gui.admin.DimensionManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.SpawnManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.VanillaResetScreen;
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
 *
 * <p>The Admin tab (Block C) is not a separate Screen: it reuses this same frame/top bar and
 * just swaps which widgets {@link #initContent()} builds and which section
 * {@link #renderContent} draws, toggled by {@link #showingAdmin}.
 */
public class DashboardScreen extends BaseMenuScreen {
	private static final int ACTION_BUTTON_WIDTH = 80;
	private static final int ACTION_BUTTON_HEIGHT = 20;
	private static final int ACTION_BUTTON_GAP = 4;

	private static final int INVITE_BANNER_Y = TOP_BAR_HEIGHT + 8;
	private static final int INVITE_BANNER_HEIGHT = 24;
	private static final int CONTENT_START_Y = INVITE_BANNER_Y + INVITE_BANNER_HEIGHT + 8;
	private static final int LINE_HEIGHT = 11;

	private static final int ADMIN_TOGGLE_WIDTH = 90;
	private static final int ADMIN_GATEWAY_BUTTON_WIDTH = 220;
	private static final int ADMIN_GATEWAY_BUTTON_HEIGHT = 20;
	private static final int ADMIN_GATEWAY_GAP = 8;

	private boolean showingAdmin = false;

	private ButtonWidget adminToggleButton;

	// Player-view widgets (only non-null while !showingAdmin).
	private ButtonWidget settingsButton;
	private ButtonWidget membersButton;
	private ButtonWidget biomeButton;
	private ButtonWidget limitsButton;
	private ButtonWidget teleportsButton;
	private ButtonWidget deleteIslandButton;
	private ButtonWidget acceptInviteButton;
	private ButtonWidget ignoreInviteButton;
	private ButtonWidget createIslandButton;

	public DashboardScreen() {
		super(Text.literal("Dashboard"), null);
	}

	@Override
	protected void initContent() {
		this.adminToggleButton = this.addDrawableChild(ButtonWidget.builder(
						this.showingAdmin
								? Text.translatable("islandcoreclient.dashboard.my_island_button")
								: Text.translatable("islandcoreclient.dashboard.admin_button"),
						button -> {
							this.showingAdmin = !this.showingAdmin;
							this.clearAndInit();
						})
				.dimensions(this.width - 8 - ADMIN_TOGGLE_WIDTH, (TOP_BAR_HEIGHT - 20) / 2, ADMIN_TOGGLE_WIDTH, 20)
				.build());

		if (this.showingAdmin) {
			initAdminGatewayContent();
		} else {
			initPlayerContent();
		}

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
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Alternar sin isla"),
						button -> DebugSimulationHelpers.toggleHasIslandDebug())
				.dimensions(8, this.height - 40, 160, 16)
				.build());
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Forzar admin"),
						button -> DebugSimulationHelpers.toggleAdminDebug())
				.dimensions(172, this.height - 40, 140, 16)
				.build());
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("[DEBUG] Alternar spawn"),
						button -> DebugSimulationHelpers.toggleSpawnExistsDebug())
				.dimensions(316, this.height - 40, 150, 16)
				.build());
	}

	private void initPlayerContent() {
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

		this.createIslandButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.create_island_button"),
						button -> onCreateIslandClicked())
				.dimensions(this.width / 2 - 100, this.height - 116, 200, 20)
				.build());
	}

	private void initAdminGatewayContent() {
		int totalHeight = ADMIN_GATEWAY_BUTTON_HEIGHT * 4 + ADMIN_GATEWAY_GAP * 3;
		int x = this.width / 2 - ADMIN_GATEWAY_BUTTON_WIDTH / 2;
		int y = this.height / 2 - totalHeight / 2;

		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dashboard.island_list_button"),
						button -> this.client.setScreen(new AdminIslandListScreen(this)))
				.dimensions(x, y, ADMIN_GATEWAY_BUTTON_WIDTH, ADMIN_GATEWAY_BUTTON_HEIGHT)
				.build());
		y += ADMIN_GATEWAY_BUTTON_HEIGHT + ADMIN_GATEWAY_GAP;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dashboard.spawn_button"),
						button -> this.client.setScreen(new SpawnManagerScreen(this)))
				.dimensions(x, y, ADMIN_GATEWAY_BUTTON_WIDTH, ADMIN_GATEWAY_BUTTON_HEIGHT)
				.build());
		y += ADMIN_GATEWAY_BUTTON_HEIGHT + ADMIN_GATEWAY_GAP;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dashboard.dimension_manager_button"),
						button -> this.client.setScreen(new DimensionManagerScreen(this)))
				.dimensions(x, y, ADMIN_GATEWAY_BUTTON_WIDTH, ADMIN_GATEWAY_BUTTON_HEIGHT)
				.build());
		y += ADMIN_GATEWAY_BUTTON_HEIGHT + ADMIN_GATEWAY_GAP;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dashboard.vanilla_reset_button"),
						button -> this.client.setScreen(new VanillaResetScreen(this)))
				.dimensions(x, y, ADMIN_GATEWAY_BUTTON_WIDTH, ADMIN_GATEWAY_BUTTON_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean isOperator = ClientConnectionState.isOperator();
		this.adminToggleButton.visible = isOperator;
		this.adminToggleButton.active = isOperator;

		if (this.showingAdmin) {
			renderAdminGatewayContent(context);
		} else {
			renderPlayerContent(context);
		}
	}

	private void renderAdminGatewayContent(DrawContext context) {
		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.admin.dashboard.heading"),
				this.width / 2, TOP_BAR_HEIGHT + 20, 0xFFFFFF);
	}

	private void renderPlayerContent(DrawContext context) {
		boolean connected = ClientConnectionState.getStatus() == ClientConnectionState.Status.CONNECTED;
		boolean hasIsland = ClientIslandCache.hasIsland();
		// Every island action only makes sense once the handshake actually connected; with no
		// island yet they stay visible but dimmed rather than disappearing, per design.
		this.settingsButton.visible = connected;
		this.settingsButton.active = connected && hasIsland;
		this.membersButton.visible = connected;
		this.membersButton.active = connected && hasIsland;
		this.biomeButton.visible = connected;
		this.biomeButton.active = connected && hasIsland;
		this.limitsButton.visible = connected;
		this.limitsButton.active = connected && hasIsland;
		this.teleportsButton.visible = connected;
		this.teleportsButton.active = connected && hasIsland;
		this.deleteIslandButton.visible = connected;
		this.deleteIslandButton.active = connected && hasIsland;

		this.createIslandButton.visible = connected && !hasIsland;
		this.createIslandButton.active = connected && !hasIsland;

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
		int primaryColor = hasIsland ? 0xFFFFFF : 0x777777;
		int secondaryColor = hasIsland ? 0xDDDDDD : 0x777777;

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_size", ClientIslandCache.getSize(), ClientIslandCache.getMaxSize()),
				x, y, primaryColor);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_type", ClientIslandCache.getIslandType()), x, y, primaryColor);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_state", ClientIslandCache.getState()), x, y, primaryColor);
		y += LINE_HEIGHT;
		Text homeText = ClientIslandCache.isHomeSet()
				? Text.translatable("islandcoreclient.dashboard.summary_home_set")
				: Text.translatable("islandcoreclient.dashboard.summary_home_not_set");
		context.drawTextWithShadow(this.textRenderer, homeText, x, y, primaryColor);
		y += LINE_HEIGHT + 6;

		context.drawTextWithShadow(this.textRenderer, Text.translatable("islandcoreclient.dashboard.members_heading"), x, y, primaryColor);
		y += LINE_HEIGHT;
		for (ClientMemberView member : ClientIslandCache.getMembers()) {
			// role().label() carries its own explicit color (GOLD/AQUA/GREEN), which would win
			// over secondaryColor below and defeat the dimming — so skip it while hasIsland=false.
			Text roleText = hasIsland ? member.role().label() : Text.literal(member.role().name());
			Text line = Text.literal(member.name() + " ").append(roleText);
			context.drawTextWithShadow(this.textRenderer, line, x, y, secondaryColor);
			y += LINE_HEIGHT;
		}
	}

	private void onCreateIslandClicked() {
		simulateIslandCreate();
		this.clearAndInit();
	}

	// TODO: replace with sending IslandCreateC2S and awaiting ActionResultS2C once IslandCore
	// implements the island creation protocol.
	private static void simulateIslandCreate() {
		ClientIslandCache.setHasIsland(true);
	}
}
