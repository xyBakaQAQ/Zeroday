package com.xybakaqaq.gui.targethud;

import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.TargetHUD;
import com.xybakaqaq.utils.CombatTarget;
import com.xybakaqaq.utils.DragState;
import com.xybakaqaq.utils.Sdf;
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

// The Skija-rendered target HUD widget: head (players) + name + an animated health bar. The target is
// the entity you are looking at (crosshair) or most recently attacked — both feed it — kept while alive
// and within the module's distance; with no target, your own info is shown while the chat bar is open.
// Animated: smooth health bar; a hurt punch when the target loses health (the head shrinks
// and flashes red, the panel flashes); and a fade-up on appear / disappear (using cached data so
// it can keep fading even after the target is gone). Rendered at the physical framebuffer resolution.
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
	private static float ghostDelay;
	private static float prevHealth;
	private static float slideIn;
	private static float clock;

	// Seconds the damage ghost holds before it starts draining, so the lost health is readable.
	private static final float GHOST_HOLD = 0.3f;

	// cached display data (so the fade-out keeps working after the entity is gone)
	private static String cachedName = "";
	private static String cachedHpText = "";
	private static float cachedFrac;
	private static boolean cachedHasHead;
	private static Identifier cachedSkin;

	private static final SkijaBridge bridge = new SkijaBridge("targethud");

	private TargetHudWidget() {
	}

	// Called from the attack mixin: remembers the entity the player just hit.
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

		// Frame-rate independent smoothing (1 - e^-rate*dt) so the feel is the same at any FPS.
		appear = approach(appear, present ? 1f : 0f, present ? 16f : 11f, dt);
		hurt = approach(hurt, 0f, 8f, dt);
		slideIn = approach(slideIn, 0f, 14f, dt);
		clock += dt;
		if (!present && appear < 0.02f) {
			displayTarget = null;
			return;
		}

		float targetFrac = cachedFrac;
		if (healthDisplay < 0f) {
			healthDisplay = targetFrac;
			ghostDisplay = targetFrac;
		} else {
			healthDisplay = approach(healthDisplay, targetFrac, 16f, dt);
			if (targetFrac > ghostDisplay) {
				// healed: the ghost snaps up with the bar
				ghostDisplay = targetFrac;
				ghostDelay = GHOST_HOLD;
			} else {
				// damaged: hold briefly, then drain so the lost slice stays readable
				ghostDelay = Math.max(0f, ghostDelay - dt);
				if (ghostDelay <= 0f) {
					ghostDisplay = approach(ghostDisplay, healthDisplay, 7f, dt);
				}
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
		float flash = Math.max(0f, Math.min(1f, hurt));
		// Ease the appear/disappear so it settles instead of just fading linearly.
		float ease = easeOut(appear);
		int alpha = Math.round(Math.max(0f, Math.min(1f, ease)) * 255f);

		int drawX = drag.x() - Math.round(slideIn * 18f);
		int drawY = drag.y() - Math.round((1f - ease) * 6f);

		// Only the static part is baked (face, name, hp text). The bar, the damage ghost and the flash
		// are live GPU quads, so a moving bar never re-rasterises Skija every frame.
		long key = SkijaBridge.key(cachedName, cachedHpText, w, H);
		Window win = Minecraft.getInstance().getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? (float) win.getWidth() / win.getGuiScaledWidth() : 1f;
		int sw = Math.max(1, Math.round(w * scale));
		int sh = Math.max(1, Math.round(H * scale));
		try {
			bridge.render(sw, sh, key, canvas -> {
				canvas.scale(scale, scale);
				draw(canvas, w, contentX, cachedName, cachedHpText);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, bridge.textureId(), drawX, drawY, 0.0F, 0.0F,
					w, H, sw, sh, sw, sh, (alpha << 24) | 0xFFFFFF);
		} catch (Throwable ignored) {
			return;
		}

		// Bar: track, then the white damage residue, then the health. The fill colour follows the
		// displayed value, so it slides from green to red with the bar instead of snapping.
		int barX = drawX + contentX;
		int barY = drawY + 24;
		int barW = w - contentX - 12;
		int barH = 7;
		Sdf.roundRect(graphics, barX, barY, barW, barH, 3, withAlpha(0x55000000, alpha));
		if (ghost > hd + 0.001f) {
			Sdf.roundRect(graphics, barX, barY, Math.max(3, Math.round(barW * ghost)), barH, 3,
					withAlpha(0xAAFFFFFF, alpha));
		}
		if (hd > 0.004f) {
			Sdf.roundRect(graphics, barX, barY, Math.max(3, Math.round(barW * hd)), barH, 3,
					withAlpha(lerp(0xFFE53935, 0xFF6FBF59, hd), alpha));
		}
		if (flash > 0.01f) {
			Sdf.roundRect(graphics, drawX, drawY, w, H, 8,
					withAlpha(0xFF3838, Math.round(alpha * flash * 0.28f)));
		}

		float shake = flash * flash;
		int shakeX = Math.round((float) Math.sin(clock * 45f) * shake * 3f);
		int shakeY = Math.round((float) Math.sin(clock * 53f) * shake * 1.8f);
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
					graphics.fill(boxX, boxY, boxX + HEAD, boxY + HEAD, withAlpha(0xFF3838, Math.round(alpha * flash * 0.43f)));
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

	// Begins a drag if the left button presses inside the widget.
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

	private static void draw(Canvas canvas, int w, int contentX, String name, String hpText) {
		try (Paint bg = new Paint()) {
			bg.setShader(Shader.makeLinearGradient(0, 0, 0, H, new int[] {0xE01D1D24, 0xE013131A}));
			canvas.drawRRect(RRect.makeXYWH(0, 0, w, H, 8), bg);
		}

		try (Font font = new Font(Skia.harmony(), 12f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFFE8E8EA);
			canvas.drawString(trim(font, name, w - contentX - 8), contentX, 18, font, paint);
		}

		try (Font small = new Font(Skia.harmony(), 9f); Paint paint = new Paint()) {
			paint.setAntiAlias(true);
			paint.setColor(0xFF9A9AA2);
			canvas.drawString(hpText, contentX, 42, small, paint);
		}
	}

	// Exponential approach: frame-rate independent, unlike a naive lerp with a dt factor.
	private static float approach(float current, float target, float rate, float dt) {
		return current + (target - current) * (1f - (float) Math.exp(-rate * dt));
	}

	private static float easeOut(float t) {
		float u = 1f - clamp01(t);
		return 1f - u * u * u;
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	// Keeps the colour, replaces its alpha (0..255).
	private static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0xFFFFFF);
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
