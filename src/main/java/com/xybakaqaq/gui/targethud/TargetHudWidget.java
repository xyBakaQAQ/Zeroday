package com.xybakaqaq.gui.targethud;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.TargetHUD;
import com.xybakaqaq.utils.CombatTarget;
import com.xybakaqaq.utils.DragState;
import com.xybakaqaq.utils.skia.Skia;
import com.xybakaqaq.utils.skia.SkijaBridge;

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
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The Skija-rendered target HUD widget: head (players) + name + an animated health bar. The target is
 * the entity you are looking at (crosshair) or most recently attacked — both feed it — kept while alive
 * and within the module's distance; with no target, your own info is shown while the chat bar is open.
 *
 * <p>Animated: smooth health bar; a <b>hurt punch</b> when the target loses health (the head shrinks
 * and flashes red, the panel flashes); and a <b>fade-up</b> on appear / disappear (using cached data so
 * it can keep fading even after the target is gone). Rendered at the physical framebuffer resolution.
 */
public final class TargetHudWidget {

	private static final int H = 46;
	private static final int W = 180;
	private static final int HEAD = 28;
	private static final String WINDOW = "targethud";

	private static final DragState drag = new DragState(WINDOW);

	private static LivingEntity displayTarget;
	private static long lastNanos;

	// animation state
	private static float appear;
	private static float hurt;
	private static float healthDisplay = -1f;
	private static float ghostDisplay;
	private static float prevHealth;
	private static float slideIn;
	private static float clock;
	private static long animSeq;

	// cached display data (so the fade-out keeps working after the entity is gone)
	private static String cachedName = "";
	private static String cachedHpText = "";
	private static float cachedFrac;
	private static boolean cachedHasHead;
	private static Identifier cachedSkin;

	private static final SkijaBridge bridge = new SkijaBridge("targethud");

	private TargetHudWidget() {
	}

	/** Called from the attack mixin: remembers the entity the player just hit. */
	public static void onAttack(Entity entity) {
		CombatTarget.onAttack(entity);
	}

	public static void render(GuiGraphicsExtractor graphics, boolean chatOpen) {
		drag.update(chatOpen, W, H, graphics.guiWidth(), graphics.guiHeight(), 10, 10);

		long now = System.nanoTime();
		float dt = lastNanos == 0 ? 0f : Math.min(0.1f, (now - lastNanos) / 1.0e9f);
		lastNanos = now;

		LivingEntity target = resolveTarget(chatOpen);
		boolean present = target != null;
		if (present) {
			if (target != displayTarget) {
				displayTarget = target;
				prevHealth = target.getHealth();
				hurt = 0f;
				healthDisplay = -1f;
				slideIn = 1f;
			}
			capture(target);
		} else if (displayTarget == null) {
			return;
		}

		appear += ((present ? 1f : 0f) - appear) * Math.min(1f, dt * 10f);
		hurt += (0f - hurt) * Math.min(1f, dt * 6f);
		slideIn += (0f - slideIn) * Math.min(1f, dt * 12f);
		clock += dt;
		if (!present && appear < 0.02f) {
			displayTarget = null;
			return;
		}
		if (healthDisplay < 0f) {
			healthDisplay = cachedFrac;
			ghostDisplay = cachedFrac;
		} else {
			healthDisplay += (cachedFrac - healthDisplay) * Math.min(1f, dt * 14f);
			if (cachedFrac > ghostDisplay) {
				ghostDisplay = cachedFrac;
			} else {
				ghostDisplay += (healthDisplay - ghostDisplay) * Math.min(1f, dt * 3f);
			}
		}

		drawWidget(graphics);
	}

	private static void capture(LivingEntity target) {
		cachedName = target.getName().getString();
		float hp = target.getHealth();
		float max = Math.max(1f, target.getMaxHealth());
		if (hp < prevHealth - 0.01f) {
			hurt = 1f;
		}
		prevHealth = hp;
		cachedFrac = Math.max(0f, Math.min(1f, hp / max));
		cachedHpText = (int) Math.ceil(hp) + " / " + (int) Math.ceil(max);
		cachedHasHead = target instanceof AbstractClientPlayer;
		cachedSkin = cachedHasHead ? ((AbstractClientPlayer) target).getSkin().body().texturePath() : null;
	}

