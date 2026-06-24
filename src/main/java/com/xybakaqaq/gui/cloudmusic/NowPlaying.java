package com.xybakaqaq.gui.cloudmusic;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.platform.NativeImage;
import com.xybakaqaq.modules.ModuleManager;
import com.xybakaqaq.modules.render.CloudMusic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Shared, process-wide "now playing" state and play queue. Both the full {@link CloudMusicScreen}
 * and the on-screen {@link CloudMusicWidget} read/drive playback through here, so the cover, title,
 * artist, progress and next/previous all stay consistent whether or not the screen is open.
 */
public final class NowPlaying {

	/** Texture id of the current cover (loaded at {@link #COVER_PX}px square). */
	public static final Identifier COVER = Identifier.fromNamespaceAndPath("zeroday", "cloudmusic_cover");
	public static final int COVER_PX = 64;

	// The queue we play from (parallel lists), and the index currently playing.
	private static final List<Long> ids = new ArrayList<>();
	private static final List<String> titles = new ArrayList<>();
	private static final List<String> artists = new ArrayList<>();
	private static final List<String> covers = new ArrayList<>();
	private static final List<Long> durations = new ArrayList<>();
	private static int index = -1;

	public static volatile String title = "未播放";
	public static volatile String artist = "";
	public static volatile boolean hasCover;
	public static volatile long durationMs;

	/** Play mode: 0 = sequential, 1 = loop-one, 2 = shuffle. */
	private static volatile int mode;

	static {
		MusicPlayer.setOnEnd(NowPlaying::advance);
	}

	private record Line(long time, String text) {
	}

	private static final Pattern LRC_TAG = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?\\]");
	private static volatile List<Line> lyrics = List.of();

	private NowPlaying() {
	}

	/** @return the lyric line for the current playback position, or "" if none / disabled. */
	public static String currentLyric() {
		CloudMusic module = ModuleManager.get(CloudMusic.class);
		if (module == null || !module.lyricsEnabled()) {
			return "";
		}
		List<Line> ls = lyrics;
		if (ls.isEmpty()) {
			return "";
		}
		long pos = MusicPlayer.positionMs();
		String current = "";
		for (Line line : ls) {
			if (line.time() <= pos) {
				current = line.text();
			} else {
				break;
			}
		}
		return current;
	}

	/** @return the id of the track currently playing, or -1. */
	public static synchronized long playingId() {
		return index >= 0 && index < ids.size() ? ids.get(index) : -1;
	}

	/** Replaces the play queue (e.g. from a playlist or a search result). */
	public static synchronized void setQueue(List<Long> qIds, List<String> qTitles, List<String> qArtists,
			List<String> qCovers, List<Long> qDurations) {
		ids.clear();
		ids.addAll(qIds);
		titles.clear();
		titles.addAll(qTitles);
		artists.clear();
		artists.addAll(qArtists);
		covers.clear();
		covers.addAll(qCovers);
		durations.clear();
		durations.addAll(qDurations);
	}

	/** Plays the queue entry at {@code i} (fetches the stream URL + cover off-thread). */
	public static synchronized void playIndex(int i) {
		if (i < 0 || i >= ids.size()) {
			return;
		}
		index = i;
		long id = ids.get(i);
		title = titles.get(i);
		artist = artists.get(i);
		durationMs = durations.get(i);
		lyrics = List.of();
		loadCover(id, covers.get(i));
		loadLyrics(id);
		String label = title;
		runAsync(() -> {
			try {
				String url = NeteaseApi.songUrl(id);
				if (url == null) {
					title = label + "（无法播放/VIP）";
					return;
				}
				MusicPlayer.play(url);
			} catch (Exception ex) {
				title = "播放失败";
			}
		});
	}

	public static int mode() {
		return mode;
	}

	/** Cycles play mode: 顺序 → 单曲循环 → 随机. */
	public static void cycleMode() {
		mode = (mode + 1) % 3;
	}

	public static String modeName() {
		return mode == 1 ? "单曲循环" : mode == 2 ? "随机播放" : "顺序播放";
	}

	public static synchronized List<String> queueTitles() {
		return new ArrayList<>(titles);
	}

	public static synchronized List<String> queueArtists() {
		return new ArrayList<>(artists);
	}

	public static synchronized int queueIndex() {
		return index;
	}

	public static synchronized int queueSize() {
		return ids.size();
	}

	/** Advances when a track ends, honouring the play {@link #mode}. */
	private static synchronized void advance() {
		if (ids.isEmpty()) {
			return;
		}
		switch (mode) {
			case 1 -> playIndex(index);
			case 2 -> playIndex((int) (Math.random() * ids.size()));
			default -> {
				if (index + 1 < ids.size()) {
					playIndex(index + 1);
				}
			}
		}
	}

	public static synchronized void next() {
		if (index + 1 < ids.size()) {
			playIndex(index + 1);
		}
	}

	public static synchronized void previous() {
		if (index - 1 >= 0) {
			playIndex(index - 1);
		}
	}

	/** Stops playback and clears the now-playing state. */
	public static synchronized void stop() {
		MusicPlayer.stop();
		title = "未播放";
		artist = "";
		hasCover = false;
		durationMs = 0;
		index = -1;
		lyrics = List.of();
	}

	private static void loadLyrics(long id) {
		// Always fetch (one tiny request) so the Lyrics toggle is an instant show/hide — the display
		// is gated in currentLyric(), not the fetch.
		runAsync(() -> {
			try {
				List<Line> parsed = parseLrc(NeteaseApi.lyric(id));
				if (playingId() == id) {
					lyrics = parsed;
				}
			} catch (Exception ignored) {
				// no lyrics
			}
		});
	}

	private static List<Line> parseLrc(String lrc) {
		List<Line> lines = new ArrayList<>();
		if (lrc == null) {
			return lines;
		}
		for (String raw : lrc.split("\n")) {
			String text = raw.replaceAll("\\[[^\\]]*\\]", "").trim();
			if (text.isEmpty()) {
				continue;
			}
			Matcher matcher = LRC_TAG.matcher(raw);
			while (matcher.find()) {
				long min = Long.parseLong(matcher.group(1));
				long sec = Long.parseLong(matcher.group(2));
				long frac = 0;
				if (matcher.group(3) != null) {
					String f = matcher.group(3);
					frac = Long.parseLong(f);
					if (f.length() == 1) {
						frac *= 100;
					} else if (f.length() == 2) {
						frac *= 10;
					}
				}
				lines.add(new Line(min * 60000 + sec * 1000 + frac, text));
			}
		}
		lines.sort((a, b) -> Long.compare(a.time(), b.time()));
		return lines;
	}

	private static void loadCover(long id, String picUrl) {
		hasCover = false;
		if (picUrl == null || picUrl.isEmpty()) {
			return;
		}
		runAsync(() -> {
			try {
				byte[] bytes = CoverCache.fetchBytes(id, picUrl, COVER_PX);
				NativeImage image = Images.decode(bytes);
				Minecraft.getInstance().execute(() -> {
					Minecraft.getInstance().getTextureManager().register(COVER, new DynamicTexture(() -> "cloudmusic-cover", image));
					hasCover = true;
				});
			} catch (Exception ex) {
				System.err.println("[CloudMusic] cover load failed: " + ex);
			}
		});
	}

	private static void runAsync(Runnable task) {
		Thread thread = new Thread(task, "cloudmusic-np");
		thread.setDaemon(true);
		thread.start();
	}
}
