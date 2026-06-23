package com.xybakaqaq.modules.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * HUD module: the on-screen ArrayList and the hotbar style.
 *
 * <ul>
 *   <li><b>ArrayList</b> — on/off, default on.</li>
 *   <li><b>Hotbar</b> — {@code off} (vanilla) or {@code zeroday} (custom), default {@code zeroday}.</li>
 * </ul>
 */
public class HUD extends Module {

	private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
	private final ModeSetting hotbar = new ModeSetting("Hotbar", "zeroday", "off", "zeroday");

	public HUD() {
		super("HUD", Category.RENDER, "On-screen HUD: ArrayList and hotbar style.", GLFW.GLFW_KEY_UNKNOWN, true);
	}

	public BooleanSetting arrayList() {
		return arrayList;
	}

	public ModeSetting hotbar() {
		return hotbar;
	}

	/** @return whether the hotbar should use the Zeroday style (vs. vanilla). */
	public boolean isHotbarZeroday() {
		return hotbar.is("zeroday");
	}

	/**
	 * Renders the ArrayList — the enabled modules' names down the top-right corner, sorted by
	 * name width (longest first). Called from the {@code Hud} mixin while the HUD is visible.
	 */
	public void extractArrayList(GuiGraphicsExtractor graphics) {
		if (!arrayList.isEnabled()) {
			return;
		}

		Font font = mc.font;
		List<Module> enabled = new ArrayList<>();
		for (Module module : ModuleManager.getModules()) {
			if (module.isEnabled()) {
				enabled.add(module);
			}
		}
		enabled.sort(Comparator.comparingInt((Module module) -> font.width(module.getName())).reversed());

		int rightEdge = graphics.guiWidth();
		int y = 2;
		for (Module module : enabled) {
			String name = module.getName();
			int width = font.width(name);
			graphics.textWithBackdrop(font, Component.literal(name), rightEdge - width, y, width, -1);
			y += font.lineHeight + 1;
		}
	}

	/**
	 * @return the pixel height of the ArrayList (0 if disabled or empty). Used by the {@code Hud}
	 *         mixin to push the vanilla potion-effect icons down so they sit below it.
	 */
	public int arrayListHeight() {
		if (!arrayList.isEnabled()) {
			return 0;
		}
		int count = 0;
		for (Module module : ModuleManager.getModules()) {
			if (module.isEnabled()) {
				count++;
			}
		}
		return count == 0 ? 0 : 2 + count * (mc.font.lineHeight + 1);
	}
}
