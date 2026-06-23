package com.xybakaqaq.gui.cloudmusic;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Image decoding helper. NetEase covers are JPEG, which MC's {@code NativeImage.read} rejects (it
 * validates a PNG header), so we decode via ImageIO and copy the pixels into a {@link NativeImage}.
 */
public final class Images {

	private Images() {
	}

	public static NativeImage decode(byte[] bytes) throws IOException {
		BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
		if (img == null) {
			throw new IOException("unsupported image format");
		}
		int w = img.getWidth();
		int h = img.getHeight();
		NativeImage image = new NativeImage(w, h, false);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = img.getRGB(x, y);
				int a = (argb >>> 24) & 0xff;
				int r = (argb >> 16) & 0xff;
				int g = (argb >> 8) & 0xff;
				int b = argb & 0xff;
				image.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
			}
		}
		return image;
	}
}
