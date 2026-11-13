package com.xybakaqaq.gui.clickgui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.platform.InputConstants;

import com.xybakaqaq.config.WindowConfig;
import com.xybakaqaq.gui.ui.Anim;
import com.xybakaqaq.gui.ui.Component;
import com.xybakaqaq.gui.ui.Text;
import com.xybakaqaq.gui.ui.UiScreen;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.settings.BooleanSetting;
import com.xybakaqaq.modules.settings.ColorSetting;
import com.xybakaqaq.modules.settings.ModeSetting;
import com.xybakaqaq.modules.settings.NumberSetting;
import com.xybakaqaq.modules.settings.Setting;
import com.xybakaqaq.modules.settings.StringSetting;
import com.xybakaqaq.utils.KeyUtils;
import com.xybakaqaq.utils.skia.Skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

// One draggable panel per Category. Left-click a module to toggle it, right-click to expand
// its settings and keybind. Panel positions persist to windows.json.
public class ClickGuiScreen extends UiScreen {

	// Minimum panel width; panels widen to fit their longest row.
	private static final int PANEL_W = 116;
	private static final int HEADER_H = 20;
	private static final int ROW_H = 16;
	private static final int PICKER_H = 84;

	// Gap between a row's label and its right-aligned value.
	private static final float ROW_GAP = 12f;

	// #pos sentinel: never dragged, so the panel sits in the auto column.
	private static final int AUTO_X = Integer.MIN_VALUE;

	// 26.3 reports SDL mouse buttons (1 = left, 2 = middle, 3 = right), not GLFW's 0/1/2.
	private static final int MOUSE_LEFT = 1;
	private static final int MOUSE_RIGHT = 3;

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

	// UI state remembered for the rest of the run (not written to disk)
	private static final Set<Module> expanded = new HashSet<>();
	private static final Set<ModeSetting> openModes = new HashSet<>();
	private static final Set<ColorSetting> openColors = new HashSet<>();

	private Category dragCat;
	private double dragOffX;
	private double dragOffY;
	private Module binding;
	private StringSetting editingString;
	private NumberRow activeSlider;
	private ColorPickerRow activeColor;

