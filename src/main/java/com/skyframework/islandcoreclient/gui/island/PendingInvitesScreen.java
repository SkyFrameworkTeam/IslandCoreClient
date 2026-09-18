package com.skyframework.islandcoreclient.gui.island;

import java.util.List;

import com.skyframework.islandcoreclient.gui.common.BaseMenuScreen;
import com.skyframework.islandcoreclient.gui.common.TimeFormat;
import com.skyframework.islandcoreclient.state.ClientIslandCache;
import com.skyframework.islandcoreclient.state.ClientPendingInviteView;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Full-page list of the local island's own outgoing pending invites (5-minute countdown each) —
 * split out of {@link MembersScreen} the same way {@code AdminIslandMembersScreen} is split out of
 * {@code AdminIslandDetailScreen}, reached via a top-bar "Invitaciones pendientes (N)" button in the
 * same reserved right-hand slot. Read-only, same as {@code AdminIslandMembersScreen}: the protocol
 * has no cancel call for an outgoing invite, only accept (see {@code MemberInviteAcceptC2S}) — it
 * simply expires server-side on its own, same as this list already assumed before the split.
 */
public class PendingInvitesScreen extends BaseMenuScreen {
	private static final int CONTENT_X = 16;
	private static final int ROW_HEIGHT = 14;
	private static final int LIST_TOP = TOP_BAR_HEIGHT + 8;

	public PendingInvitesScreen(Screen parent) {
		super(Text.translatable("islandcoreclient.members.pending_invites_title"), parent);
	}

	// Called by ClientPacketHandlers when a fresh IslandSnapshotS2C lands while this screen is open.
	public void refreshFromNetwork() {
		this.clearAndInit();
	}

	@Override
	protected void renderContent(DrawContext context, int mouseX, int mouseY, float delta) {
		List<ClientPendingInviteView> invites = ClientIslandCache.getPendingInvites();
		if (invites.isEmpty()) {
			context.drawTextWithShadow(this.textRenderer,
					Text.translatable("islandcoreclient.members.pending_invites_empty"), CONTENT_X, LIST_TOP, 0xAAAAAA);
			return;
		}

		int y = LIST_TOP;
		for (ClientPendingInviteView invite : invites) {
			String time = TimeFormat.minutesSeconds(invite.getRemainingSeconds());
			context.drawTextWithShadow(this.textRenderer,
					Text.literal(invite.targetName() + " (" + time + ")"), CONTENT_X, y, 0x999999);
			y += ROW_HEIGHT;
		}
	}
}
