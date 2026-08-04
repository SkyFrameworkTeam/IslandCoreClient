package com.skyframework.islandcoreclient.gui.admin;

import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.network.ClientErrorToasts;
import com.skyframework.islandcoreclient.network.PendingActionTracker;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDeleteC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDeleteConfirmC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandDetailRequestC2S;
import com.skyframework.islandcoreclient.network.admin.island.AdminIslandListRequestC2S;
import com.skyframework.islandcoreclient.state.ClientAdminIslandDetailView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.Nullable;

/**
 * Real 3-layer delete flow, same shape as {@code DeleteIslandScreen}: request (opens a 30s LOCAL
 * window mirroring IslandDeletionServiceImpl's own 30s server window, exactly like every other
 * confirmation flow in this project) then confirm. The detail itself is requested once from the
 * constructor; {@link #refreshFromNetwork()} only rebuilds from {@link ClientIslandCache}.
 *
 * <p>Layout, verified against an 854x480 window (a small-but-normal size — at Minecraft's default
 * Auto GUI scale that's a virtual screen of ONLY 427x240, not 854x480; every number below is real
 * pixel arithmetic against that 240px height, not eyeballed):
 * <ul>
 *   <li>IDs stays full-width (owner name + full UUID is too long for a half-width column).
 *   <li>Location+State share the LEFT column (44 + gap(8) + 44 = 96px); Entities alone is the
 *   RIGHT column (77px) — deliberately NOT "Location | State+Entities" (that pairing was tried
 *   first and left the taller column at 206px, which doesn't fit); this pairing's taller column
 *   is only 96px. Content bottom = contentStartY(36) + IDs(33) + gap(8) + 96 = 173px.
 *   <li>"Ver miembros (N)" moved OUT of the content column entirely, into the top bar's free
 *   right-hand slot (same slot DashboardScreen's admin toggle / DimensionManagerScreen's create
 *   button already use) — with two stacked buttons below the columns, 173 + gap(8) + delete
 *   button(20) landed inside a few px of the screen edge at 240px; moving one to the top bar
 *   removes it from this budget entirely instead of trimming margins further.
 *   <li>Only "Eliminar esta isla" remains bottom-pinned, at height - BUTTON_Y_FROM_BOTTOM(32) = 208
 *   for a 240px window: comfortably below content bottom (173 + gap = 181) and 32px is enough
 *   headroom for its own 20px height + margin. 240 - 181 = 59px of slack even in the worst case
 *   (pending-deletion banner shown, content bottom becomes 192, still 16px of slack at 208).
 * </ul>
 * Members is NOT rendered here at all: an island's member count is unbounded, so instead of an
 * embedded scrollable box (a previous version of this screen did that, and it still collided with
 * the delete button) this only shows the top-bar button that opens
 * {@link AdminIslandMembersScreen} — a full dedicated page with all the room a long member list
 * could ever need.
 */
public class AdminIslandDetailScreen extends BaseMenuScreen {
	private static final long PENDING_DELETION_WINDOW_SECONDS = 30L;
	private static final int CONTENT_X = 16;
	private static final int LINE_HEIGHT = 11;
	private static final int SECTION_GAP = 8;
	private static final int BUTTON_Y_FROM_BOTTOM = 32;
	private static final int SECTION_HEADER_COLOR = 0xFFDD55;
	private static final int BODY_COLOR = 0xDDDDDD;
	private static final int COLUMN_GAP = 16;
	private static final int TOP_BAR_ACTION_WIDTH = 150;
	private static final int TOP_BAR_ACTION_HEIGHT = 20;

	private final UUID ownerUuid;

	// 0 = no pending deletion. Screen-local UI flow state, same pattern as DeleteIslandScreen.
	private long pendingDeletionExpiresAtMillis = 0L;

	public AdminIslandDetailScreen(UUID ownerUuid, Screen parent) {
		super(Text.translatable("islandcoreclient.admin.island_detail.title"), parent);
		this.ownerUuid = ownerUuid;
		ClientPlayNetworking.send(new AdminIslandDetailRequestC2S(ownerUuid));
	}

	// Called by ClientPacketHandlers when a fresh AdminIslandDetailS2C lands while this screen is
	// open — same pattern as TeleportsScreen/BiomeScreen.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Nullable
	private ClientAdminIslandDetailView detail() {
		return ClientIslandCache.getAdminIslandDetail(this.ownerUuid);
	}