	public ClickGuiScreen() {
		super("clickgui", net.minecraft.network.chat.Component.literal("ClickGUI"));
		for (Category cat : Category.values()) {
			if (modulesIn(cat).isEmpty()) {
				continue;
			}
			// AUTO_X: never dragged, so build() places it in the auto-sized column.
			pos.put(cat, new float[] {
					WindowConfig.getX("clickgui_" + cat, AUTO_X),
					WindowConfig.getY("clickgui_" + cat, 8)
			});
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

		// Measure every panel first so the never-dragged ones share a uniform column width.
		List<Category> cats = new ArrayList<>();
		List<Component> built = new ArrayList<>();
		float columnW = PANEL_W;
		for (Category cat : Category.values()) {
			List<Module> mods = modulesIn(cat);
			if (mods.isEmpty()) {
				continue;
			}
			Component panel = new Component()
					.dir(Component.Axis.COLUMN)
					.align(Component.Align.STRETCH)
					.minWidth(PANEL_W)
					.bg(PANEL_BG)
					.radius(5);

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
						if (!setting.isVisible()) {
							continue;
						}
						if (setting instanceof ModeSetting modeSetting) {
							panel.add(new ModeHeaderRow(modeSetting));
							if (openModes.contains(modeSetting)) {
								for (String option : modeSetting.getModes()) {
									panel.add(new ModeOptionRow(modeSetting, option));
								}
							}
						} else if (setting instanceof NumberSetting numberSetting) {
							panel.add(new NumberRow(numberSetting));
						} else if (setting instanceof ColorSetting colorSetting) {
							panel.add(new ColorRow(colorSetting));
							if (openColors.contains(colorSetting)) {
								panel.add(new ColorPickerRow(colorSetting));
							}
						} else if (setting instanceof BooleanSetting booleanSetting) {
							panel.add(new SettingRow(booleanSetting));
						} else if (setting instanceof StringSetting stringSetting) {
							panel.add(new StringRow(stringSetting));
						}
					}
					panel.add(new BindRow(module));
				}
			}
			panel.measure();
			columnW = Math.max(columnW, panel.measuredW());
			cats.add(cat);
			built.add(panel);
		}

		for (int i = 0; i < built.size(); i++) {
			Category cat = cats.get(i);
			Component panel = built.get(i);
			float[] p = pos.get(cat);
			float x = p[0] == AUTO_X ? 8 + i * (columnW + 8) : p[0];
			panel.pos(x, p[1]);
			panels.put(cat, panel);
			headers.put(cat, panel.children.get(0));
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
		if (event.button() == MOUSE_LEFT) {
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
		if (activeColor != null) {
			activeColor.applyAt(event.x(), event.y());
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
		if (activeColor != null) {
			activeColor = null;
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
			binding.setKey(key == InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN.getValue() : key);
			binding = null;
			return true;
		}
		if (editingString != null) {
			int key = event.key();
			if (key == InputConstants.KEY_ESCAPE || key == InputConstants.KEY_RETURN) {
				editingString = null;
				rebuild();
				return true;
			}
			if (key == InputConstants.KEY_BACKSPACE) {
				String value = editingString.getValue();
				if (!value.isEmpty()) {
					editingString.setValue(value.substring(0, value.length() - 1));
					rebuild();
				}
				return true;
			}
			// Swallow other keys while editing so they don't bind/navigate behind the text field.
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (editingString != null) {
			String value = editingString.getValue();
			if (value.length() < editingString.getMaxLength() && !Character.isISOControl(event.codepoint())) {
				editingString.setValue(value + event.codepointAsString());
				rebuild();
			}
			return true;
		}
		return super.charTyped(event);
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
		protected float intrinsicCross() {
			return 8 + bodyFont.measureTextWidth(module.getName()) + ROW_GAP
					+ bodyFont.measureTextWidth("+") + 12;
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
			if (button == MOUSE_RIGHT) {
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
		protected float intrinsicCross() {
			return rowWidth(setting.isChild() ? 24 : 14, setting.getName(), setting.isEnabled() ? "ON" : "OFF");
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
			drawText(canvas, bodyFont, paint, setting.getName(), x + (setting.isChild() ? 24 : 14), y, h, SUBTEXT);
			drawTextRight(canvas, bodyFont, paint, setting.isEnabled() ? "ON" : "OFF", x + w - 8, y, h,
					setting.isEnabled() ? ACCENT : SUBTEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			setting.toggle();
			rebuild();
			return true;
		}
	}

	// Header row of a mode setting: shows the current value; click opens/closes its dropdown.
	private final class ModeHeaderRow extends Component {
		private final ModeSetting setting;
		private final Anim hover = new Anim(0);

		ModeHeaderRow(ModeSetting setting) {
			this.setting = setting;
			height(ROW_H);
		}

		@Override
		protected float intrinsicCross() {
			return rowWidth(14, setting.getName(), setting.getValue());
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

	// One option inside an open mode dropdown; click selects it and closes the dropdown.
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
		protected float intrinsicCross() {
			return 24 + bodyFont.measureTextWidth(option) + 8;
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

	// A number setting: name + value + a draggable slider track. Click or drag to set the value.
	private final class NumberRow extends Component {
		private final NumberSetting setting;
		private final Anim hover = new Anim(0);

		NumberRow(NumberSetting setting) {
			this.setting = setting;
			height(ROW_H + 4);
		}

		// Whole-number steps (line counts, times of day) are shown without decimals.
		private String value() {
			return String.format(Locale.ROOT, setting.getStep() >= 1.0 ? "%.0f" : "%.2f", setting.getValue());
		}

		@Override
		protected float intrinsicCross() {
			return rowWidth(setting.isChild() ? 24 : 14, setting.getName(), value());
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
			// text sits in the upper part of the row; the slider track gets the bottom strip
			float textH = h - 6;
			drawText(canvas, bodyFont, paint, setting.getName(), x + (setting.isChild() ? 24 : 14), y, textH, SUBTEXT);
			drawTextRight(canvas, bodyFont, paint, value(), x + w - 8, y, textH, ACCENT);

			float tx = x + 8;
			float tw = w - 16;
			float ty = y + h - 4;
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

	// A color setting: name + a swatch on the right; click toggles the picker below it.
	private final class ColorRow extends Component {
		private final ColorSetting setting;
		private final Anim hover = new Anim(0);

		ColorRow(ColorSetting setting) {
			this.setting = setting;
			height(ROW_H);
		}

		@Override
		protected float intrinsicCross() {
			// label, a gap, then the 16px swatch sitting 8px from the right edge
			return rowWidth(setting.isChild() ? 24 : 14, setting.getName(), null) + ROW_GAP + 16;
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
			drawText(canvas, bodyFont, paint, setting.getName(), x + (setting.isChild() ? 24 : 14), y, h, SUBTEXT);
			float sw = 16;
			float sh = 9;
			float sxp = x + w - 8 - sw;
			float syp = y + (h - sh) / 2f;
			paint.setColor(0xFF101014);
			canvas.drawRRect(RRect.makeXYWH(sxp - 1, syp - 1, sw + 2, sh + 2, 2), paint);
			paint.setColor(0xFF000000 | (setting.getColor() & 0xFFFFFF));
			canvas.drawRRect(RRect.makeXYWH(sxp, syp, sw, sh, 2), paint);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			if (!openColors.remove(setting)) {
				openColors.add(setting);
			}
			rebuild();
			return true;
		}
	}

	/** The expanded color picker: a saturation/value square + a hue strip, both drag-to-pick. */
	private final class ColorPickerRow extends Component {
		private final ColorSetting setting;
		private boolean dragHue;

		ColorPickerRow(ColorSetting setting) {
			this.setting = setting;
			height(72);
		}

		@Override
		protected void onUpdate(float dt, double mx, double my) {
		}

		private float[] geom() {
			float sx = x + 8;
			float sy = y + 4;
			float sqw = w - 16;
			float sqh = 46;
			float hx = x + 8;
			float hy = sy + sqh + 6;
			float hw = w - 16;
			float hh = 9;
			return new float[] {sx, sy, sqw, sqh, hx, hy, hw, hh};
		}

		@Override
		protected void drawSelf(Canvas canvas, Paint paint) {
			float[] g = geom();
			float sx = g[0];
			float sy = g[1];
			float sqw = g[2];
			float sqh = g[3];
			float hx = g[4];
			float hy = g[5];
			float hw = g[6];
			float hh = g[7];
			float[] hsv = rgbToHsv(setting.getColor());
			int hueColor = 0xFF000000 | hsvToRgb(hsv[0], 1f, 1f);

			paint.setMode(PaintMode.FILL);
			try (Shader base = Shader.makeLinearGradient(sx, sy, sx + sqw, sy, new int[] {0xFFFFFFFF, hueColor})) {
				paint.setShader(base);
				canvas.drawRect(Rect.makeXYWH(sx, sy, sqw, sqh), paint);
			}
			try (Shader shade = Shader.makeLinearGradient(sx, sy, sx, sy + sqh, new int[] {0x00000000, 0xFF000000})) {
				paint.setShader(shade);
				canvas.drawRect(Rect.makeXYWH(sx, sy, sqw, sqh), paint);
			}
			try (Shader hue = Shader.makeLinearGradient(hx, hy, hx + hw, hy, new int[] {
					0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000})) {
				paint.setShader(hue);
				canvas.drawRRect(RRect.makeXYWH(hx, hy, hw, hh, 2), paint);
			}
			paint.setShader(null);

			paint.setMode(PaintMode.STROKE);
			paint.setStrokeWidth(1.4f);
			paint.setColor(0xFFFFFFFF);
			canvas.drawCircle(sx + hsv[1] * sqw, sy + (1f - hsv[2]) * sqh, 2.5f, paint);
			canvas.drawRect(Rect.makeXYWH(hx + hsv[0] * hw - 1, hy - 1, 2, hh + 2), paint);
			paint.setMode(PaintMode.FILL);
			paint.setStrokeWidth(1f);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			dragHue = my >= geom()[5] - 2;
			activeColor = this;
			applyAt(mx, my);
			return true;
		}

		void applyAt(double mx, double my) {
			float[] g = geom();
			float[] hsv = rgbToHsv(setting.getColor());
			if (dragHue) {
				hsv[0] = clamp01((float) ((mx - g[4]) / g[6]));
			} else {
				hsv[1] = clamp01((float) ((mx - g[0]) / g[2]));
				hsv[2] = 1f - clamp01((float) ((my - g[1]) / g[3]));
			}
			// keep the setting's alpha, so translucent colors (e.g. the chat backdrop) stay translucent
			int alpha = setting.getColor() & 0xFF000000;
			setting.setColor((alpha == 0 ? 0xFF000000 : alpha) | hsvToRgb(hsv[0], hsv[1], hsv[2]));
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
			String key = binding == module ? "..." : KeyUtils.name(module.getKey());
			drawTextRight(canvas, bodyFont, paint, key, x + w - 8, y, h, TEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			binding = module;
			return true;
		}
	}

	// A free-form text setting: name + current value; click to edit, typing edits, Esc/Enter ends.
	private final class StringRow extends Component {
		private final StringSetting setting;
		private final Anim hover = new Anim(0);

		@Override
		protected float intrinsicCross() {
			return rowWidth(14, "Bind", KeyUtils.name(module.getKey()));
		}

		StringRow(StringSetting setting) {
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
			boolean editing = editingString == setting;
			paint.setMode(PaintMode.FILL);
			if (editing || hover.value() > 0.01f) {
				paint.setColor(editing ? 0x33C20C0C : alpha(0x18FFFFFF, hover.value()));
				canvas.drawRRect(RRect.makeXYWH(x, y, w, h, 0), paint);
			}
			drawText(canvas, bodyFont, paint, setting.getName(), x + 14, y, h, SUBTEXT);
			String value = editing ? setting.getValue() + "_" : setting.getValue();
			drawTextRight(canvas, bodyFont, paint, value, x + w - 8, y, h, editing ? ACCENT : TEXT);
		}

		@Override
		protected boolean onClick(double mx, double my, int button) {
			editingString = editingString == setting ? null : setting;
			rebuild();
			return true;
		}
	}

	// ---------- helpers ----------

	private static void drawText(Canvas canvas, Font font, Paint paint, String s, float tx, float rowY, float rowH, int color) {
		@Override
		protected float intrinsicCross() {
			return rowWidth(14, setting.getName(), setting.getValue() + "_");
		}

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
	// Natural width of a "label ... value" row. value may be null for fixed-width widgets.
	private float rowWidth(float left, String label, String value) {
		float w = left + bodyFont.measureTextWidth(label);
		if (value != null && !value.isEmpty()) {
			w += ROW_GAP + bodyFont.measureTextWidth(value);
		}
		return w + 8;
	}

	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	// HSV (each 0..1) to packed RGB (0xRRGGBB).
	private static int hsvToRgb(float h, float s, float v) {
		h = (h % 1f + 1f) % 1f;
		int i = (int) (h * 6f) % 6;
		float f = h * 6f - (int) (h * 6f);
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float t = v * (1f - (1f - f) * s);
		float r;
		float g;
		float b;
		switch (i) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
	}

	// Packed ARGB to HSV components {h, s, v}, each 0..1.
	private static float[] rgbToHsv(int argb) {
		float r = ((argb >> 16) & 0xFF) / 255f;
		float g = ((argb >> 8) & 0xFF) / 255f;
		float b = (argb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float d = max - min;
		float h = 0f;
		if (d != 0f) {
			if (max == r) {
				h = ((g - b) / d) % 6f;
			} else if (max == g) {
				h = (b - r) / d + 2f;
			} else {
				h = (r - g) / d + 4f;
			}
			h /= 6f;
			if (h < 0f) {
				h += 1f;
			}
		}
		return new float[] {h, max == 0f ? 0f : d / max, max};
	}

	private static String catName(Category cat) {
		String n = cat.name();
		return n.charAt(0) + n.substring(1).toLowerCase();
	}
}
