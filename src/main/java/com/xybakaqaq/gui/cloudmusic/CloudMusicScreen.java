package com.xybakaqaq.gui.cloudmusic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mojang.blaze3d.platform.NativeImage;

import com.xybakaqaq.utils.Sdf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * In-game NetEase Cloud Music UI: a centered, dark, rounded panel (Hanabi-style). Login
 * (phone / local-QR) then a player with the user's playlists, search, the now-playing cover,
 * a progress bar, the currently playing track highlighted, and playback ({@link MusicPlayer}).
 */
public class CloudMusicScreen extends Screen {

	private enum View {
		LOGIN,
		HOME
	}

	private static final Identifier QR_TEXTURE = Identifier.fromNamespaceAndPath("zeroday", "cloudmusic_qr");
	private static final int QR_SIZE = 170;
	private static final int COVER = 40;
	private static final int ROW_H = 16;
	// colors (0xAARRGGBB)
	private static final int PANEL = 0xF01A1A1F;
	private static final int LIST_BG = 0x40121216;
	private static final int ACCENT = 0xFFC20C0C;
	private static final int TEXT = 0xFFE8E8EA;
	private static final int SUBTEXT = 0xFF9A9AA2;
	private static final int LYRIC = 0xFFD8A8A8;
	private static final int ROW_SEL = 0xFFC20C0C;
	private static final int ROW_HOVER = 0x33FFFFFF;
	private static final int TRACK_BAR = 0x33FFFFFF;

	private View view = View.LOGIN;
	private boolean qrTab = true;

	private EditBox phoneBox;
	private EditBox passwordBox;
	private EditBox searchBox;

	private volatile String status = "";
	private volatile String qrKey;
	private volatile boolean qrReady;
	private boolean loggedIn;
	private boolean polling;
	private int pollCounter;

	private final List<String> playlistNames = new ArrayList<>();
	private final List<Long> playlistIds = new ArrayList<>();
	private final List<String> playlistCovers = new ArrayList<>();
	private final List<String> trackLabels = new ArrayList<>();
	private final List<Long> trackIds = new ArrayList<>();
	private final List<String> trackTitles = new ArrayList<>();
	private final List<String> trackArtists = new ArrayList<>();
	private final List<String> trackCovers = new ArrayList<>();
	private final List<Long> trackDurations = new ArrayList<>();
	private int playlistScroll;
	private int trackScroll;
	private int trackSelected = -1;

	// centered panel + list rects (computed in init)
	private int px;
	private int py;
	private int panelW;
	private int panelH;
	private int listY;
	private int listH;
	private int plX;
	private int plW;
	private int tkX;
	private int tkW;
	private int controlsX;

	public CloudMusicScreen() {
		super(Minecraft.getInstance(), Minecraft.getInstance().font, Component.literal("网易云音乐"));
		status = "正在尝试自动登录...";
		runAsync(() -> {
			try {
				if (NeteaseApi.loadCookie() && NeteaseApi.userId() > 0) {
					minecraft.execute(this::onLoginSuccess);
				} else {
					status = "";
				}
			} catch (Exception ignored) {
				status = "";
			}
		});
	}

	@Override
	protected void init() {
		panelW = view == View.LOGIN ? 320 : Math.min(width - 40, 660);
		panelH = view == View.LOGIN ? 360 : Math.min(height - 40, 430);
		px = (width - panelW) / 2;
		py = (height - panelH) / 2;
		if (view == View.LOGIN) {
			initLogin();
		} else {
			initHome();
		}
	}

	/** Draws the panel chrome with the GPU SDF shader: subtle border ring, gradient panel, cards. */
	private void renderChrome(GuiGraphicsExtractor graphics) {
		Sdf.roundRect(graphics, px - 1, py - 1, panelW + 2, panelH + 2, 13, 0x22FFFFFF);
		Sdf.roundRect(graphics, px, py, panelW, panelH, 12, 0xF0242430, 0xF013131A);
		if (view == View.HOME) {
			Sdf.roundRect(graphics, plX, listY, plW, listH, 6, 0x66121216);
			Sdf.roundRect(graphics, tkX, listY, tkW, listH, 6, 0x66121216);
			Sdf.roundRect(graphics, px + 8, py + panelH - 52, panelW - 16, 44, 8, 0x44000000);
		} else if (qrTab) {
			int qx = px + panelW / 2 - QR_SIZE / 2;
			int qy = py + 76;
			Sdf.roundRect(graphics, qx - 7, qy - 7, QR_SIZE + 14, QR_SIZE + 14, 8, 0xFFFFFFFF);
		}
	}

