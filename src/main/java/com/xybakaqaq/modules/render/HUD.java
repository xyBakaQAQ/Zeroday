package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;

/**
 * HUD module (Skija-rendered widgets): the draggable ArrayList and potion display, and the hotbar
 * style.
 *
 * <ul>
 *   <li><b>ArrayList</b> — enabled modules, draggable (default top-right). Default on.</li>
 *   <li><b>Hotbar</b> — {@code off} (vanilla) or {@code zeroday} (custom). Default {@code zeroday}.</li>
 *   <li><b>Potions</b> — custom potion display (suppresses vanilla), draggable (default bottom-right).</li>
 * </ul>
 *
 * <p>The actual rendering lives in the {@code gui.arraylist} / {@code gui.potions} widgets; this module
 * just holds the settings and gates them.
 */
public class HUD extends Module {

	/** Row height of the ArrayList, mirrored by the widget — used to push vanilla effects below it. */
	private static final int ARRAYLIST_ROW_H = 13;

	private final BooleanSetting arrayList = new BooleanSetting("ArrayList", true);
	private final ModeSetting hotbar = new ModeSetting("Hotbar", "zeroday", "off", "zeroday");
	private final BooleanSetting potions = new BooleanSetting("Potions", true);
	private final BooleanSetting inventory = new BooleanSetting("Inventory", false);

	public HUD() {
		super("HUD", Category.RENDER, "On-screen HUD: ArrayList, hotbar style, potions.", InputConstants.UNKNOWN.getValue(), true);
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

	/** @return whether the custom potion-effect display (and vanilla suppression) is active. */
	public boolean potions() {
		return potions.isEnabled();
	}

	/** @return whether the inventory (main storage, no hotbar) display is active. */
	public boolean inventory() {
		return inventory.isEnabled();
	}

	/**
	 * @return the ArrayList's pixel height (0 if disabled or empty). Used by the {@code Hud} mixin to
	 *         push the vanilla potion icons below it when the custom Potions display is off.
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
		return count == 0 ? 0 : count * ARRAYLIST_ROW_H;
	}
}
