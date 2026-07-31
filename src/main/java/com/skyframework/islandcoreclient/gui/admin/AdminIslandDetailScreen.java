package com.skyframework.islandcoreclient.gui.admin;

import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientAdminIslandDetailView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

public class AdminIslandDetailScreen extends BaseMenuScreen {
	private static final long PENDING_DELETION_WINDOW_SECONDS = 30L;
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int SECTION_GAP = 8;
	private static final int BUTTON_Y_FROM_BOTTOM = 44;
	private static final int SECTION_HEADER_COLOR = 0xFFDD55;
	private static final int BODY_COLOR = 0xDDDDDD;

	private final UUID ownerUuid;
	@Nullable
	private final ClientAdminIslandDetailView detail;

	// 0 = no pending deletion. Screen-local UI flow state, same pattern as DeleteIslandScreen.
	private long pendingDeletionExpiresAtMillis = 0L;

	public AdminIslandDetailScreen(UUID ownerUuid, Screen parent) {
		super(Text.translatable("islandcoreclient.admin.island_detail.title"), parent);
		this.ownerUuid = ownerUuid;
		this.detail = ClientIslandCache.getAdminIslandDetail(ownerUuid);
	}

	@Override
	protected void initContent() {
		if (this.detail == null) {
			return;
		}

		boolean pending = this.pendingDeletionExpiresAtMillis > 0;
		int buttonY = this.height - BUTTON_Y_FROM_BOTTOM;

		ButtonWidget deleteButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_detail.delete_button").formatted(Formatting.RED),
						button -> onDeleteClicked())
				.dimensions(CONTENT_X, buttonY, 200, 20)
				.build());
		deleteButton.visible = !pending;
		deleteButton.active = !pending;

		ButtonWidget confirmButton = this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_detail.confirm_delete_button"),
						button -> onConfirmDeleteClicked())
				.dimensions(CONTENT_X, buttonY, 200, 20)
				.build());
		confirmButton.visible = pending;
		confirmButton.active = pending;
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		if (this.detail == null) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_detail.not_found"), CONTENT_X, TOP_BAR_HEIGHT + 8, 0xAAAAAA);
			return;
		}

		if (this.pendingDeletionExpiresAtMillis > 0 && System.currentTimeMillis() >= this.pendingDeletionExpiresAtMillis) {
			this.pendingDeletionExpiresAtMillis = 0L;
			this.clearAndInit();
			return;
		}

		int x = CONTENT_X;
		int y = TOP_BAR_HEIGHT + 8;

		if (this.pendingDeletionExpiresAtMillis > 0) {
			long remaining = Math.max(0L, (this.pendingDeletionExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_detail.pending_delete", remaining), x, y, 0xFFCC55);
			y += LINE_HEIGHT + SECTION_GAP;
		}

		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_ids");
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.island_id", this.detail.islandId());
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.owner", this.detail.ownerName() + " (" + this.detail.ownerUuid() + ")");
		y += SECTION_GAP;

		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_location");
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.dimension", this.detail.dimension());
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.grid", this.detail.gridX() + ", " + this.detail.gridZ());
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.size",
				this.detail.size() + "/" + this.detail.maxSize() + " (parcela: " + this.detail.plotSize() + ")");
		y += SECTION_GAP;

		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_members");
		for (ClientMemberView member : this.detail.members()) {
			Text line = Text.literal(member.name() + " ").append(member.role().label())
					.append(Text.literal(" (" + member.uuid() + ")"));
			context.drawTextWithShadow(this.textRenderer, line, x, y, BODY_COLOR);
			y += LINE_HEIGHT;
		}
		y += SECTION_GAP;

		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_state");
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.state", this.detail.state());
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.created_at", this.detail.createdAt());
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.updated_at", this.detail.updatedAt());
		y += SECTION_GAP;

		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_entities");
		ClientAdminIslandDetailView.EntityCounts entities = this.detail.entities();
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_players", String.valueOf(entities.players()));
		y += LINE_HEIGHT;
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_hostile", String.valueOf(entities.hostile()));
		y += LINE_HEIGHT;
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_passive", String.valueOf(entities.passive()));
		y += LINE_HEIGHT;
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_cobblemon", String.valueOf(entities.cobblemon()));
		y += LINE_HEIGHT;
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_items", String.valueOf(entities.items()));
		y += LINE_HEIGHT;
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.entities_other", String.valueOf(entities.other()));
	}

	private int drawSectionHeader(DrawContext context, int x, int y, String key) {
		context.drawTextWithShadow(this.textRenderer, Text.translatable(key), x, y, SECTION_HEADER_COLOR);
		return y + LINE_HEIGHT;
	}

	private int drawLine(DrawContext context, int x, int y, String labelKey, String value) {
		context.drawTextWithShadow(this.textRenderer, Text.translatable(labelKey, value), x, y, BODY_COLOR);
		return y + LINE_HEIGHT;
	}

	private void onDeleteClicked() {
		if (this.detail == null) {
			return;
		}
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						simulateAdminIslandDeleteRequest();
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.admin.island_detail.confirm_title"),
				Text.translatable("islandcoreclient.admin.island_detail.confirm_message",
						Text.literal(this.detail.ownerName()).formatted(Formatting.BOLD))));
	}

	private void onConfirmDeleteClicked() {
		simulateAdminIslandDeleteConfirm();
		this.close();
	}

	// TODO: replace with sending AdminIslandDeleteRequestC2S once IslandCore implements the admin
	// protocol; the server should own the 30s confirmation window, same reasoning as the
	// player-facing DeleteIslandScreen.
	private void simulateAdminIslandDeleteRequest() {
		this.pendingDeletionExpiresAtMillis = System.currentTimeMillis() + PENDING_DELETION_WINDOW_SECONDS * 1000L;
	}

	// TODO: replace with sending AdminIslandDeleteConfirmC2S and awaiting ActionResultS2C once
	// IslandCore implements the admin protocol.
	private void simulateAdminIslandDeleteConfirm() {
		ClientIslandCache.removeAdminIsland(this.ownerUuid);
	}
}
