package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;

/**
 * Removes world darkness by forcing the gamma (brightness) used by the lightmap to a high value.
 * The actual override lives in {@code LightmapRenderStateExtractorMixin}; this is just the toggle.
 */
public final class FullBright extends Module {

	public FullBright() {
		super("FullBright", Category.RENDER, "Max brightness — removes darkness.", InputConstants.UNKNOWN.getValue(),true);
	}
}