	private void rebuild() {
		clearWidgets();
		init();
	}

	// ---------- login ----------

	private void initLogin() {
		int cx = px + panelW / 2;
		addRenderableWidget(Button.builder(Component.literal("手机号登录"), b -> {
			qrTab = false;
			rebuild();
		}).bounds(cx - 102, py + 40, 98, 20).build());
		addRenderableWidget(Button.builder(Component.literal("扫码登录"), b -> {
			qrTab = true;
			rebuild();
		}).bounds(cx + 4, py + 40, 98, 20).build());

		if (qrTab) {
			if (qrKey == null) {
				startQr();
			}
		} else {
			phoneBox = new EditBox(font, cx - 100, py + 110, 200, 20, Component.literal("手机号"));
			phoneBox.setHint(Component.literal("手机号"));
			phoneBox.setMaxLength(20);
			addRenderableWidget(phoneBox);
			passwordBox = new EditBox(font, cx - 100, py + 138, 200, 20, Component.literal("密码"));
			passwordBox.setHint(Component.literal("密码"));
			passwordBox.setMaxLength(64);
			addRenderableWidget(passwordBox);
			addRenderableWidget(Button.builder(Component.literal("登 录"), b -> doPhoneLogin())
					.bounds(cx - 100, py + 168, 200, 20).build());
		}
	}

	private void startQr() {
		qrReady = false;
		status = "正在生成二维码...";
		runAsync(() -> {
			try {
				String key = NeteaseApi.qrKey();
				qrKey = key;
				NativeImage image = qrImage(NeteaseApi.qrUrl(key), QR_SIZE);
				minecraft.execute(() -> uploadQr(image));
				status = "请使用网易云音乐 App 扫码登录";
			} catch (Exception ex) {
				status = "二维码获取失败: " + ex.getMessage();
			}
		});
	}

	private void uploadQr(NativeImage image) {
		minecraft.getTextureManager().register(QR_TEXTURE, new DynamicTexture(() -> "cloudmusic-qr", image));
		qrReady = true;
	}

	private void doPhoneLogin() {
		String phone = phoneBox.getValue().trim();
		String password = passwordBox.getValue();
		if (phone.isEmpty() || password.isEmpty()) {
			status = "请输入手机号和密码";
			return;
		}
		status = "登录中...";
		runAsync(() -> {
			try {
				if (NeteaseApi.loginCellphone(phone, password) == 200) {
					minecraft.execute(this::onLoginSuccess);
				} else {
					status = "登录失败";
				}
			} catch (Exception ex) {
				status = "登录失败: " + ex.getMessage();
			}
		});
	}

	@Override
	public void tick() {
		super.tick();
		if (view != View.LOGIN || !qrTab || qrKey == null || loggedIn || polling) {
			return;
		}
		if (pollCounter++ % 40 != 0) {
			return;
		}
		polling = true;
		String key = qrKey;
		runAsync(() -> {
			try {
				int code = NeteaseApi.qrCheck(key);
				switch (code) {
					case 801 -> status = "等待扫码...";
					case 802 -> status = "已扫码，请在手机上确认";
					case 803 -> minecraft.execute(this::onLoginSuccess);
					case 800 -> {
						qrKey = null;
						minecraft.execute(this::startQr);
					}
					default -> status = "状态码: " + code;
				}
			} catch (Exception ex) {
				status = "轮询失败: " + ex.getMessage();
			} finally {
				polling = false;
			}
		});
	}

	// ---------- home ----------

	private void onLoginSuccess() {
		if (loggedIn) {
			return;
		}
		loggedIn = true;
		NeteaseApi.saveCookie();
		view = View.HOME;
		status = "";
		rebuild();
		loadPlaylists();
	}

