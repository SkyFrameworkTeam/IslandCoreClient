package com.skyframework.islandcoreclient.hud;

import com.skyframework.islandcoreclient.state.ClientAllyLocationView;
import com.skyframework.islandcoreclient.state.ClientAllyLocationsCache;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Draws a small icon (the target's own player head via {@link PlayerSkinDrawer} when the client
 * already has their {@link PlayerListEntry} cached, a plain colored square otherwise) for every
 * entry in {@link ClientAllyLocationsCache}, positioned by ANGLE relative to the camera rather
 * than a true 3D-to-screen projection matrix: the angular offset (target yaw/pitch minus camera
 * yaw/pitch) is compared against the current FOV to decide in-view vs. off-screen, then mapped
 * linearly to a screen position. In view, that gives the icon's actual on-screen spot; off
 * screen, the same unclamped linear position is clamped to the nearest point on the screen's
 * inset border along the ray from the screen center — the standard "off-screen direction arrow"
 * technique. Entries closer than {@link #HIDE_DISTANCE_BLOCKS} are skipped entirely, per spec.
 */
public final class AllyHudRenderer {
	private static final double HIDE_DISTANCE_BLOCKS = 15.0;
	// Halved from the original 16 — the head-sized icon read as too prominent/intrusive on the HUD.
	private static final int ICON_SIZE = 8;
	private static final int EDGE_MARGIN = 20;
	private static final int LABEL_GAP = 2;

	private AllyHudRenderer() {
	}

	public static void register() {
		HudRenderCallback.EVENT.register(AllyHudRenderer::onHudRender);
	}

	private static void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.hudHidden) {
			return;
		}

		List<ClientAllyLocationView> entries = ClientAllyLocationsCache.getEntries();
		if (entries.isEmpty()) {
			return;
		}

		Camera camera = client.gameRenderer.getCamera();
		Vec3d camPos = camera.getPos();
		float camYaw = camera.getYaw();
		float camPitch = camera.getPitch();

		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();

		double fov = client.options.getFov().getValue();
		double halfVFov = fov / 2.0;
		double aspect = (double) screenWidth / (double) screenHeight;
		double halfHFov = Math.toDegrees(Math.atan(Math.tan(Math.toRadians(halfVFov)) * aspect));

		for (ClientAllyLocationView entry : entries) {
			double dx = entry.x() - camPos.x;
			double dy = entry.y() - camPos.y;
			double dz = entry.z() - camPos.z;
			double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (distance < HIDE_DISTANCE_BLOCKS) {
				continue;
			}

			double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
			// Standard Minecraft yaw convention: 0 = south (+Z), increasing clockwise viewed from
			// above; pitch: 0 = horizontal, positive = looking down.
			double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
			double targetPitch = Math.toDegrees(-Math.atan2(dy, horizontalDistance));

			double relativeYaw = wrapDegrees(targetYaw - camYaw);
			double relativePitch = targetPitch - camPitch;

			boolean inView = Math.abs(relativeYaw) < halfHFov && Math.abs(relativePitch) < halfVFov;

			double rawX = screenWidth / 2.0 + (relativeYaw / halfHFov) * (screenWidth / 2.0);
			double rawY = screenHeight / 2.0 + (relativePitch / halfVFov) * (screenHeight / 2.0);

			int drawX;
			int drawY;
			if (inView) {
				drawX = (int) Math.round(rawX);
				drawY = (int) Math.round(rawY);
			} else {
				int[] clamped = clampToRect(screenWidth / 2.0, screenHeight / 2.0, rawX, rawY,
						EDGE_MARGIN, EDGE_MARGIN, screenWidth - EDGE_MARGIN, screenHeight - EDGE_MARGIN);
				drawX = clamped[0];
				drawY = clamped[1];
			}

			drawMarker(context, client, entry, drawX, drawY);
		}
	}

	private static void drawMarker(DrawContext context, MinecraftClient client, ClientAllyLocationView entry, int centerX, int centerY) {
		int x = centerX - ICON_SIZE / 2;
		int y = centerY - ICON_SIZE / 2;

		PlayerListEntry listEntry = client.getNetworkHandler() != null ? client.getNetworkHandler().getPlayerListEntry(entry.uuid()) : null;
		if (listEntry != null) {
			PlayerSkinDrawer.draw(context, listEntry.getSkinTextures(), x, y, ICON_SIZE);
		} else {
			// Fallback generic icon: this client has no cached tab-list entry for that uuid (e.g.
			// never rendered them nearby this session) — a plain colored marker rather than
			// skipping the ally entirely, per spec.
			context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFF000000);
			context.fill(x + 1, y + 1, x + ICON_SIZE - 1, y + ICON_SIZE - 1, 0xFF55FF55);
		}

		Text nameText = Text.literal(entry.name());
		int textWidth = client.textRenderer.getWidth(nameText);
		context.drawTextWithShadow(client.textRenderer, nameText, centerX - textWidth / 2, y + ICON_SIZE + LABEL_GAP, 0xFFFFFF);
	}

	private static double wrapDegrees(double degrees) {
		double result = degrees % 360.0;
		if (result >= 180.0) {
			result -= 360.0;
		} else if (result < -180.0) {
			result += 360.0;
		}
		return result;
	}

	// Scales the vector from (centerX, centerY) to (px, py) so it lands exactly on the border of
	// the inset rectangle [left, top, right, bottom] — the point where the ray from the center
	// through the raw (possibly far off-screen) position first crosses that border.
	private static int[] clampToRect(double centerX, double centerY, double px, double py, double left, double top, double right, double bottom) {
		double dx = px - centerX;
		double dy = py - centerY;
		if (dx == 0.0 && dy == 0.0) {
			return new int[] {(int) centerX, (int) centerY};
		}

		double halfWidth = (right - left) / 2.0;
		double halfHeight = (bottom - top) / 2.0;
		double scaleX = dx != 0.0 ? halfWidth / Math.abs(dx) : Double.MAX_VALUE;
		double scaleY = dy != 0.0 ? halfHeight / Math.abs(dy) : Double.MAX_VALUE;
		double scale = Math.min(scaleX, scaleY);

		return new int[] {(int) Math.round(centerX + dx * scale), (int) Math.round(centerY + dy * scale)};
	}
}