	private static void drawWidget(GuiGraphicsExtractor graphics) {
		int w = W;
		int contentX = 12 + HEAD + 6;
		float hd = healthDisplay;
		float ghost = ghostDisplay;
		float frac = cachedFrac;
		float flash = Math.max(0f, Math.min(1f, hurt));
		String name = cachedName;
		String hpText = cachedHpText;

		int drawX = drag.x() - Math.round(slideIn * 18f);
		int drawY = drag.y() - Math.round((1f - appear) * 6f);
		int alpha = Math.round(Math.max(0f, Math.min(1f, appear)) * 255f);

		// The panel texture only depends on name / hp / bar / flash; appear (alpha), slide and shake are
		// applied at blit time, so cache the raster whenever the bar + flash have settled.
		boolean texSettled = flash < 0.01f && Math.abs(hd - frac) < 0.002f && Math.abs(ghost - hd) < 0.002f;
		long key = texSettled ? SkijaBridge.key(name, hpText, Math.round(frac * 1000f)) : ++animSeq;

		Window win = Minecraft.getInstance().getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(w * scale));
		int sh = Math.max(1, Math.round(H * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, w, contentX, name, hpText, hd, ghost, frac, flash);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), drawX, drawY, 0.0F, 0.0F,
					w, H, sw, sh, sw, sh, (alpha << 24) | 0xFFFFFF);
		} catch (Throwable ignored) {
			return;
		}

		int shakeX = Math.round((float) Math.sin(clock * 45f) * flash * 2.5f);
		int shakeY = Math.round((float) Math.sin(clock * 53f) * flash * 1.6f);
		int boxX = drawX + 12 + shakeX;
		int boxY = drawY + (H - HEAD) / 2 + shakeY;
		float shrink = 1f - flash * 0.22f;

		if (cachedHasHead && cachedSkin != null) {
			int hs = Math.round(HEAD * shrink);
			int fhx = boxX + (HEAD - hs) / 2;
			int fhy = boxY + (HEAD - hs) / 2;
			int tint = (alpha << 24) | (lerp(0xFFFFFFFF, 0xFFFF5A5A, flash) & 0xFFFFFF);
			graphics.blit(RenderPipelines.GUI_TEXTURED, cachedSkin, fhx, fhy, 8.0F, 8.0F, hs, hs, 8, 8, 64, 64, tint);
			graphics.blit(RenderPipelines.GUI_TEXTURED, cachedSkin, fhx, fhy, 40.0F, 8.0F, hs, hs, 8, 8, 64, 64, tint);
		} else {
			LivingEntity entity = displayTarget;
			if (entity != null && entity.isAlive() && !entity.isRemoved() && appear > 0.35f) {
				renderEntityHead(graphics, entity, boxX, boxY, shrink);
				if (flash > 0.01f) {
					graphics.fill(boxX, boxY, boxX + HEAD, boxY + HEAD, (Math.round(flash * 110f) << 24) | 0xFF3838);
				}
			}
		}
	}

	private static void renderEntityHead(GuiGraphicsExtractor graphics, LivingEntity entity, int boxX, int boxY, float shrink) {
		EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
		EntityRenderer<? super LivingEntity, ?> renderer = dispatcher.getRenderer(entity);
		EntityRenderState renderState = renderer.createRenderState(entity, 1.0F);
		renderState.outlineColor = 0;
		renderState.shadowPieces.clear();
		float boxHeight;
		if (renderState instanceof LivingEntityRenderState living) {
			living.bodyRot = 180.0F;
			living.yRot = 0.0F;
			living.xRot = 0.0F;
			living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
			living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
			living.scale = 1.0F;
			boxHeight = living.boundingBoxHeight;
		} else {
			boxHeight = renderState.boundingBoxHeight;
		}
		float size = HEAD * 0.82f / Math.max(0.6f, boxHeight) * shrink;
		Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
		Vector3f translation = new Vector3f(0.0F, boxHeight / 2.0F, 0.0F);
		graphics.entity(renderState, size, translation, rotation, null, boxX, boxY, boxX + HEAD, boxY + HEAD);
	}

	/** Begins a drag if the left button presses inside the widget. */
	public static boolean mouseClicked(double mx, double my, int button) {
		return drag.mouseClicked(mx, my, button);
	}

	private static LivingEntity resolveTarget(boolean chatOpen) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) {
			return null;
		}
		TargetHUD module = ModuleManager.get(TargetHUD.class);
		double maxBlocks = module != null ? module.distance() : 6.0;
		LivingEntity target = CombatTarget.resolve(maxBlocks);
		if (target != null) {
			return target;
		}
		return chatOpen ? mc.player : null;
	}

	private static void draw(Canvas canvas, int w, int contentX, String name, String hpText, float healthFrac, float ghostFrac, float realFrac, float flash) {
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
			// damage ghost — white residue trailing the lost health
			if (ghostFrac > healthFrac + 0.001f) {
				paint.setColor(0xAAFFFFFF);
				canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(3f, barW * ghostFrac), barH, 3), paint);
			}
			paint.setColor(lerp(0xFFE53935, 0xFF6FBF59, realFrac));
			canvas.drawRRect(RRect.makeXYWH(barX, barY, Math.max(3f, barW * healthFrac), barH, 3), paint);
		}

		try (Font small = new Font(Skia.harmony(), 9f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFF9A9AA2);
			canvas.drawString(hpText, barX, barY + barH + 11, small, paint);
		}

		if (flash > 0.01f) {
			try (Paint red = new Paint()) {
				red.setColor((Math.round(flash * 70f) << 24) | 0xFF3838);
				canvas.drawRRect(RRect.makeXYWH(0, 0, w, H, 8), red);
			}
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
