package com.skyframework.islandcoreclient.gui.admin;

import java.util.List;
import java.util.UUID;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.state.ClientAdminIslandDetailView;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientMemberView;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import org.jetbrains.annotations.Nullable;

/**
 * Full-page member list for an admin-viewed island, split out of {@link AdminIslandDetailScreen}
 * since a member count is unbounded (unlike everything else on that screen). Reads from the same
 * {@link ClientIslandCache#getAdminIslandDetail(UUID)} cache the parent screen already populated —
 * no separate network request, same data, same {@link #refreshFromNetwork()} pattern as every
 * other Admin screen in case a fresh reply lands while this screen is the one open.
 */
public class AdminIslandMembersScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int ROW_HEIGHT = 14;
	private static final int LIST_START_Y = TOP_BAR_HEIGHT + 8;

	private final UUID ownerUuid;

	public AdminIslandMembersScreen(UUID ownerUuid, Screen parent) {
		super(Text.translatable("islandcoreclient.admin.island_members.title"), parent);
		this.ownerUuid = ownerUuid;
	}

	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Nullable
	private ClientAdminIslandDetailView detail() {
		return ClientIslandCache.getAdminIslandDetail(this.ownerUuid);
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		ClientAdminIslandDetailView detail = this.detail();
		if (detail == null) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_detail.not_found"), CONTENT_X, LIST_START_Y, 0xAAAAAA);
			return;
		}

		List<ClientMemberView> members = detail.members();
		if (members.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.admin.island_members.empty"), CONTENT_X, LIST_START_Y, 0xAAAAAA);
			return;
		}

		int y = LIST_START_Y;
		for (ClientMemberView member : members) {
			Text line = Text.literal(member.name() + " ").append(member.role().label())
					.append(Text.literal(" (" + member.uuid() + ")"));
			context.drawTextWithShadow(this.textRenderer, line, CONTENT_X, y, 0xDDDDDD);
			y += ROW_HEIGHT;
		}
	}
}
