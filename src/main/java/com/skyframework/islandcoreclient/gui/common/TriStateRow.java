package com.skyframework.islandcoreclient.gui.common;

import com.skyframework.islandcoreclient.state.ClientTriState;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A 3-state setting row: a button showing "Label: ALLOW/DENY/DEFAULT" (colored per state) that
 * cycles itself immediately on click (optimistic update, ALLOW -&gt; DENY -&gt; DEFAULT -&gt; ALLOW) and
 * reports the new value via {@link OnCycle}. Set inactive (via the constructor's {@code enabled}
 * flag) to render read-only, e.g. for non-owners — same shape as {@link ToggleRow}, for the flag
 * system's 3 states instead of a plain boolean.
 */
public class TriStateRow extends ButtonWidget {
	@FunctionalInterface
	public interface OnCycle {
		void onCycle(ClientTriState newValue);
	}

	private final Text label;
	private final OnCycle onCycle;
	private ClientTriState value;

	public TriStateRow(int x, int y, int width, int height, Text label, ClientTriState initialValue, boolean enabled, OnCycle onCycle) {
		super(x, y, width, height, buildMessage(label, initialValue), button -> {
		}, DEFAULT_NARRATION_SUPPLIER);
		this.label = label;
		this.value = initialValue;
		this.onCycle = onCycle;
		this.active = enabled;
	}

	@Override
	public void onPress() {
		this.value = this.value.next();
		this.setMessage(buildMessage(this.label, this.value));
		this.onCycle.onCycle(this.value);
	}

	private static Text buildMessage(Text label, ClientTriState value) {
		return label.copy().append(": ").append(value.label());
	}
}
