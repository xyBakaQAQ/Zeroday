package com.xybakaqaq.gui.targethud;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.config.WindowConfig;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.TargetHUD;
import com.xybakaqaq.utils.Skia;
import com.xybakaqaq.utils.SkijaBridge;

import com.mojang.blaze3d.platform.Window;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The Skija-rendered target HUD widget: head (for players) + name + an animated health bar. The
 * target is the most recently attacked living entity, kept while it is alive and within the module's
 * distance; with no target, your own info is shown while the chat bar is open. Rendered at the
 * physical framebuffer resolution (crisp). Draggable like the CloudMusic widget.
 */
public final class TargetHudWidget {

	private static final int H = 46;
	private static final int HEAD = 28;
	private static final String WINDOW = "targethud";

	private static int x = 10;
	private static int y = 10;
	private static boolean posLoaded;
	private static boolean dragging;
	private static double dragOffX;
	private static double dragOffY;

	private static LivingEntity lastTarget;
	private static LivingEntity animTarget;
	private static float healthDisplay = -1f;
	private static long lastNanos;

	private static final SkijaBridge bridge = new SkijaBridge("targethud");

	private TargetHudWidget() {
	}

	/** Called from the attack mixin: remembers the entity the player just hit. */
	public static void onAttack(Entity entity) {
		if (entity instanceof LivingEntity living) {
			lastTarget = living;
		}
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		if (!posLoaded) {
			x = WindowConfig.getX(WINDOW, x);
			y = WindowConfig.getY(WINDOW, y);
			posLoaded = true;
		}
		if (!chatOpen) {
			dragging = false;
		} else if (dragging) {
			updateDrag();
		}

		LivingEntity target = resolveTarget(chatOpen);
		if (target == null) {
			return;
		}

		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;
		float frac = Math.max(0f, Math.min(1f, target.getHealth() / Math.max(1f, target.getMaxHealth())));
		if (target != animTarget || healthDisplay < 0f) {
			healthDisplay = frac;
			animTarget = target;
		} else {
			healthDisplay += (frac - healthDisplay) * Math.min(1f, dt * 8f);
		}

		boolean hasHead = target instanceof AbstractClientPlayer;
		int w = hasHead ? 180 : 150;
		int contentX = hasHead ? 12 + HEAD + 6 : 12;
		float hd = healthDisplay;
		String name = target.getName().getString();
		String hpText = (int) Math.ceil(target.getHealth()) + " / " + (int) Math.ceil(target.getMaxHealth());

		Window win = Minecraft.getInstance().getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(w * scale));
		int sh = Math.max(1, Math.round(H * scale));
		try {
			bridge.render(sw, sh, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, w, contentX, name, hpText, hd, frac);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), x, y, 0.0F, 0.0F, w, H, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			return;
		}

		// Player head (drawn with MC on top of the Skija panel).
		if (hasHead) {
			Identifier skin = ((AbstractClientPlayer) target).getSkin().body().texturePath();
			int hx = x + 12;
			int hy = y + (H - HEAD) / 2;
			graphics.blit(RenderPipelines.GUI_TEXTURED, skin, hx, hy, 8.0F, 8.0F, HEAD, HEAD, 8, 8, 64, 64);
			graphics.blit(RenderPipelines.GUI_TEXTURED, skin, hx, hy, 40.0F, 8.0F, HEAD, HEAD, 8, 8, 64, 64);
		}
	}

	/** Begins a drag if the left button presses inside the widget. */
	public static boolean mouseClicked(double mx, double my, int button) {
		int w = lastWidth();
		if (button == 0 && mx >= x && mx < x + w && my >= y && my < y + H) {
			dragging = true;
			dragOffX = mx - x;
			dragOffY = my - y;
			return true;
		}
		return false;
	}

	private static int lastWidth() {
		return 180; // generous hit area (max panel width)
	}

	private static LivingEntity resolveTarget(boolean chatOpen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return null;
		}
		TargetHUD module = ModuleManager.get(TargetHUD.class);
		double maxBlocks = module != null ? module.distance() : 6.0;
		if (lastTarget != null && lastTarget.isAlive() && !lastTarget.isRemoved()
				&& mc.player.distanceTo(lastTarget) <= maxBlocks) {
			return lastTarget;
		}
		lastTarget = null;
		return chatOpen ? mc.player : null;
	}

	private static void updateDrag() {
		Minecraft mc = Minecraft.getInstance();
		Window win = mc.getWindow();
		if (GLFW.glfwGetMouseButton(win.handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
			dragging = false;
			WindowConfig.set(WINDOW, x, y);
			return;
		}
		double mx = mc.mouseHandler.xpos() * win.getGuiScaledWidth() / win.getScreenWidth();
		double my = mc.mouseHandler.ypos() * win.getGuiScaledHeight() / win.getScreenHeight();
		x = clamp((int) Math.round(mx - dragOffX), 0, win.getGuiScaledWidth() - 180);
		y = clamp((int) Math.round(my - dragOffY), 0, win.getGuiScaledHeight() - H);
	}

	private static void draw(Canvas canvas, int w, int contentX, String name, String hpText, float healthFrac, float realFrac) {
		try (Paint bg = new Paint()) {
			bg.setShader(Shader.makeLinearGradient(0, 0, 0, H, new int[] {0xE01D1D24, 0xE013131A}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, w, H, 8), bg);
		}

		try (Font font = new Font(Skia.harmony(), 12f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFFE8E8EA);
			canvas.drawString(trim(font, name, w - contentX - 8), contentX, 18, font, paint);
		}

		int barX = contentX;
		int barY = 24;
		int barW = w - contentX - 12;
		int barH = 7;
		try (Paint paint = new Paint()) {
			paint.setColor(0x55000000);
			canvas.drawRRect(RRect.makeXYWH(barX, barY, barW, barH, 3), paint);
			paint.setColor(lerp(0xFFE53935, 0xFF6FBF59, realFrac));
			canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(3f, barW * healthFrac), barH, 3), paint);
		}

		try (Font small = new Font(Skia.harmony(), 9f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFF9A9AA2);
			canvas.drawString(hpText, barX, barY + barH + 11, small, paint);
		}
	}

	private static String trim(Font font, String s, float maxWidth) {
		if (s == null) {
			return "";
		}
		if (font.measureTextWidth(s) <= maxWidth) {
			return s;
		}
		while (s.length() > 1 && font.measureTextWidth(s + "…") > maxWidth) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "…";
	}

	private static int clamp(int v, int lo, int hi) {
		return Math.max(lo, Math.min(v, hi));
	}

	private static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = channel(from, to, t, 24);
		int r = channel(from, to, t, 16);
		int g = channel(from, to, t, 8);
		int b = channel(from, to, t, 0);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int channel(int from, int to, float t, int shift) {
		int f = (from >>> shift) & 0xFF;
		int x = (to >>> shift) & 0xFF;
		return Math.round(f + (x - f) * t);
	}
}
