package com.xybakaqaq.gui.ui;

import java.util.ArrayList;
import java.util.List;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;

/**
 * Base UI component: a node in a flexbox tree (Yoga-style layout — flex direction, justify, align,
 * padding, margin, gap, grow), drawn with Skija. Layout is two-pass: {@link #measure} computes
 * intrinsic sizes bottom-up, then {@link #arrange} assigns absolute bounds top-down. Subclasses add
 * content ({@link Text}) or interaction ({@link Button}).
 *
 * <p>Sizes are in GUI pixels. {@code NaN} width/height means "auto" (measured from content).
 */
public class Component {

	public enum Axis {
		ROW, COLUMN
	}

	public enum Justify {
		START, CENTER, END, SPACE_BETWEEN
	}

	public enum Align {
		START, CENTER, END, STRETCH
	}

	// --- style ---
	protected float styleW = Float.NaN;
	protected float styleH = Float.NaN;
	protected float grow;
	protected Axis direction = Axis.COLUMN;
	protected Justify justify = Justify.START;
	protected Align align = Align.START;
	protected float padL, padT, padR, padB;
	protected float marginL, marginT, marginR, marginB;
	protected float gap;
	protected int bg;
	protected float radius;
	protected int borderColor;
	protected float borderWidth;

	// --- free (absolute) positioning: when this container is free, children sit at their pos() ---
	protected boolean freeLayout;
	protected float posX, posY;

	// --- computed bounds (absolute, GUI px) ---
	protected float x, y, w, h;
	protected float measuredW, measuredH;

	public final List<Component> children = new ArrayList<>();

	// ---------- fluent style ----------

	public Component size(float width, float height) {
		this.styleW = width;
		this.styleH = height;
		return this;
	}

	public Component width(float width) {
		this.styleW = width;
		return this;
	}

	public Component height(float height) {
		this.styleH = height;
		return this;
	}

	public Component grow(float grow) {
		this.grow = grow;
		return this;
	}

	public Component dir(Axis direction) {
		this.direction = direction;
		return this;
	}

	public Component justify(Justify justify) {
		this.justify = justify;
		return this;
	}

	public Component align(Align align) {
		this.align = align;
		return this;
	}

	public Component padding(float all) {
		return padding(all, all, all, all);
	}

	public Component padding(float left, float top, float right, float bottom) {
		this.padL = left;
		this.padT = top;
		this.padR = right;
		this.padB = bottom;
		return this;
	}

	public Component margin(float all) {
		this.marginL = this.marginT = this.marginR = this.marginB = all;
		return this;
	}

	public Component gap(float gap) {
		this.gap = gap;
		return this;
	}

	public Component bg(int argb) {
		this.bg = argb;
		return this;
	}

	public Component radius(float radius) {
		this.radius = radius;
		return this;
	}

	public Component border(int color, float width) {
		this.borderColor = color;
		this.borderWidth = width;
		return this;
	}

	public Component add(Component... components) {
		for (Component c : components) {
			children.add(c);
		}
		return this;
	}

	/** Marks this container as free/absolute: children are placed at their {@link #pos}. */
	public Component free() {
		this.freeLayout = true;
		return this;
	}

	/** Absolute offset within a {@link #free} parent's content box. */
	public Component pos(float x, float y) {
		this.posX = x;
		this.posY = y;
		return this;
	}

	public float posX() {
		return posX;
	}

	public float posY() {
		return posY;
	}

	// ---------- layout ----------

	/** Bottom-up: compute intrinsic (auto) size from content. */
	public void measure() {
		for (Component c : children) {
			c.measure();
		}
		float padX = padL + padR;
		float padY = padT + padB;
		float autoMain = 0;
		float autoCross = 0;
		for (int i = 0; i < children.size(); i++) {
			Component c = children.get(i);
			autoMain += main(c.outerW(), c.outerH());
			autoCross = Math.max(autoCross, cross(c.outerW(), c.outerH()));
		}
		if (!children.isEmpty()) {
			autoMain += gap * (children.size() - 1);
		} else {
			autoMain = intrinsicMain();
			autoCross = intrinsicCross();
		}
		float autoW = direction == Axis.ROW ? autoMain + padX : autoCross + padX;
		float autoH = direction == Axis.ROW ? autoCross + padY : autoMain + padY;
		measuredW = Float.isNaN(styleW) ? autoW : styleW;
		measuredH = Float.isNaN(styleH) ? autoH : styleH;
	}