	private void initHome() {
		int topY = py + 8;
		int searchBtnW = 50;
		int searchW = 150;
		int searchBtnX = px + panelW - 12 - searchBtnW;
		int searchX = searchBtnX - 6 - searchW;
		searchBox = new EditBox(font, searchX, topY, searchW, 20, Component.literal("搜索"));
		searchBox.setHint(Component.literal("搜索歌曲"));
		searchBox.setMaxLength(60);
		addRenderableWidget(searchBox);
		addRenderableWidget(Button.builder(Component.literal("搜索"), b -> doSearch())
				.bounds(searchBtnX, topY, searchBtnW, 20).build());

		listY = py + 38;
		listH = py + panelH - 52 - listY;
		plX = px + 12;
		plW = 150;
		tkX = plX + plW + 10;
		tkW = px + panelW - 12 - tkX;

		int bw = 46;
		int gap = 4;
		controlsX = px + panelW - 12 - (bw * 4 + gap * 3);
		int by = py + panelH - 30;
		addRenderableWidget(Button.builder(Component.literal("播放"), b -> playSelected()).bounds(controlsX, by, bw, 20).build());
		addRenderableWidget(Button.builder(Component.literal("暂停"), b -> MusicPlayer.pause()).bounds(controlsX + bw + gap, by, bw, 20).build());
		addRenderableWidget(Button.builder(Component.literal("继续"), b -> MusicPlayer.resume()).bounds(controlsX + (bw + gap) * 2, by, bw, 20).build());
		addRenderableWidget(Button.builder(Component.literal("停止"), b -> NowPlaying.stop())
				.bounds(controlsX + (bw + gap) * 3, by, bw, 20).build());
	}

	private void loadPlaylists() {
		runAsync(() -> {
			try {
				long uid = NeteaseApi.userId();
				JsonArray playlists = NeteaseApi.userPlaylists(uid);
				minecraft.execute(() -> {
					playlistNames.clear();
					playlistIds.clear();
					playlistCovers.clear();
					for (int i = 0; i < playlists.size(); i++) {
						JsonObject pl = playlists.get(i).getAsJsonObject();
						playlistIds.add(pl.get("id").getAsLong());
						playlistNames.add(pl.get("name").getAsString());
						playlistCovers.add(pl.has("coverImgUrl") && !pl.get("coverImgUrl").isJsonNull()
								? pl.get("coverImgUrl").getAsString() : null);
					}
				});
			} catch (Exception ignored) {
				// ignore
			}
		});
	}

	private void selectPlaylist(int index) {
		long id = playlistIds.get(index);
		runAsync(() -> {
			try {
				JsonArray tracks = NeteaseApi.playlistTracks(id);
				minecraft.execute(() -> setTracks(tracks));
			} catch (Exception ignored) {
				// ignore
			}
		});
	}

	private void doSearch() {
		String keyword = searchBox.getValue().trim();
		if (keyword.isEmpty()) {
			return;
		}
		runAsync(() -> {
			try {
				JsonArray songs = NeteaseApi.search(keyword);
				minecraft.execute(() -> setTracks(songs));
			} catch (Exception ignored) {
				// ignore
			}
		});
	}

	private void setTracks(JsonArray songs) {
		trackLabels.clear();
		trackIds.clear();
		trackTitles.clear();
		trackArtists.clear();
		trackCovers.clear();
		trackDurations.clear();
		trackSelected = -1;
		trackScroll = 0;
		for (int i = 0; i < songs.size(); i++) {
			JsonObject song = songs.get(i).getAsJsonObject();
			trackIds.add(song.get("id").getAsLong());
			trackLabels.add(songLabel(song));
			trackTitles.add(songName(song));
			trackArtists.add(songArtist(song));
			trackCovers.add(coverUrl(song));
			trackDurations.add(song.has("dt") ? song.get("dt").getAsLong()
					: song.has("duration") ? song.get("duration").getAsLong() : 0L);
		}
	}

	private void playSelected() {
		if (trackSelected < 0 || trackSelected >= trackIds.size()) {
			return;
		}
		NowPlaying.setQueue(trackIds, trackTitles, trackArtists, trackCovers, trackDurations);
		NowPlaying.playIndex(trackSelected);
	}

