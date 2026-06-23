package com.xybakaqaq.modules.render;

import org.lwjgl.glfw.GLFW;

import com.xybakaqaq.gui.cloudmusic.NowPlaying;
import com.xybakaqaq.modules.Category;
import com.xybakaqaq.modules.Module;
import com.xybakaqaq.modules.settings.BooleanSetting;

/**
 * NetEase Cloud Music. While enabled, a small now-playing widget (cover / title / artist / lyric) is
 * drawn top-left. Open the chat bar to free the mouse: the widget then shows playback controls, and
 * right-clicking it opens the full music screen (login, playlists, search, playback).
 */
public final class CloudMusic extends Module {

	private final BooleanSetting lyrics = new BooleanSetting("Lyrics", true);

	public CloudMusic() {
		super("CloudMusic", Category.RENDER, "NetEase Cloud Music: now-playing widget + full UI (chat to control, right-click to open).", GLFW.GLFW_KEY_UNKNOWN);
	}

	/** @return whether synced lyrics should be fetched and shown. */
	public boolean lyricsEnabled() {
		return lyrics.isEnabled();
	}

	@Override
	protected void onDisable() {
		NowPlaying.stop();
	}
}
