package com.skyframework.islandcoreclient.gui.common;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * A boolean setting row: a button showing "Label: ON/OFF" that flips itself immediately on
 * click (optimistic update) and reports the new value via {@link OnToggle}. Set inactive
 * (via the constructor's {@code enabled} flag) to render read-only, e.g. for non-owners.
 */
public class ToggleRow extends ButtonWidget {
	@FunctionalInterface
	public interface OnToggle {
		void onToggle(boolean newValue);
	}

	private final Text label;
	private final OnToggle onToggle;
	private boolean value;

	public ToggleRow(int x, int y, int width, int height, Text label, boolean initialValue, boolean enabled, OnToggle onToggle) {
		super(x, y, width, height, buildMessage(label, initialValue), button -> {
		}, DEFAULT_NARRATION_SUPPLIER);
		this.label = label;
		this.value = initialValue;
		this.onToggle = onToggle;
		this.active = enabled;
	}

	@Override
	public void onPress() {
		this.value = !this.value;
		this.setMessage(buildMessage(this.label, this.value));
		this.onToggle.onToggle(this.value);
	}

	private static Text buildMessage(Text label, boolean value) {
		return label.copy().append(": ").append(ScreenTexts.onOrOff(value));
	}
}
