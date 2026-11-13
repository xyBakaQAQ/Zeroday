package com.xybakaqaq.utils;

import java.util.Locale;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.sdl.SDLKeyboard;

// 26.3 reports SDL scancodes; these convert legacy GLFW codes and name scancodes for display.
public final class KeyUtils {

	// Written to config.json once keybinds are known to be SDL scancodes.
	public static final int KEY_SCHEME_SDL = 2;

	private KeyUtils() {
	}

	// Migrates a pre-scancode config: GLFW keycode to SDL scancode, or UNKNOWN.
	public static int glfwToScancode(int glfwKey) {
		// GLFW_KEY_UNKNOWN is -1; an unbound module was saved with that.
		if (glfwKey <= 0) {
			return InputConstants.UNKNOWN.getValue();
		}
		// Letters: GLFW 65..90 -> SDL 4..29.
		if (glfwKey >= 65 && glfwKey <= 90) {
			return glfwKey - 61;
		}
		// Digits: GLFW 48..57 -> SDL 39 (for '0') then 30..38 (for '1'..'9').
		if (glfwKey >= 48 && glfwKey <= 57) {
			return glfwKey == 48 ? 39 : glfwKey - 19;
		}
		// Function keys: GLFW 290..301 -> SDL 58..69; GLFW 302..313 -> SDL 104..115.
		if (glfwKey >= 290 && glfwKey <= 301) {
			return glfwKey - 232;
		}
		if (glfwKey >= 302 && glfwKey <= 313) {
			return glfwKey - 198;
		}
		// Keypad digits: GLFW 320..329 -> SDL 98 (KP_0) then 89..97 (KP_1..KP_9).
		if (glfwKey >= 320 && glfwKey <= 329) {
			return glfwKey == 320 ? 98 : glfwKey - 232;
		}
		return switch (glfwKey) {
			case 32 -> 44;    // space
			case 39 -> 52;    // apostrophe
			case 44 -> 54;    // comma
			case 45 -> 45;    // minus
			case 46 -> 55;    // period
			case 47 -> 56;    // slash
			case 59 -> 51;    // semicolon
			case 61 -> 46;    // equals
			case 91 -> 47;    // left bracket
			case 92 -> 49;    // backslash
			case 93 -> 48;    // right bracket
			case 96 -> 53;    // grave
			case 161 -> 50;   // world 1 (non-US backslash)
			case 256 -> 41;   // escape
			case 257 -> 40;   // enter
			case 258 -> 43;   // tab
			case 259 -> 42;   // backspace
			case 260 -> 73;   // insert
			case 261 -> 76;   // delete
			case 262 -> 79;   // right
			case 263 -> 80;   // left
			case 264 -> 81;   // down
			case 265 -> 82;   // up
			case 266 -> 75;   // page up
			case 267 -> 78;   // page down
			case 268 -> 74;   // home
			case 269 -> 77;   // end
			case 280 -> 57;   // caps lock
			case 281 -> 71;   // scroll lock
			case 282 -> 83;   // num lock
			case 283 -> 70;   // print screen
			case 284 -> 72;   // pause
			case 330 -> 99;   // keypad decimal
			case 331 -> 84;   // keypad divide
			case 332 -> 85;   // keypad multiply
			case 333 -> 86;   // keypad subtract
			case 334 -> 87;   // keypad add
			case 335 -> 88;   // keypad enter
			case 336 -> 103;  // keypad equal
			case 340 -> 225;  // left shift
			case 341 -> 224;  // left control
			case 342 -> 226;  // left alt
			case 343 -> 227;  // left super
			case 344 -> 229;  // right shift
			case 345 -> 228;  // right control
			case 346 -> 230;  // right alt
			case 347 -> 231;  // right super
			case 348 -> 118;  // menu
			default -> InputConstants.UNKNOWN.getValue();
		};
	}


	public static String name(int scancode) {
		if (scancode == InputConstants.UNKNOWN.getValue()) {
			return "None";
		}
		return switch (scancode) {
			case InputConstants.KEY_RSHIFT -> "RSHIFT";
			case InputConstants.KEY_LSHIFT -> "LSHIFT";
			case InputConstants.KEY_RCONTROL -> "RCTRL";
			case InputConstants.KEY_LCONTROL -> "LCTRL";
			case InputConstants.KEY_RALT -> "RALT";
			case InputConstants.KEY_LALT -> "LALT";
			case InputConstants.KEY_SPACE -> "SPACE";
			case InputConstants.KEY_TAB -> "TAB";
			default -> {
				int keycode = SDLKeyboard.SDL_GetKeyFromScancode(scancode, (short) 0, false);
				String name = SDLKeyboard.SDL_GetKeyName(keycode);
				yield name != null && !name.isEmpty() ? name.toUpperCase(Locale.ROOT) : "KEY" + scancode;
			}
		};
	}
}
