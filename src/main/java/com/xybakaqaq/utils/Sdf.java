package com.xybakaqaq.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;

import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/**
 * GPU-shader UI primitives, rendered through Minecraft's programmable {@code RenderPipeline}
 * abstraction ({@code com.mojang.renderpearl} since 26.3, backend-agnostic — runs on both the
 * OpenGL and Vulkan backends). A custom SDF
 * shader draws perfectly anti-aliased rounded rectangles with a vertical gradient, on the GPU.
 *
 * <p>Per-shape parameters are smuggled through standard vertex channels (no custom uniforms, so no
 * custom bind group is needed): UV0 = local pixel position, UV1 = half-size, UV2 = corner radius.
 * The pipeline is compiled lazily on first use, so no static registration is required.
 */
public final class Sdf {

	/** Position + Color + UV0(local px) + UV1(half-size) + UV2(radius). */
	private static final VertexFormat FORMAT = VertexFormat.builder(0)
			.addAttribute("Position", GpuFormat.RGB32_FLOAT)
			.addAttribute("Color", GpuFormat.RGBA8_UNORM)
			.addAttribute("UV0", GpuFormat.RG32_FLOAT)
			.addAttribute("UV1", GpuFormat.RG16_SINT)
			.addAttribute("UV2", GpuFormat.RG16_SINT)
			.build();

	/** SDF rounded-rectangle pipeline — mirrors the vanilla GUI bind groups + translucent blend. */
	public static final RenderPipeline PANEL = RenderPipeline.builder()
			.withLocation(id("pipeline/sdf_panel"))
			.withVertexShader(id("core/sdf_panel"))
			.withFragmentShader(id("core/sdf_panel"))
			.withBindGroupLayout(BindGroupLayouts.GLOBALS)
			.withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
			.withBindGroupLayout(BindGroupLayouts.PROJECTION)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexBinding(0, FORMAT)
			.withPrimitiveTopology(PrimitiveTopology.QUADS)
			.withCull(false)
			.build();

	private Sdf() {
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("zeroday", path);
	}

	/** Draws a rounded rectangle with a top→bottom gradient (ARGB colours). */
	public static void roundRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int colTop, int colBottom) {
		ScreenRectangle bounds = new ScreenRectangle(x, y, w, h);
		graphics.guiRenderState.addGuiElement(new State(
				PANEL, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
				x, y, x + w, y + h, colTop, colBottom, radius, null, bounds));
	}

	/** Draws a solid rounded rectangle. */
	public static void roundRect(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int radius, int color) {
		roundRect(graphics, x, y, w, h, radius, color, color);
	}

	/** A single rounded-rect quad submitted to the GUI render state and drawn with {@link #PANEL}. */
	public record State(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2fc pose,
			int x0, int y0, int x1, int y1,
			int colTop, int colBottom,
			int radius,
			@Nullable ScreenRectangle scissorArea,
			@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {

		@Override
		public void buildVertices(VertexConsumer vc) {
			float hw = (x1 - x0) / 2.0f;
			float hh = (y1 - y0) / 2.0f;
			int ihw = Math.round(hw);
			int ihh = Math.round(hh);
			int r = Math.max(0, Math.min(radius, Math.min(ihw, ihh)));
			vc.addVertexWith2DPose(pose, x0, y0).setColor(colTop).setUv(-hw, -hh).setUv1(ihw, ihh).setUv2(r, 0);
			vc.addVertexWith2DPose(pose, x0, y1).setColor(colBottom).setUv(-hw, hh).setUv1(ihw, ihh).setUv2(r, 0);
			vc.addVertexWith2DPose(pose, x1, y1).setColor(colBottom).setUv(hw, hh).setUv1(ihw, ihh).setUv2(r, 0);
			vc.addVertexWith2DPose(pose, x1, y0).setColor(colTop).setUv(hw, -hh).setUv1(ihw, ihh).setUv2(r, 0);
		}
	}
}