	// ---------- render ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// GPU SDF panel chrome behind everything.
		renderChrome(graphics);
		graphics.text(font, "网易云音乐", px + 12, py + 11, ACCENT);

		if (view == View.LOGIN) {
			renderLogin(graphics);
		} else {
			renderHome(graphics, mouseX, mouseY);
		}
		// MC widgets (search box, buttons) on top of the panel + content.
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private void renderLogin(GuiGraphicsExtractor graphics) {
		int cx = px + panelW / 2;
		if (qrTab && qrReady) {
			int qx = cx - QR_SIZE / 2;
			int qy = py + 76;
			graphics.blit(RenderPipelines.GUI_TEXTURED, QR_TEXTURE, qx, qy, 0.0F, 0.0F, QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);
		}
		if (!status.isEmpty()) {
			graphics.centeredText(font, Component.literal(status), cx, py + panelH - 26, SUBTEXT);
		}
	}

	private void renderHome(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		graphics.text(font, "我的歌单", plX, listY - 11, SUBTEXT);
		renderList(graphics, plX, listY, plW, listH, playlistNames, playlistIds, playlistCovers, playlistScroll, -1, -1, mouseX, mouseY);
		long pid = NowPlaying.playingId();
		int playing = pid < 0 ? -1 : trackIds.indexOf(pid);
		renderList(graphics, tkX, listY, tkW, listH, trackLabels, trackIds, trackCovers, trackScroll, trackSelected, playing, mouseX, mouseY);
		renderPlayerBar(graphics);
	}

	private void renderPlayerBar(GuiGraphicsExtractor graphics) {
		int cyp = py + panelH - COVER - 6;
		if (NowPlaying.hasCover) {
			Sdf.roundRect(graphics, px + 12, cyp, COVER, COVER, 5, 0x40000000);
			graphics.blit(RenderPipelines.GUI_TEXTURED, NowPlaying.COVER, px + 12, cyp, 0.0F, 0.0F,
					COVER, COVER, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX);
		}
		int textX = px + 12 + COVER + 8;
		int textW = controlsX - 12 - textX;
		graphics.text(font, trim(NowPlaying.title, textW), textX, cyp + 1, TEXT);
		graphics.text(font, trim(NowPlaying.artist, textW), textX, cyp + 11, SUBTEXT);
		String lyric = NowPlaying.currentLyric();
		if (!lyric.isEmpty()) {
			graphics.text(font, trim(lyric, textW), textX, cyp + 21, LYRIC);
		}

		int barX = textX;
		int barY = cyp + COVER - 6;
		int barW = textW;
		if (barW > 20) {
			float frac = NowPlaying.durationMs > 0 ? Math.min(1f, MusicPlayer.positionMs() / (float) NowPlaying.durationMs) : 0f;
			Sdf.roundRect(graphics, barX, barY, barW, 4, 2, TRACK_BAR);
			if (frac > 0) {
				Sdf.roundRect(graphics, barX, barY, Math.max(4, (int) (barW * frac)), 4, 2, ACCENT);
			}
		}
	}

	private void renderList(GuiGraphicsExtractor graphics, int x, int y, int w, int h,
			List<String> items, List<Long> rowIds, List<String> rowCovers,
			int scroll, int selected, int playing, int mouseX, int mouseY) {
		graphics.enableScissor(x, y, x + w, y + h);
		boolean thumbs = rowCovers != null;
		int thumb = ROW_H - 4;
		int visible = h / ROW_H;
		for (int i = scroll; i < items.size() && i < scroll + visible + 1; i++) {
			int ry = y + (i - scroll) * ROW_H;
			boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= ry && mouseY < ry + ROW_H && mouseY >= y && mouseY < y + h;
			if (i == selected) {
				graphics.fill(x, ry, x + w, ry + ROW_H, ROW_SEL);
			} else if (hover) {
				graphics.fill(x, ry, x + w, ry + ROW_H, ROW_HOVER);
			}

			int textX = x + 5;
			if (thumbs) {
				int cxp = x + 3;
				int cyp = ry + 2;
				Identifier tex = CoverCache.get(rowIds.get(i), rowCovers.get(i));
				if (tex != null) {
					graphics.blit(RenderPipelines.GUI_TEXTURED, tex, cxp, cyp, 0.0F, 0.0F,
							thumb, thumb, CoverCache.PX, CoverCache.PX, CoverCache.PX, CoverCache.PX);
				} else {
					Sdf.roundRect(graphics, cxp, cyp, thumb, thumb, 2, 0x33FFFFFF);
				}
				textX = cxp + thumb + 5;
			}

			int color = TEXT;
			if (i == selected) {
				color = 0xFFFFFFFF;
			} else if (i == playing) {
				color = ACCENT;
				graphics.fill(x, ry, x + 2, ry + ROW_H, ACCENT);
			}
			graphics.text(font, trim(items.get(i), w - (textX - x) - 5), textX, ry + 4, color);
		}
		graphics.disableScissor();
	}

