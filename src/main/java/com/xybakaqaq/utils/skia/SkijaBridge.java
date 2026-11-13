package com.xybakaqaq.utils.skia;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.NativeImage;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Pixmap;
import io.github.humbleui.skija.Surface;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

// Shared Skija → MC bridge. Skija renders straight into an MC NativeImage's native memory
// (zero-copy, via Surface#makeRasterDirect) — Skija's RGBA_8888 byte layout matches
// NativeImage's RGBA, so no per-pixel conversion is needed. DynamicTexture#upload() then
// pushes it to the GPU. Skija raster needs no GL context; only the upload touches the GPU, so call
// #render on the render thread.
@SuppressWarnings("deprecation") // some Skija pixel APIs carry deprecation annotations
public final class SkijaBridge implements AutoCloseable {

	private final Identifier textureId;

	private NativeImage nativeImage;
	private DynamicTexture texture;
	private Surface surface;
	private Pixmap pixmap;
	private ByteBuffer buffer;
	private boolean directSurface;
	private int width;
	private int height;
	private long lastKey;
	private boolean hasKey;

	public SkijaBridge(String name) {
		this.textureId = Identifier.fromNamespaceAndPath("zeroday", name);
	}

	public Identifier textureId() {
		return textureId;
	}

	// A content key (deep-hashed over parts) for the cached #render(int, int, long, Consumer).
	public static long key(Object... parts) {
		return Arrays.deepHashCode(parts);
	}

	// Renders drawer into a w×h surface and uploads it. Call on the render thread.
	public void render(int w, int h, Consumer<Canvas> drawer) {
		ensure(w, h);
		draw(drawer);
		hasKey = false;
	}

	// Cached render: skips the raster + GPU upload when key and the size are unchanged from the
	// last call (the texture still holds that content, so the caller just re-blits it). Use a content
	// hash as key for mostly-static widgets to avoid per-frame work. Call on the render thread.
	public void render(int w, int h, long key, Consumer<Canvas> drawer) {
		if (surface != null && w == width && h == height && hasKey && key == lastKey) {
			return;
		}
		ensure(w, h);
		draw(drawer);
		lastKey = key;
		hasKey = true;
	}

	private void draw(Consumer<Canvas> drawer) {
		Canvas canvas = surface.getCanvas();
		canvas.resetMatrix();
		canvas.clear(0x00000000);
		drawer.accept(canvas);
		if (directSurface || surface.readPixels(pixmap, 0, 0)) {
			texture.upload();
		}
	}

	private void ensure(int w, int h) {
		if (surface != null && w == width && h == height) {
			return;
		}
		close();
		width = w;
		height = h;
		nativeImage = new NativeImage(w, h, true);
		ImageInfo info = new ImageInfo(w, h, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL);
		buffer = MemoryUtil.memByteBuffer(nativeImage.getPointer(), Math.toIntExact((long) w * h * 4L));
		pixmap = Pixmap.make(info, buffer, w * 4);
		Surface direct = Surface.makeRasterDirect(pixmap);
		if (direct != null) {
			surface = direct;
			directSurface = true;
		} else {
			surface = Surface.makeRasterN32Premul(w, h);
			directSurface = false;
		}
		texture = new DynamicTexture(() -> "skija", nativeImage);
		Minecraft.getInstance().getTextureManager().register(textureId, texture);
	}

	@Override
	public void close() {
		hasKey = false;
		if (surface != null) {
			surface.close();
			surface = null;
		}
		if (pixmap != null) {
			pixmap.close();
			pixmap = null;
		}
		buffer = null;
		if (texture != null) {
			try {
				Minecraft.getInstance().getTextureManager().release(textureId);
			} catch (Exception ignored) {
				try {
					texture.close();
				} catch (Exception alsoIgnored) {
					// best-effort cleanup
				}
			}
			texture = null;
			nativeImage = null;
		}
	}
}
