package com.skyframework.islandcoreclient.gui.island;

import com.skyframework.islandcoreclient.gui.admin.AdminIslandListScreen;
import com.skyframework.islandcoreclient.gui.admin.DefaultConfigScreen;
import com.skyframework.islandcoreclient.gui.admin.DimensionManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.SpawnManagerScreen;
import com.skyframework.islandcoreclient.gui.admin.VanillaResetScreen;
import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.party.PartyScreen;
import com.skyframework.islandcoreclient.gui.common.TimeFormat;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.island.IslandCreateC2S;
import com.skyframework.islandcoreclient.network.island.IslandSnapshotRequestC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteAcceptC2S;
import com.skyframework.islandcoreclient.network.member.MemberInviteDeclineC2S;
import com.skyframework.islandcoreclient.state.ClientConnectionState;
import com.skyframework.islandcoreclient.state.ClientIncomingInviteView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Root screen of the IslandCore Client menu. Content below the top bar only appears once the
 * handshake connected; while UNKNOWN/UNSUPPORTED/PROTOCOL_MISMATCH it just reflects that state
 * (see Sprint 1).
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
	// Minimum gap kept below the "Panel de administración" heading (drawn at TOP_BAR_HEIGHT + 20,
	// see renderAdminGatewayContent) — the button list below is normally vertically centered in the
	// whole screen, which reads fine on a tall window but let the first button creep up under the
	// heading on a short one (a small window, or a high GUI Scale shrinking the effective GUI-space
	// height). This is enforced as a floor on top of the centering, not a replacement for it.
	private static final int ADMIN_HEADING_GAP = 16;

	private boolean showingAdmin = false;

	private ButtonWidget adminToggleButton;

	// Player-view widgets (only non-null while !showingAdmin).
	private ButtonWidget settingsButton;
	private ButtonWidget partyButton;
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
		if (ClientConnectionState.getStatus() == ClientConnectionState.Status.PROTOCOL_MISMATCH) {
			// No snapshot request, no admin toggle, no player/admin widgets at all — renderContent
			// shows only the mismatch message for this status, so nothing here would ever be seen.
			return;
		}

		if (ClientConnectionState.getStatus() == ClientConnectionState.Status.CONNECTED) {
			// Refetch on every (re)entry to this screen — clearAndInit() from the admin toggle,
			// navigating back from a child screen, or the very first open all run through here —
			// so the summary/members list shown is never more stale than the last screen visit.
			ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
		}

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
		// Unlike every other action button here, Party must stay clickable with no island: the
		// island-members half of the merged screen just renders dimmed/empty in that case (see
		// PartyScreen's own MEMBERS page), but the party half never depended on having an island —
		// so this one is deliberately NOT gated by hasIsland in renderPlayerContent below.
		this.partyButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.party_button"),
						button -> this.client.setScreen(new PartyScreen(this)))
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
						button -> onAcceptInviteClicked())
				.dimensions(acceptX, inviteButtonY, 66, 16)
				.build());
		// Clears the banner immediately (optimistic, same as before) and also tells the server to
		// drop the pending invite via MemberInviteDeclineC2S, so it no longer reappears on the next
		// snapshot refresh — it used to only be hidden locally while the server-side invite stayed
		// alive until its own 5-minute timeout.
		this.ignoreInviteButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.invite_ignore"),
						button -> onIgnoreInviteClicked())
				.dimensions(ignoreX, inviteButtonY, 66, 16)
				.build());

		this.createIslandButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.dashboard.create_island_button"),
						button -> onCreateIslandClicked())
				.dimensions(this.width / 2 - 100, this.height - 116, 200, 20)
				.build());
	}

	private void initAdminGatewayContent() {
		int totalHeight = ADMIN_GATEWAY_BUTTON_HEIGHT * 5 + ADMIN_GATEWAY_GAP * 4;
		int x = this.width / 2 - ADMIN_GATEWAY_BUTTON_WIDTH / 2;
		int minY = TOP_BAR_HEIGHT + 20 + this.textRenderer.fontHeight + ADMIN_HEADING_GAP;
		int y = Math.max(this.height / 2 - totalHeight / 2, minY);

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
		y += ADMIN_GATEWAY_BUTTON_HEIGHT + ADMIN_GATEWAY_GAP;
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.dashboard.default_config_button"),
						button -> this.client.setScreen(new DefaultConfigScreen(this)))
				.dimensions(x, y, ADMIN_GATEWAY_BUTTON_WIDTH, ADMIN_GATEWAY_BUTTON_HEIGHT)
				.build());
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (ClientConnectionState.getStatus() == ClientConnectionState.Status.PROTOCOL_MISMATCH) {
			int centerX = this.width / 2;
			int centerY = this.height / 2;
			// Split across two lines: the combined sentence is long enough to risk running past
			// the screen edge with drawCenteredTextWithShadow's single-line rendering.
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("Este servidor usa una versión distinta de protocolo de IslandCore."),
					centerX, centerY - 6, 0xFFFFFF);
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("Actualiza tu mod de cliente."),
					centerX, centerY + 6, 0xFFFFFF);
			return;
		}

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
		// Not hasIsland-gated like the rest of this row — see the field's own comment above.
		this.partyButton.visible = connected;
		this.partyButton.active = connected;
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
			String remaining = TimeFormat.minutesSeconds(invite.expiresInSeconds());
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.dashboard.invite_banner", invite.fromName(), remaining),
					20, INVITE_BANNER_Y + (INVITE_BANNER_HEIGHT - this.textRenderer.fontHeight) / 2, 0xFFFFFF);
		}

		int x = 16;
		// CONTENT_START_Y only when the invite banner is actually drawn above — it used to be a
		// fixed constant that always reserved that banner's height (32px, ~3 lines) even when
		// hasInvite is false (the common case), pushing the summary/members content down for no
		// reason. See the class-level note on this bug.
		int y = hasInvite ? CONTENT_START_Y : INVITE_BANNER_Y;
		int primaryColor = hasIsland ? 0xFFFFFF : 0x777777;
		int secondaryColor = hasIsland ? 0xDDDDDD : 0x777777;

		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_size", ClientIslandCache.getSize(), ClientIslandCache.getMaxSize()),
				x, y, primaryColor);
		y += LINE_HEIGHT;
		context.drawTextWithShadow(this.textRenderer,
				Text.translatable("islandcoreclient.dashboard.summary_biome", currentBiomeLabel()), x, y, primaryColor);
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

	// Real currentBiomeId from the snapshot, not islandType — that field is a distinct,
	// almost-decorative server concept that's always "plains" and tells the player nothing
	// useful. Falls back to the raw id if it's not one of the ones IslandCoreClient has its own
	// translation for (a custom server biome_tiers.json config, or before the first snapshot
	// ever arrives and currentBiomeId is still null).
	private static Text currentBiomeLabel() {
		String biomeId = ClientIslandCache.getCurrentBiomeId();
		if (biomeId == null || biomeId.isEmpty()) {
			return Text.literal("?");
		}
		return ClientIslandCache.getKnownBiomeLabel(biomeId).orElseGet(() -> Text.literal(biomeId));
	}

	private void onCreateIslandClicked() {
		ClientPlayNetworking.send(new IslandCreateC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
	}

	private void onIgnoreInviteClicked() {
		// Clears the banner right away, same as before — no need to wait for the server's ack since
		// there's no error state worth surfacing here (a stale/expired invite fails harmlessly).
		ClientIslandCache.setIncomingInvite(null);
		ClientPlayNetworking.send(new MemberInviteDeclineC2S());
	}

	private void onAcceptInviteClicked() {
		ClientPlayNetworking.send(new MemberInviteAcceptC2S());
		PendingActionTracker.await((success, reasonKey) -> {
			// Clear the local banner either way: on success the invite was consumed server-side;
			// on failure (e.g. NO_PENDING_INVITE) it was already stale, so there's nothing left to
			// show either.
			ClientIslandCache.setIncomingInvite(null);
			if (success) {
				ClientPlayNetworking.send(new IslandSnapshotRequestC2S());
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
		});
	}
}
