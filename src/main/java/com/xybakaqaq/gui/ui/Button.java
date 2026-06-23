package com.xybakaqaq.gui.ui;

import java.util.function.IntConsumer;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;

/**
 * A clickable container with an animated hover overlay. Holds child components (e.g. a {@link Text}
 * label). The click handler receives the mouse button (0 = left, 1 = right).
 */
public final class Button extends Component {

	private final IntConsumer onClick;
	private final Anim hover = new Anim(0);
	private int hoverColor = 0x22FFFFFF;

	public Button(IntConsumer onClick) {
		this.onClick = onClick;
	}

	public Button hoverColor(int argb) {
		this.hoverColor = argb;
		return this;
	}

	@Override
	protected void onUpdate(float dt, double mouseX, double mouseY) {
		hover.set(contains(mouseX, mouseY) ? 1f : 0f);
		hover.update(dt);
	}

	@Override
	protected void drawSelf(Canvas canvas, Paint paint) {
		super.drawSelf(canvas, paint);
		float hv = hover.value();
		int baseAlpha = hoverColor >>> 24;
		if (hv > 0.01f && baseAlpha != 0) {
			int alpha = Math.round(baseAlpha * hv);
			paint.setMode(PaintMode.FILL);
			paint.setColor((alpha << 24) | (hoverColor & 0xFFFFFF));
			canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint);
		}
	}

	@Override
	protected boolean onClick(double mouseX, double mouseY, int button) {
		if (onClick != null) {
			onClick.accept(button);
		}
		return true;
	}
}