	@Override
	protected void initContent() {
		ClientAdminIslandDetailView detail = this.detail();
		if (detail == null) {
			return;
		}

		// Top bar's free right-hand slot — same slot/height DashboardScreen's admin toggle and
		// DimensionManagerScreen's create button already use. Living here instead of the content
		// column is what actually makes this screen fit an 854x480 window; see the class javadoc.
		this.addDrawableChild(ButtonWidget.builder(
						Text.translatable("islandcoreclient.admin.island_detail.view_members_button", detail.members().size()),
						button -> onViewMembersClicked())
				.dimensions(this.width - 8 - TOP_BAR_ACTION_WIDTH, (TOP_BAR_HEIGHT - TOP_BAR_ACTION_HEIGHT) / 2,
						TOP_BAR_ACTION_WIDTH, TOP_BAR_ACTION_HEIGHT)
				.build());

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
		ClientAdminIslandDetailView detail = this.detail();
		if (detail == null) {
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
		int y = contentStartY();

		if (this.pendingDeletionExpiresAtMillis > 0) {
			long remaining = Math.max(0L, (this.pendingDeletionExpiresAtMillis - System.currentTimeMillis()) / 1000L);
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_detail.pending_delete", remaining), x, y, 0xFFCC55);
			y += LINE_HEIGHT + SECTION_GAP;
		}

		// IDs stays full-width — see the class javadoc on why this section isn't column-split.
		y = drawSectionHeader(context, x, y, "islandcoreclient.admin.island_detail.section_ids");
		y = drawLine(context, x, y, "islandcoreclient.admin.island_detail.island_id", detail.islandId());
		drawLine(context, x, y, "islandcoreclient.admin.island_detail.owner", detail.ownerName() + " (" + detail.ownerUuid() + ")");

		int columnsY = y + LINE_HEIGHT + SECTION_GAP;
		int rightX = rightColumnX();

		// LEFT: Location + State (short/medium lines, safe at half width).
		int leftY = drawSectionHeader(context, x, columnsY, "islandcoreclient.admin.island_detail.section_location");
		leftY = drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.dimension", detail.dimension());
		leftY = drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.grid", detail.gridX() + ", " + detail.gridZ());
		leftY = drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.size",
				detail.size() + "/" + detail.maxSize() + " (parcela: " + detail.plotSize() + ")");
		leftY += SECTION_GAP;

		leftY = drawSectionHeader(context, x, leftY, "islandcoreclient.admin.island_detail.section_state");
		leftY = drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.state", detail.state());
		leftY = drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.created_at", detail.createdAt());
		drawLine(context, x, leftY, "islandcoreclient.admin.island_detail.updated_at", detail.updatedAt());

		// RIGHT: Entities alone — the taller of the two if paired with anything else, so it gets
		// its own column instead (see the class javadoc for the exact numbers).
		int rightY = drawSectionHeader(context, rightX, columnsY, "islandcoreclient.admin.island_detail.section_entities");
		ClientAdminIslandDetailView.EntityCounts entities = detail.entities();
		rightY = drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_players", String.valueOf(entities.players()));
		rightY = drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_hostile", String.valueOf(entities.hostile()));
		rightY = drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_passive", String.valueOf(entities.passive()));
		rightY = drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_cobblemon", String.valueOf(entities.cobblemon()));
		rightY = drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_items", String.valueOf(entities.items()));
		drawLine(context, rightX, rightY, "islandcoreclient.admin.island_detail.entities_other", String.valueOf(entities.other()));
	}

	private int rightColumnX() {
		return this.width / 2 + COLUMN_GAP / 2;
	}

	// y right after this screen's top-of-content, accounting for the optional pending-deletion
	// banner — every field below is always present (no other optional lines), so this is the same
	// value whether used here (for the delete button's Y) or derived procedurally in renderContent.
	private int contentStartY() {
		return TOP_BAR_HEIGHT + 8 + (this.pendingDeletionExpiresAtMillis > 0 ? LINE_HEIGHT + SECTION_GAP : 0);
	}

	private int drawSectionHeader(DrawContext context, int x, int y, String key) {
		context.drawTextWithShadow(this.textRenderer, Text.translatable(key), x, y, SECTION_HEADER_COLOR);
		return y + LINE_HEIGHT;
	}

	private int drawLine(DrawContext context, int x, int y, String labelKey, String value) {
		context.drawTextWithShadow(this.textRenderer, Text.translatable(labelKey, value), x, y, BODY_COLOR);
		return y + LINE_HEIGHT;
	}

	private void onViewMembersClicked() {
		this.client.setScreen(new AdminIslandMembersScreen(this.ownerUuid, this));
	}

	private void onDeleteClicked() {
		ClientAdminIslandDetailView detail = this.detail();
		if (detail == null) {
			return;
		}
		this.client.setScreen(new ConfirmScreen(
				confirmed -> {
					if (confirmed) {
						requestDelete();
					}
					this.client.setScreen(this);
				},
				Text.translatable("islandcoreclient.admin.island_detail.confirm_title"),
				Text.translatable("islandcoreclient.admin.island_detail.confirm_message",
						Text.literal(detail.ownerName()).formatted(Formatting.BOLD))));
	}

	private void requestDelete() {
		ClientPlayNetworking.send(new AdminIslandDeleteC2S(this.ownerUuid));
		PendingActionTracker.await((success, reasonKey) -> {
			if (success) {
				this.pendingDeletionExpiresAtMillis = System.currentTimeMillis() + PENDING_DELETION_WINDOW_SECONDS * 1000L;
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.clearAndInit();
		});
	}

	private void onConfirmDeleteClicked() {
		ClientPlayNetworking.send(new AdminIslandDeleteConfirmC2S(this.ownerUuid));
		PendingActionTracker.await((success, reasonKey) -> {
			this.pendingDeletionExpiresAtMillis = 0L;
			if (success) {
				// The list screen we're about to return to (this.close() -> parent) has no reason
				// to know its cached page just lost a row otherwise — mirrors DeleteIslandScreen's
				// own IslandSnapshotRequestC2S refetch after a successful confirm.
				ClientPlayNetworking.send(new AdminIslandListRequestC2S(0, AdminIslandListScreen.PAGE_SIZE, ""));
			} else {
				ClientErrorToasts.showReason(reasonKey);
			}
			this.close();
		});
	}
}
