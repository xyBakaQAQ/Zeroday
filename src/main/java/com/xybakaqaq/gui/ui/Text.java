package com.xybakaqaq.gui.ui;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Typeface;

// A text label. Measures its own intrinsic size from the font, so it slots straight into the flexbox
// layout. The string and colour are mutable (for MVVM binding via State#bind).
public final class Text extends Component {

	public enum TextAlign {
		LEFT, CENTER, RIGHT
	}

	private String text;
	private final Font font;
	private int color;
	private TextAlign textAlign = TextAlign.LEFT;

	public Text(String text, Typeface typeface, float size, int color) {
		this.text = text == null ? "" : text;
		this.font = new Font(typeface, size);
		this.color = color;
	}

	public Text setText(String text) {
		this.text = text == null ? "" : text;
		return this;
	}

	public Text color(int color) {
		this.color = color;
		return this;
	}

	public Text textAlign(TextAlign align) {
		this.textAlign = align;
		return this;
	}

	@Override
	public void measure() {
		float tw = font.measureTextWidth(text);
		FontMetrics m = font.getMetrics();
		float th = -m.getAscent() + m.getDescent();
		measuredW = Float.isNaN(styleW) ? tw + padL + padR : styleW;
		measuredH = Float.isNaN(styleH) ? (float) Math.ceil(th) + padT + padB : styleH;
	}

	@Override
	protected void drawSelf(Canvas canvas, Paint paint) {
		super.drawSelf(canvas, paint);
		FontMetrics m = font.getMetrics();
		float baseline = y + h / 2f - (m.getAscent() + m.getDescent()) / 2f;
		float tw = font.measureTextWidth(text);
		float tx = switch (textAlign) {
			case CENTER -> x + (w - tw) / 2f;
			case RIGHT -> x + w - padR - tw;
			default -> x + padL;
		};
		paint.setMode(PaintMode.FILL);
		paint.setColor(color);
		canvas.drawString(text, tx, baseline, font, paint);
	}
}
