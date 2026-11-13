package com.xybakaqaq.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

// World-space rendering helpers, built on the vanilla Gizmos system (the 26.2 replacement
// for hand-rolled Tesselator drawing, which no longer exists). Stateless: the module that draws
// subscribes to Fabric's level render events itself and calls these inside the hook.
public final class RenderUtils {

	private RenderUtils() {
	}

	// Draws a filled + outlined box at world coordinates. May only be called while gizmo submission
	// is active (from a LevelRenderEvents#BEFORE_GIZMOS callback).
	public static void box(AABB box, int strokeArgb, float strokeWidth, int fillArgb, boolean throughWalls) {
		GizmoProperties props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(strokeArgb, strokeWidth, fillArgb));
		if (throughWalls) {
			props.setAlwaysOnTop();
		}
	}

	// Draws a filled + outlined box around a block, slightly inflated to avoid z-fighting.
	public static void blockBox(BlockPos pos, int strokeArgb, float strokeWidth, int fillArgb, boolean throughWalls) {
		box(new AABB(pos).inflate(0.002), strokeArgb, strokeWidth, fillArgb, throughWalls);
	}
}
