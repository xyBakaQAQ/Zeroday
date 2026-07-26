package com.xybakaqaq.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/**
 * World-space rendering helpers, built on the vanilla {@link Gizmos} system (the 26.2 replacement
 * for hand-rolled Tesselator drawing, which no longer exists). Modules register a per-frame
 * callback via {@link #onFrame}; the callbacks run inside Fabric's
 * {@link LevelRenderEvents#BEFORE_GIZMOS} hook, where gizmo submission is active, so they can call
 * {@link #box} / {@link #blockBox} directly.
 */
public final class RenderUtils {

	private static final List<Runnable> FRAME_CALLBACKS = new CopyOnWriteArrayList<>();
	private static final List<BooleanSupplier> OUTLINE_SUPPRESSORS = new CopyOnWriteArrayList<>();

	private RenderUtils() {
	}

	/** Hooks the level render pipeline. Called once from mod init, before modules register. */
	public static void init() {
		LevelRenderEvents.BEFORE_GIZMOS.register(context -> {
			for (Runnable callback : FRAME_CALLBACKS) {
				callback.run();
			}
		});
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, outlineRenderState) -> {
			for (BooleanSupplier suppressor : OUTLINE_SUPPRESSORS) {
				if (suppressor.getAsBoolean()) {
					return false;
				}
			}
			return true;
		});
	}

	/**
	 * Registers a callback invoked every frame while the level is being rendered (gizmo submission
	 * active). Keep it cheap: check whether the module is enabled and return early.
	 */
	public static void onFrame(Runnable callback) {
		FRAME_CALLBACKS.add(callback);
	}

	/** Registers a condition; while it holds, the vanilla block hit outline is not drawn. */
	public static void suppressVanillaBlockOutlineWhen(BooleanSupplier condition) {
		OUTLINE_SUPPRESSORS.add(condition);
	}

	/**
	 * Draws a filled + outlined box at world coordinates. May only be called from an
	 * {@link #onFrame} callback (gizmos need an active collector).
	 *
	 * @param throughWalls draw on top of terrain (depth test off)
	 */
	public static void box(AABB box, int strokeArgb, float strokeWidth, int fillArgb, boolean throughWalls) {
		GizmoProperties props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(strokeArgb, strokeWidth, fillArgb));
		if (throughWalls) {
			props.setAlwaysOnTop();
		}
	}

	/** Draws a filled + outlined box around a block, slightly inflated to avoid z-fighting. */
	public static void blockBox(BlockPos pos, int strokeArgb, float strokeWidth, int fillArgb, boolean throughWalls) {
		box(new AABB(pos).inflate(0.002), strokeArgb, strokeWidth, fillArgb, throughWalls);
	}
}
