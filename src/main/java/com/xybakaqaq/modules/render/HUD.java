package com.xybakaqaq.modules.render;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;

// HUD module (Skija-rendered widgets): the draggable ArrayList and potion display, and the hotbar
// style.
// <ul>
// <li>ArrayList — enabled modules, draggable (default top-right). Default on.</li>
// <li>Hotbar — off (vanilla) or zeroday (custom). Default zeroday.</li>
// <li>Potions — custom potion display (suppresses vanilla), draggable (default bottom-right).</li>
// </ul>
// The actual rendering lives in the gui.arraylist / gui.potions widgets; this module
// just holds the settings and gates them.
public class HUD extends Module {

	// Row height of the ArrayList, mirrored by the widget — used to push vanilla effects below it.
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


	public boolean isHotbarZeroday() {
		return hotbar.is("zeroday");
	}


	public boolean potions() {
		return potions.isEnabled();
	}


	public boolean inventory() {
		return inventory.isEnabled();
	}

	// push the vanilla potion icons below it when the custom Potions display is off.
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
