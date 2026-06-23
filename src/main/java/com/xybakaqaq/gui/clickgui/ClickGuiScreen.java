package com.xybakaqaq.gui.clickgui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.config.WindowConfig;
import com.xybakaqaq.gui.ui.Anim;
import com.xybakaqaq.gui.ui.Component;
import com.xybakaqaq.gui.ui.Text;
import com.xybakaqaq.gui.ui.UiScreen;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ModeSetting;
import com.xybakaqaq.modules.settings.NumberSetting;
import com.xybakaqaq.modules.settings.Setting;
import com.xybakaqaq.utils.Skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * The ClickGUI, built on the {@link com.xybakaqaq.gui.ui} framework: one draggable, flexbox-laid-out
 * panel per {@link Category}. Rendered crisply (physical-resolution Skija + HarmonyOS font) with
 * animated hover / on-off transitions. Left-click a module to toggle, right-click to expand its
 * settings + keybind. Panel positions persist to {@code windows.json}.
 */
public class ClickGuiScreen extends UiScreen {

	private static final int PANEL_W = 116;
	private static final int HEADER_H = 20;
	private static final int ROW_H = 16;

	private static final int PANEL_BG = 0xE01A1A20;
	private static final int HEADER_BG = 0xFF24242F;
	private static final int ACCENT = 0xFFC20C0C;
	private static final int ENABLED_BG = 0x66C20C0C;
	private static final int TEXT = 0xFFE8E8EA;
	private static final int SUBTEXT = 0xFF8A8A92;

	private final Font bodyFont = new Font(Skia.harmony(), 11f);
	private final Font headFont = new Font(Skia.harmonyMedium(), 12f);

	private final Map<Category, float[]> pos = new EnumMap<>(Category.class);
	private final Map<Category, Component> panels = new EnumMap<>(Category.class);
	private final Map<Category, Component> headers = new EnumMap<>(Category.class);
	private final Set<Module> expanded = new HashSet<>();
	private final Set<ModeSetting> openModes = new HashSet<>();

	private Category dragCat;
	private double dragOffX;
	private double dragOffY;
	private Module binding;
	private NumberRow activeSlider;

	public ClickGuiScreen() {
		super("clickgui", net.minecraft.network.chat.Component.literal("ClickGUI"));
		int index = 0;
		for (Category cat : Category.values()) {
			if (modulesIn(cat).isEmpty()) {
				continue;
			}
			float dx = 8 + index * (PANEL_W + 8);
			pos.put(cat, new float[] {
					WindowConfig.getX("clickgui_" + cat, (int) dx),
					WindowConfig.getY("clickgui_" + cat, 8)
			});
			index++;
		}
	}

	private static List<Module> modulesIn(Category cat) {
		List<Module> list = new ArrayList<>();
		for (Module module : ModuleManager.getModules()) {
			if (module.getCategory() == cat) {
				list.add(module);
			}
		}
		return list;
	}

	@Override
	protected Component build() {
		panels.clear();
		headers.clear();
		Component root = new Component().free().size(width, height);
		for (Category cat : Category.values()) {
			List<Module> mods = modulesIn(cat);
			if (mods.isEmpty()) {
				continue;
			}
			float[] p = pos.get(cat);
			Component panel = new Component()
					.dir(Component.Axis.COLUMN)
					.align(Component.Align.STRETCH)
					.width(PANEL_W)
					.bg(PANEL_BG)
					.radius(5)
					.pos(p[0], p[1]);

			Component header = new Text(catName(cat), Skia.harmonyMedium(), 12f, TEXT)
					.textAlign(Text.TextAlign.LEFT)
					.height(HEADER_H)
					.padding(9, 0, 9, 0)
					.bg(HEADER_BG)
					.radius(5);
			panel.add(header);

			for (Module module : mods) {
				panel.add(new ModuleRow(module));
				if (expanded.contains(module)) {
					for (Setting<?> setting : module.getSettings()) {
						if (setting instanceof ModeSetting modeSetting) {
							panel.add(new ModeHeaderRow(modeSetting));
							if (openModes.contains(modeSetting)) {
								for (String option : modeSetting.getModes()) {
									panel.add(new ModeOptionRow(modeSetting, option));
								}
							}
						} else if (setting instanceof NumberSetting numberSetting) {
							panel.add(new NumberRow(numberSetting));
						} else if (setting instanceof BooleanSetting booleanSetting) {
							panel.add(new SettingRow(booleanSetting));
						}
					}
					panel.add(new BindRow(module));
				}
			}
			panels.put(cat, panel);
			headers.put(cat, header);
			root.add(panel);
		}
		return root;
	}