	private String trim(String s, int maxWidth) {
		if (font.width(s) <= maxWidth) {
			return s;
		}
		while (s.length() > 1 && font.width(s + "…") > maxWidth) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "…";
	}

	// ---------- input ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (super.mouseClicked(event, doubleClick)) {
			return true;
		}
		if (view == View.HOME) {
			int mx = (int) event.x();
			int my = (int) event.y();
			int pl = listIndex(plX, plW, mx, my, playlistScroll, playlistNames.size());
			if (pl >= 0) {
				selectPlaylist(pl);
				return true;
			}
			int tk = listIndex(tkX, tkW, mx, my, trackScroll, trackLabels.size());
			if (tk >= 0) {
				trackSelected = tk;
				if (doubleClick) {
					playSelected();
				}
				return true;
			}
		}
		return false;
	}

	private int listIndex(int x, int w, int mx, int my, int scroll, int count) {
		if (mx < x || mx > x + w || my < listY || my > listY + listH) {
			return -1;
		}
		int row = scroll + (my - listY) / ROW_H;
		return row >= 0 && row < count ? row : -1;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (view == View.HOME) {
			int delta = scrollY > 0 ? -1 : 1;
			if (x >= plX && x <= plX + plW) {
				playlistScroll = clampScroll(playlistScroll + delta, playlistNames.size());
				return true;
			}
			if (x >= tkX && x <= tkX + tkW) {
				trackScroll = clampScroll(trackScroll + delta, trackLabels.size());
				return true;
			}
		}
		return super.mouseScrolled(x, y, scrollX, scrollY);
	}

	private int clampScroll(int scroll, int count) {
		int max = Math.max(0, count - listH / ROW_H);
		return Math.max(0, Math.min(scroll, max));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---------- helpers ----------

	private static String coverUrl(JsonObject song) {
		JsonObject album = song.has("al") ? song.getAsJsonObject("al")
				: song.has("album") ? song.getAsJsonObject("album") : null;
		if (album != null && album.has("picUrl") && !album.get("picUrl").isJsonNull()) {
			return album.get("picUrl").getAsString();
		}
		return null;
	}

	private static String songLabel(JsonObject song) {
		String artist = songArtist(song);
		return artist.isEmpty() ? songName(song) : songName(song) + " - " + artist;
	}

	private static String songName(JsonObject song) {
		return song.has("name") ? song.get("name").getAsString() : "?";
	}

	private static String songArtist(JsonObject song) {
		JsonArray artists = song.has("ar") ? song.getAsJsonArray("ar")
				: song.has("artists") ? song.getAsJsonArray("artists") : null;
		if (artists == null || artists.size() == 0) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < artists.size(); i++) {
			if (i > 0) {
				sb.append("/");
			}
			sb.append(artists.get(i).getAsJsonObject().get("name").getAsString());
		}
		return sb.toString();
	}

	private static NativeImage qrImage(String content, int size) throws Exception {
		Map<EncodeHintType, Object> hints = new HashMap<>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		hints.put(EncodeHintType.MARGIN, 1);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
		BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
		NativeImage image = new NativeImage(size, size, false);
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				image.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
			}
		}
		return image;
	}

	private static void runAsync(Runnable task) {
		Thread thread = new Thread(task, "cloudmusic-net");
		thread.setDaemon(true);
		thread.start();
	}
}