	/** Top-down: place this node at the given bounds and lay out its children. */
	public void arrange(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		if (children.isEmpty()) {
			return;
		}
		float innerX = x + padL;
		float innerY = y + padT;
		float innerW = w - padL - padR;
		float innerH = h - padT - padB;
		if (freeLayout) {
			for (Component c : children) {
				c.arrange(innerX + c.posX, innerY + c.posY, c.measuredW, c.measuredH);
			}
			return;
		}
		float availMain = direction == Axis.ROW ? innerW : innerH;
		float availCross = direction == Axis.ROW ? innerH : innerW;

		int n = children.size();
		float[] mainSize = new float[n];
		float used = 0;
		float totalGrow = 0;
		for (int i = 0; i < n; i++) {
			Component c = children.get(i);
			mainSize[i] = main(c.measuredW, c.measuredH) + main(c.marginL + c.marginR, c.marginT + c.marginB);
			used += mainSize[i];
			totalGrow += c.grow;
		}
		used += gap * (n - 1);
		float free = availMain - used;
		if (free > 0 && totalGrow > 0) {
			for (int i = 0; i < n; i++) {
				mainSize[i] += free * children.get(i).grow / totalGrow;
			}
			free = 0;
		}

		float spacing = gap;
		float offset = 0;
		if (totalGrow == 0 && free > 0) {
			switch (justify) {
				case CENTER -> offset = free / 2f;
				case END -> offset = free;
				case SPACE_BETWEEN -> spacing = gap + (n > 1 ? free / (n - 1) : 0);
				default -> {
				}
			}
		}

		float cursor = (direction == Axis.ROW ? innerX : innerY) + offset;
		for (int i = 0; i < n; i++) {
			Component c = children.get(i);
			float cm = mainSize[i] - main(c.marginL + c.marginR, c.marginT + c.marginB);
			float crossNatural = cross(c.measuredW, c.measuredH);
			float crossMargin = cross(c.marginL + c.marginR, c.marginT + c.marginB);
			float cc = align == Align.STRETCH ? availCross - crossMargin : crossNatural;
			float crossPos;
			switch (align) {
				case CENTER -> crossPos = (availCross - cc - crossMargin) / 2f;
				case END -> crossPos = availCross - cc - crossMargin;
				default -> crossPos = 0;
			}
			float cx;
			float cy;
			float cw;
			float ch;
			if (direction == Axis.ROW) {
				cx = cursor + c.marginL;
				cy = innerY + crossPos + c.marginT;
				cw = cm;
				ch = cc;
			} else {
				cy = cursor + c.marginT;
				cx = innerX + crossPos + c.marginL;
				cw = cc;
				ch = cm;
			}
			c.arrange(cx, cy, cw, ch);
			cursor += mainSize[i] + spacing;
		}
	}

	private float outerW() {
		return measuredW + marginL + marginR;
	}

	private float outerH() {
		return measuredH + marginT + marginB;
	}

	private float main(float wv, float hv) {
		return direction == Axis.ROW ? wv : hv;
	}

	private float cross(float wv, float hv) {
		return direction == Axis.ROW ? hv : wv;
	}

	/** Intrinsic main-axis size for a leaf (overridden by content components like {@link Text}). */
	protected float intrinsicMain() {
		return 0;
	}

	protected float intrinsicCross() {
		return 0;
	}

	// ---------- update / draw ----------

	public void update(float dt, double mouseX, double mouseY) {
		onUpdate(dt, mouseX, mouseY);
		for (Component c : children) {
			c.update(dt, mouseX, mouseY);
		}
	}

	protected void onUpdate(float dt, double mouseX, double mouseY) {
	}

	public void draw(Canvas canvas, Paint paint) {
		drawSelf(canvas, paint);
		for (Component c : children) {
			c.draw(canvas, paint);
		}
	}

	protected void drawSelf(Canvas canvas, Paint paint) {
		if ((bg >>> 24) != 0) {
			paint.setMode(PaintMode.FILL);
			paint.setColor(bg);
			canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint);
		}
		if (borderWidth > 0 && (borderColor >>> 24) != 0) {
			paint.setMode(PaintMode.STROKE);
			paint.setStrokeWidth(borderWidth);
			paint.setColor(borderColor);
			canvas.drawRRect(RRect.makeXYWH(x + 0.5f, y + 0.5f, w - 1, h - 1, radius), paint);
			paint.setMode(PaintMode.FILL);
		}
	}

	// ---------- events ----------

	/** Dispatches a click to the top-most component under the cursor. @return whether handled. */
	public boolean dispatchClick(double mouseX, double mouseY, int button) {
		for (int i = children.size() - 1; i >= 0; i--) {
			if (children.get(i).dispatchClick(mouseX, mouseY, button)) {
				return true;
			}
		}
		return contains(mouseX, mouseY) && onClick(mouseX, mouseY, button);
	}

	protected boolean onClick(double mouseX, double mouseY, int button) {
		return false;
	}

	public boolean contains(double mouseX, double mouseY) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}
}