	private void rebuild() {
		root = build();
	}

	// ---------- input ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x();
		double my = event.y();
		if (event.button() == 0) {
			for (Map.Entry<Category, Component> entry : headers.entrySet()) {
				if (entry.getValue().contains(mx, my)) {
					dragCat = entry.getKey();
					Component panel = panels.get(dragCat);
					dragOffX = mx - panel.posX();
					dragOffY = my - panel.posY();
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (activeSlider != null) {
			activeSlider.applyAt(event.x());
			return true;
		}
		if (dragCat != null) {
			float nx = (float) (event.x() - dragOffX);
			float ny = (float) (event.y() - dragOffY);
			panels.get(dragCat).pos(nx, ny);
			pos.get(dragCat)[0] = nx;
			pos.get(dragCat)[1] = ny;
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (activeSlider != null) {
			activeSlider = null;
			return true;
		}
		if (dragCat != null) {
			float[] p = pos.get(dragCat);
			WindowConfig.set("clickgui_" + dragCat, (int) p[0], (int) p[1]);
			dragCat = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (binding != null) {
			int key = event.key();
			binding.setKey(key == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : key);
			binding = null;
			return true;
		}
		return super.keyPressed(event);
	}

	// ---------- rows (custom components) ----------

	private final class ModuleRow extends Component {
		private final Module module;
		private final Anim hover = new Anim(0);
		private final Anim on;

		ModuleRow(Module module) {
			this.module = module;
			this.on = new Anim(module.isEnabled() ? 1f : 0f);
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
			on.set(module.isEnabled() ? 1f : 0f);
			on.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			paint.setMode(PaintMode.FILL);
			if (hover.value() > 0.01f) {
				paint.setColor(alpha(0x22FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			if (on.value() > 0.01f) {
				paint.setColor(alpha(ENABLED_BG, on.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
				paint.setColor(alpha(ACCENT, on.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, 2, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, module.getName(), x + 8, y, h, lerp(SUBTEXT, TEXT, on.value()));
			drawText(canvas, bodyFont, paint, expanded.contains(module) ? "–" : "+", x + w - 12, y, h, SUBTEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			if (button == 1) {
				if (!expanded.remove(module)) {
					expanded.add(module);
				}
				rebuild();
			} else {
				module.toggle();
			}
			return true;
		}
	}

	private final class SettingRow extends Component {
		private final BooleanSetting setting;
		private final Anim hover = new Anim(0);

		SettingRow(BooleanSetting setting) {
			this.setting = setting;
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			if (hover.value() > 0.01f) {
				paint.setMode(PaintMode.FILL);
				paint.setColor(alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, setting.getName(), x + 14, y, h, SUBTEXT);
			drawTextRight(canvas, bodyFont, paint, setting.isEnabled() ? "ON" : "OFF", x + w - 8, y, h,
					setting.isEnabled() ? ACCENT : SUBTEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			setting.toggle();
			return true;
		}
	}

	/** Header row of a mode setting: shows the current value; click opens/closes its dropdown. */
	private final class ModeHeaderRow extends Component {
		private final ModeSetting setting;
		private final Anim hover = new Anim(0);

		ModeHeaderRow(ModeSetting setting) {
			this.setting = setting;
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			if (hover.value() > 0.01f) {
				paint.setMode(PaintMode.FILL);
				paint.setColor(alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, setting.getName(), x + 14, y, h, SUBTEXT);
			drawTextRight(canvas, bodyFont, paint, setting.getValue(), x + w - 8, y, h, ACCENT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			if (!openModes.remove(setting)) {
				openModes.add(setting);
			}
			rebuild();
			return true;
		}
	}

	/** One option inside an open mode dropdown; click selects it and closes the dropdown. */
	private final class ModeOptionRow extends Component {
		private final ModeSetting setting;
		private final String option;
		private final Anim hover = new Anim(0);

		ModeOptionRow(ModeSetting setting, String option) {
			this.setting = setting;
			this.option = option;
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			boolean selected = setting.getValue().equals(option);
			paint.setMode(PaintMode.FILL);
			if (selected) {
				paint.setColor(0x33C20C0C);
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			} else if (hover.value() > 0.01f) {
				paint.setColor(alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, option, x + 24, y, h, selected ? ACCENT : SUBTEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			setting.setValue(option);
			openModes.remove(setting);
			rebuild();
			return true;
		}
	}

	/** A number setting: name + value + a draggable slider track. Click or drag to set the value. */
	private final class NumberRow extends Component {
		private final NumberSetting setting;
		private final Anim hover = new Anim(0);

		NumberRow(NumberSetting setting) {
			this.setting = setting;
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			paint.setMode(PaintMode.FILL);
			if (hover.value() > 0.01f) {
				paint.setColor(alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, setting.getName(), x + 14, y, h, SUBTEXT);
			drawTextRight(canvas, bodyFont, paint, String.format(Locale.ROOT, "%.2f", setting.getValue()), x + w - 8, y, h, ACCENT);

			float tx = x + 8;
			float tw = w - 16;
			float ty = y + h - 3;
			double range = setting.getMax() - setting.getMin();
			float frac = range == 0 ? 0f : (float) ((setting.getValue() - setting.getMin()) / range);
			frac = Math.max(0f, Math.min(1f, frac));
			paint.setColor(0x33FFFFFF);
			canvas.drawRRect(RRect.makeXYWH(tx, ty, tw, 2, 1), paint);
			paint.setColor(ACCENT);
			canvas.drawRRect(RRect.makeXYWH(tx, ty, Math.max(1f, tw * frac), 2, 1), paint);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			activeSlider = this;
			applyAt(mx);
			return true;
		}

		void applyAt(double mx) {
			float tx = x + 8;
			float tw = w - 16;
			double frac = tw == 0 ? 0 : (mx - tx) / tw;
			frac = Math.max(0.0, Math.min(1.0, frac));
			setting.setNumber(setting.getMin() + frac * (setting.getMax() - setting.getMin()));
		}
	}

	private final class BindRow extends Component {
		private final Module module;
		private final Anim hover = new Anim(0);

		BindRow(Module module) {
			this.module = module;
			height(ROW_H);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
			hover.set(contains(mx, my) ? 1f : 0f);
			hover.update(dt);
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			if (hover.value() > 0.01f) {
				paint.setMode(PaintMode.FILL);
				paint.setColor(alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, "Bind", x + 14, y, h, SUBTEXT);
			String key = binding == module ? "..." : keyName(module.getKey());
			drawTextRight(canvas, bodyFont, paint, key, x + w - 8, y, h, TEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			binding = module;
			return true;
		}
	}

	// ---------- helpers ----------

	private static void drawText(Canvas canvas, Font font, Paint paint, String s, float tx, float rowY, float rowH, int color) {
		FontMetrics m = font.getMetrics();
		float baseline = rowY + rowH / 2f - (m.getAscent() + m.getDescent()) / 2f;
		paint.setMode(PaintMode.FILL);
		paint.setColor(color);
		canvas.drawString(s, tx, baseline, font, paint);
	}

	private static void drawTextRight(Canvas canvas, Font font, Paint paint, String s, float right, float rowY, float rowH, int color) {
		drawText(canvas, font, paint, s, right - font.measureTextWidth(s), rowY, rowH, color);
	}

	private static int alpha(int color, float factor) {
		int a = Math.round((color >>> 24) * Math.max(0f, Math.min(1f, factor)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	private static int lerp(int from, int to, float t) {
		t = Math.max(0f, Math.min(1f, t));
		int a = lerpChannel(from, to, t, 24);
		int r = lerpChannel(from, to, t, 16);
		int g = lerpChannel(from, to, t, 8);
		int b = lerpChannel(from, to, t, 0);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	private static int lerpChannel(int from, int to, float t, int shift) {
		int f = (from >>> shift) & 0xFF;
		int tt = (to >>> shift) & 0xFF;
		return Math.round(f + (tt - f) * t);
	}

	private static String catName(Category cat) {
		String n = cat.name();
		return n.charAt(0) + n.substring(1).toLowerCase();
	}

	private static String keyName(int key) {
		if (key == GLFW.GLFW_KEY_UNKNOWN) {
			return "None";
		}
		String name = GLFW.glfwGetKeyName(key, 0);
		if (name != null) {
			return name.toUpperCase();
		}
		return switch (key) {
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
			case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
			case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
			case GLFW.GLFW_KEY_SPACE -> "SPACE";
			case GLFW.GLFW_KEY_TAB -> "TAB";
			default -> "KEY" + key;
		};
	}
}
