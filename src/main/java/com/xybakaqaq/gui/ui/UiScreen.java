package com.xybakaqaq.gui.ui;

import com.xybakaqaq.utils.skia.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Paint;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Base screen for the {@link com.xybakaqaq.gui.ui} framework. Hosts a single root component tree and:
 * <ul>
 *   <li>lays it out (flexbox) every frame to the full screen,</li>
 *   <li>renders it with Skija at the <b>physical framebuffer resolution</b> (the canvas is scaled by
 *       the GUI scale, so text is crisp — not the blurry low-res-then-upscaled approach),</li>
 *   <li>ticks animations with real delta time,</li>
 *   <li>routes clicks into the tree.</li>
 * </ul>
 * Subclasses implement {@link #build()} to construct the view (typically from a ViewModel).
 */
public abstract class UiScreen extends Screen {

	private final SkijaBridge bridge;
	protected com.xybakaqaq.gui.ui.Component root;
	private long lastNanos;

	protected UiScreen(String bridgeName, Component title) {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, title);
		this.bridge = new SkijaBridge("ui_" + bridgeName);
	}

	/** Builds the root component tree. Called on open and on resize. */
	protected abstract com.xybakaqaq.gui.ui.Component build();

	/** Dim backdrop colour drawn behind the UI (override to taste). */
	protected int dim() {
		return 0x90101014;
	}

	@Override
	protected void init() {
		root = build();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;

		graphics.fill(0, 0, width, height, dim());

		if (root != null) {
			root.measure();
			root.arrange(0, 0, width, height);
			root.update(dt, mouseX, mouseY);

			Window win = minecraft.getWindow();
			// Render at most 2× the GUI size — full framebuffer res (e.g. 4×) is a huge per-frame
			// raster + upload for no visible gain, since text is already supersampled at 2×.
			float scale = width > 0 ? Math.min(2.0f, (float) win.getWidth() / width) : 1f;
			int rw = Math.max(1, Math.round(width * scale));
			int rh = Math.max(1, Math.round(height * scale));
			try {
				bridge.render(rw, rh, canvas -> {
					canvas.scale(scale, scale);
					try (Paint paint = new Paint()) {
						paint.setAntiAlias(true);
						root.draw(canvas, paint);
					}
				});
				graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), 0, 0, 0.0F, 0.0F,
						width, height, rw, rh, rw, rh);
			} catch (Throwable ignored) {
				// Skija unavailable — nothing drawn.
			}
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		// 26.3 reports SDL buttons (1 = left, 2 = middle, 3 = right). Only left and right mean
		// anything to our components, and filtering here keeps middle/extra buttons inert.
		int button = event.button();
		if (root != null && (button == 1 || button == 3) && root.dispatchClick(event.x(), event.y(), button)) {
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}
}
