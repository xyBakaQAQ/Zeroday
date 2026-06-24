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
import com.mojang.blaze3d.platform.Window;

import com.xybakaqaq.utils.Skia;
import com.xybakaqaq.utils.SkijaBridge;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Path;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.RRect;
import io.github.humbleui.types.Rect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;

/**
 * In-game NetEase Cloud Music UI, fully Skija-rendered (no MC widgets): a light desktop-style panel
 * with a top bar + search, a playlist sidebar, a track list, and a bottom player bar with drawn
 * transport icons. Text fields and buttons are custom; input is handled here. Covers (the now-playing
 * art and row thumbnails) are MC-blitted on top of the Skija panel.
 */
public class CloudMusicScreen extends Screen {

	private static final Identifier QR_TEXTURE = Identifier.fromNamespaceAndPath("zeroday", "cloudmusic_qr");
	private static final int QR_SIZE = 150;
	private static final int COVER = 38;
	private static final int ROW_H = 16;
	private static final int TOP_H = 32;
	private static final int SIDE_W = 128;
	private static final int PLAYER_H = 50;

	private static final int PANEL_BG = 0xFFFFFFFF;
	private static final int SIDEBAR_BG = 0xFFF6F6F8;
	private static final int DIVIDER = 0xFFE6E6E8;
	private static final int FIELD_BG = 0xFFF1F1F3;
	private static final int ACCENT = 0xFFEC4141;
	private static final int TEXT = 0xFF2A2A2A;
	private static final int SUBTEXT = 0xFF8C8C8C;
	private static final int LYRIC = 0xFFC85A5A;
	private static final int ROW_SEL = 0x1AEC4141;
	private static final int ROW_HOVER = 0x12000000;
	private static final int TRACK_BAR = 0xFFE2E2E4;

	private boolean loginOpen;
	private boolean userMenuOpen;
	private boolean qrTab = true;
	private boolean showQueue;

	private final Field searchField = new Field("搜索歌曲", false, 60);
	private final Field phoneField = new Field("手机号", false, 20);
	private final Field passwordField = new Field("密码", true, 64);
	private Field focused;

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
	private int selectedPlaylist = -1;

	// panel rect (screen coords) + layout (panel-local coords)
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
	private int playerTop;
	private final int[] avatarBtn = new int[4];
	private final int[] prevBtn = new int[4];
	private final int[] playBtn = new int[4];
	private final int[] nextBtn = new int[4];
	private final int[] modeBtn = new int[4];
	private final int[] queueBtn = new int[4];
	private final int[] tabPhone = new int[4];
	private final int[] tabQr = new int[4];
	private final int[] loginBtn = new int[4];
	private final int[] modalCard = new int[4];
	private final int[] closeBtn = new int[4];
	private final int[] userMenuRect = new int[4];

	private final SkijaBridge ui = new SkijaBridge("cloudmusic_ui");
	private final Font bodyFont = new Font(Skia.harmony(), 10.5f);
	private final Font smallFont = new Font(Skia.harmony(), 9f);
	private final Font titleFont = new Font(Skia.harmonyMedium(), 12f);

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
		panelW = Math.min(width - 80, 600);
		panelH = Math.min(height - 80, 380);
		px = (width - panelW) / 2;
		py = (height - panelH) / 2;
		layoutHome();
		layoutLoginModal();
	}

	/** The login pop-up is a centered card drawn over the home panel (coords are panel-local). */
	private void layoutLoginModal() {
		int mW = 260;
		int mH = 264;
		int mX = (panelW - mW) / 2;
		int mY = (panelH - mH) / 2;
		rect(modalCard, mX, mY, mW, mH);
		rect(closeBtn, mX + mW - 24, mY + 8, 16, 16);
		int cx = mX + mW / 2;
		rect(tabPhone, cx - 106, mY + 34, 102, 22);
		rect(tabQr, cx + 4, mY + 34, 102, 22);
		phoneField.bounds(cx - 105, mY + 88, 210, 22);
		passwordField.bounds(cx - 105, mY + 118, 210, 22);
		rect(loginBtn, cx - 105, mY + 152, 210, 24);
	}

	private void layoutHome() {
		playerTop = panelH - PLAYER_H;
		rect(avatarBtn, panelW - 12 - 18, 7, 18, 18);
		rect(userMenuRect, panelW - 12 - 88, 30, 88, 24);
		int searchX = SIDE_W + 8;
		searchField.bounds(searchX, 8, avatarBtn[0] - 8 - searchX, 16);

		listY = TOP_H + 15;
		listH = playerTop - 6 - listY;
		plX = 10;
		plW = SIDE_W - 20;
		tkX = SIDE_W + 8;
		tkW = panelW - 10 - tkX;

		int r = 12;
		int cx = panelW / 2;
		int cy = playerTop + PLAYER_H / 2;
		rect(playBtn, cx - r, cy - r, r * 2, r * 2);
		rect(prevBtn, cx - r - 6 - 16, cy - 8, 16, 16);
		rect(nextBtn, cx + r + 6, cy - 8, 16, 16);
		rect(queueBtn, panelW - 12 - 18, cy - 9, 18, 18);
		rect(modeBtn, queueBtn[0] - 6 - 32, cy - 9, 32, 18);
	}

	private static void rect(int[] r, int x, int y, int w, int h) {
		r[0] = x;
		r[1] = y;
		r[2] = w;
		r[3] = h;
	}

	// ---------- login ----------

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
		String phone = phoneField.text.trim();
		String password = passwordField.text;
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
		if (!loginOpen || !qrTab || qrKey == null || loggedIn || polling) {
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
		loginOpen = false;
		userMenuOpen = false;
		status = "";
		focused = null;
		loadPlaylists();
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
		selectedPlaylist = index;
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
		String keyword = searchField.text.trim();
		if (keyword.isEmpty()) {
			return;
		}
		showQueue = false;
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

	private void playPause() {
		if (MusicPlayer.isActive()) {
			MusicPlayer.togglePause();
		} else {
			playSelected();
		}
	}

	// ---------- render ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0x88101014);
		int lmx = mouseX - px;
		int lmy = mouseY - py;

		Window win = Minecraft.getInstance().getWindow();
		float scale = win.getGuiScaledWidth() > 0 ? Math.min(2.0f, (float) win.getWidth() / win.getGuiScaledWidth()) : 1f;
		int sw = Math.max(1, Math.round(panelW * scale));
		int sh = Math.max(1, Math.round(panelH * scale));
		try {
			ui.render(sw, sh, canvas -> {
				canvas.scale(scale, scale);
				drawPanel(canvas, lmx, lmy);
			});
			graphics.blit(RenderPipelines.GUI_TEXTURED, ui.textureId(), px, py, 0.0F, 0.0F, panelW, panelH, sw, sh, sw, sh);
		} catch (Throwable ignored) {
			return;
		}

		// covers (MC textures) blitted on top of the Skija panel
		if (loginOpen) {
			// dimmed home behind the modal — only the QR (also a texture) sits on the card
			if (qrTab && qrReady) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, QR_TEXTURE,
						px + modalCard[0] + modalCard[2] / 2 - QR_SIZE / 2, py + modalCard[1] + 70,
						0.0F, 0.0F, QR_SIZE, QR_SIZE, QR_SIZE, QR_SIZE);
			}
		} else {
			if (!showQueue) {
				blitTrackThumbs(graphics);
			}
			if (NowPlaying.hasCover) {
				int cy = py + playerTop + (PLAYER_H - COVER) / 2;
				graphics.blit(RenderPipelines.GUI_TEXTURED, NowPlaying.COVER, px + 10, cy, 0.0F, 0.0F,
						COVER, COVER, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX, NowPlaying.COVER_PX);
			}
		}
	}

	private void blitTrackThumbs(GuiGraphicsExtractor graphics) {
		int thumb = ROW_H - 4;
		int visible = listH / ROW_H;
		graphics.enableScissor(px + tkX, py + listY, px + tkX + tkW, py + listY + listH);
		for (int i = trackScroll; i < trackIds.size() && i < trackScroll + visible + 1; i++) {
			int ry = listY + (i - trackScroll) * ROW_H;
			Identifier tex = CoverCache.get(trackIds.get(i), trackCovers.get(i));
			if (tex != null) {
				graphics.blit(RenderPipelines.GUI_TEXTURED, tex, px + tkX + 3, py + ry + 2, 0.0F, 0.0F,
						thumb, thumb, CoverCache.PX, CoverCache.PX, CoverCache.PX, CoverCache.PX);
			}
		}
		graphics.disableScissor();
	}

	private void drawPanel(Canvas canvas, int lmx, int lmy) {
		try (Paint p = new Paint()) {
			p.setAntiAlias(true);
			p.setColor(PANEL_BG);
			canvas.drawRRect(RRect.makeXYWH(0, 0, panelW, panelH, 10), p);

			drawString(canvas, titleFont, p, "网易云音乐", 12, baseline(titleFont, 8, 20), ACCENT);
			drawHome(canvas, p, lmx, lmy);
			if (loginOpen) {
				drawLoginModal(canvas, p, lmx, lmy);
			}
			if (userMenuOpen) {
				drawUserMenu(canvas, p, lmx, lmy);
			}
		}
	}

	private void drawLoginModal(Canvas canvas, Paint p, int lmx, int lmy) {
		// dim the home panel, then the card
		p.setColor(0x66000000);
		canvas.drawRRect(RRect.makeXYWH(0, 0, panelW, panelH, 10), p);
		int mX = modalCard[0];
		int mY = modalCard[1];
		p.setColor(0xFFFFFFFF);
		canvas.drawRRect(RRect.makeXYWH(mX, mY, modalCard[2], modalCard[3], 10), p);

		drawString(canvas, titleFont, p, "登录", mX + 14, baseline(titleFont, mY + 6, 20), TEXT);
		// close X
		boolean closeHover = in(closeBtn, lmx, lmy);
		p.setColor(closeHover ? ACCENT : 0xFF888888);
		int cxx = closeBtn[0] + closeBtn[2] / 2;
		int cyy = closeBtn[1] + closeBtn[3] / 2;
		p.setStrokeWidth(1.6f);
		p.setMode(io.github.humbleui.skija.PaintMode.STROKE);
		canvas.drawLine(cxx - 4, cyy - 4, cxx + 4, cyy + 4, p);
		canvas.drawLine(cxx + 4, cyy - 4, cxx - 4, cyy + 4, p);
		p.setMode(io.github.humbleui.skija.PaintMode.FILL);

		drawButton(canvas, p, tabPhone, "手机号登录", !qrTab, lmx, lmy);
		drawButton(canvas, p, tabQr, "扫码登录", qrTab, lmx, lmy);

		if (qrTab) {
			p.setColor(0xFFF2F2F4);
			canvas.drawRRect(RRect.makeXYWH(mX + modalCard[2] / 2f - QR_SIZE / 2f - 7, mY + 63, QR_SIZE + 14, QR_SIZE + 14, 8), p);
		} else {
			drawField(canvas, p, phoneField, lmx, lmy);
			drawField(canvas, p, passwordField, lmx, lmy);
			drawButton(canvas, p, loginBtn, "登 录", true, lmx, lmy);
		}
		if (!status.isEmpty()) {
			float tw = bodyFont.measureTextWidth(status);
			drawString(canvas, bodyFont, p, status, mX + modalCard[2] / 2f - tw / 2f, mY + modalCard[3] - 14, SUBTEXT);
		}
	}

	private void drawUserMenu(Canvas canvas, Paint p, int lmx, int lmy) {
		boolean hover = in(userMenuRect, lmx, lmy);
		p.setColor(0x33000000);
		canvas.drawRRect(RRect.makeXYWH(userMenuRect[0] - 1, userMenuRect[1] - 1, userMenuRect[2] + 2, userMenuRect[3] + 2, 7), p);
		p.setColor(0xFFFFFFFF);
		canvas.drawRRect(RRect.makeXYWH(userMenuRect[0], userMenuRect[1], userMenuRect[2], userMenuRect[3], 6), p);
		if (hover) {
			p.setColor(ROW_SEL);
			canvas.drawRRect(RRect.makeXYWH(userMenuRect[0], userMenuRect[1], userMenuRect[2], userMenuRect[3], 6), p);
		}
		drawString(canvas, bodyFont, p, "退出登录", userMenuRect[0] + 12,
				baseline(bodyFont, userMenuRect[1], userMenuRect[3]), hover ? ACCENT : TEXT);
	}

	private void drawHome(Canvas canvas, Paint p, int lmx, int lmy) {
		// top bar + sidebar + dividers
		p.setColor(SIDEBAR_BG);
		canvas.drawRect(Rect.makeXYWH(0, TOP_H, SIDE_W, playerTop - TOP_H), p);
		p.setColor(DIVIDER);
		canvas.drawRect(Rect.makeXYWH(0, TOP_H, panelW, 1), p);
		canvas.drawRect(Rect.makeXYWH(SIDE_W, TOP_H, 1, playerTop - TOP_H), p);

		drawField(canvas, p, searchField, lmx, lmy);
		drawAvatar(canvas, p, avatarBtn, loggedIn, lmx, lmy);

		drawString(canvas, bodyFont, p, "我的歌单", plX, baseline(bodyFont, TOP_H + 3, ROW_H - 4), SUBTEXT);
		if (playlistNames.isEmpty()) {
			drawString(canvas, smallFont, p, "点击右上角头像登录", plX, listY + 10, SUBTEXT);
		}
		drawList(canvas, p, plX, listY, plW, listH, playlistNames, playlistScroll, selectedPlaylist, -1, false, lmx, lmy);

		long pid = NowPlaying.playingId();
		if (showQueue) {
			List<String> queue = queueLabels();
			drawString(canvas, bodyFont, p, "播放列表 (" + queue.size() + ")", tkX, baseline(bodyFont, TOP_H + 3, ROW_H - 4), SUBTEXT);
			drawList(canvas, p, tkX, listY, tkW, listH, queue, trackScroll, NowPlaying.queueIndex(), NowPlaying.queueIndex(), false, lmx, lmy);
		} else {
			drawString(canvas, bodyFont, p, "歌曲", tkX, baseline(bodyFont, TOP_H + 3, ROW_H - 4), SUBTEXT);
			int playing = pid < 0 ? -1 : trackIds.indexOf(pid);
			drawList(canvas, p, tkX, listY, tkW, listH, trackLabels, trackScroll, trackSelected, playing, true, lmx, lmy);
		}

		drawPlayerBar(canvas, p, lmx, lmy);
	}

	private List<String> queueLabels() {
		List<String> titles = NowPlaying.queueTitles();
		List<String> artists = NowPlaying.queueArtists();
		List<String> labels = new ArrayList<>(titles.size());
		for (int i = 0; i < titles.size(); i++) {
			String a = i < artists.size() ? artists.get(i) : "";
			labels.add(a.isEmpty() ? titles.get(i) : titles.get(i) + " - " + a);
		}
		return labels;
	}

	private void drawPlayerBar(Canvas canvas, Paint p, int lmx, int lmy) {
		long pos = MusicPlayer.positionMs();
		long dur = NowPlaying.durationMs;
		float frac = dur > 0 ? Math.min(1f, pos / (float) dur) : 0f;
		p.setColor(TRACK_BAR);
		canvas.drawRect(Rect.makeXYWH(0, playerTop, panelW, 2), p);
		if (frac > 0) {
			p.setColor(ACCENT);
			canvas.drawRect(Rect.makeXYWH(0, playerTop, panelW * frac, 2), p);
		}

		int cy = playerTop + (PLAYER_H - COVER) / 2;
		if (!NowPlaying.hasCover) {
			p.setColor(0xFFE8E8EA);
			canvas.drawRRect(RRect.makeXYWH(10, cy, COVER, COVER, 5), p);
		}
		int textX = 10 + COVER + 8;
		int textW = playBtn[0] - 30 - textX;
		if (textW > 20) {
			drawString(canvas, bodyFont, p, trim(bodyFont, NowPlaying.title, textW), textX, cy + 12, TEXT);
			drawString(canvas, smallFont, p, trim(smallFont, NowPlaying.artist, textW), textX, cy + 24, SUBTEXT);
			String lyric = NowPlaying.currentLyric();
			if (!lyric.isEmpty()) {
				drawString(canvas, smallFont, p, trim(smallFont, lyric, textW), textX, cy + 36, LYRIC);
			}
		}

		// transport icons
		drawPrevNext(canvas, p, prevBtn, true);
		drawPrevNext(canvas, p, nextBtn, false);
		int pcx = playBtn[0] + playBtn[2] / 2;
		int pcy = playBtn[1] + playBtn[3] / 2;
		p.setColor(ACCENT);
		canvas.drawCircle(pcx, pcy, playBtn[2] / 2f, p);
		p.setColor(0xFFFFFFFF);
		if (MusicPlayer.isActive() && !MusicPlayer.isPaused()) {
			canvas.drawRect(Rect.makeXYWH(pcx - 4, pcy - 5, 3, 10), p);
			canvas.drawRect(Rect.makeXYWH(pcx + 1, pcy - 5, 3, 10), p);
		} else {
			try (Path tri = Path.makePolygon(new Point[] {
					new Point(pcx - 4, pcy - 6), new Point(pcx - 4, pcy + 6), new Point(pcx + 6, pcy)
			}, true)) {
				canvas.drawPath(tri, p);
			}
		}

		String modeShort = NowPlaying.mode() == 1 ? "单曲" : NowPlaying.mode() == 2 ? "随机" : "顺序";
		drawButton(canvas, p, modeBtn, modeShort, false, lmx, lmy);
		drawQueueIcon(canvas, p, queueBtn, showQueue, lmx, lmy);

		String time = fmt(pos) + " / " + fmt(dur);
		float tw = smallFont.measureTextWidth(time);
		drawString(canvas, smallFont, p, time, modeBtn[0] - 8 - tw, baseline(smallFont, modeBtn[1], modeBtn[3]), SUBTEXT);
	}

	private void drawAvatar(Canvas canvas, Paint p, int[] r, boolean logged, int lmx, int lmy) {
		int cx = r[0] + r[2] / 2;
		int cy = r[1] + r[3] / 2;
		float rad = r[2] / 2f;
		boolean hover = in(r, lmx, lmy);
		p.setColor(logged ? ACCENT : hover ? 0xFFA8A8AC : 0xFFB8B8BC);
		canvas.drawCircle(cx, cy, rad, p);
		canvas.save();
		canvas.clipRRect(RRect.makeXYWH(r[0], r[1], r[2], r[3], rad));
		p.setColor(0xFFFFFFFF);
		canvas.drawCircle(cx, cy - rad * 0.16f, rad * 0.34f, p);
		canvas.drawCircle(cx, cy + rad * 0.66f, rad * 0.6f, p);
		canvas.restore();
	}

	private void drawQueueIcon(Canvas canvas, Paint p, int[] r, boolean active, int lmx, int lmy) {
		boolean hover = in(r, lmx, lmy);
		p.setColor(active ? 0x1AEC4141 : hover ? 0xFFEAEAEC : 0x00000000);
		if (active || hover) {
			canvas.drawRRect(RRect.makeXYWH(r[0], r[1], r[2], r[3], 4), p);
		}
		p.setColor(active ? ACCENT : 0xFF555555);
		int lx = r[0] + 4;
		int lw = r[2] - 8;
		for (int i = 0; i < 3; i++) {
			canvas.drawRect(Rect.makeXYWH(lx, r[1] + 5 + i * 4, lw, 1.5f), p);
		}
	}

	private void drawPrevNext(Canvas canvas, Paint p, int[] r, boolean prev) {
		int cx = r[0] + r[2] / 2;
		int cy = r[1] + r[3] / 2;
		p.setColor(0xFF555555);
		Point[] pts = prev
				? new Point[] { new Point(cx + 4, cy - 5), new Point(cx + 4, cy + 5), new Point(cx - 3, cy) }
				: new Point[] { new Point(cx - 4, cy - 5), new Point(cx - 4, cy + 5), new Point(cx + 3, cy) };
		try (Path tri = Path.makePolygon(pts, true)) {
			canvas.drawPath(tri, p);
		}
		canvas.drawRect(Rect.makeXYWH(prev ? cx - 5 : cx + 3, cy - 5, 2, 10), p);
	}

	private void drawList(Canvas canvas, Paint p, int x, int y, int w, int h, List<String> items,
			int scroll, int selected, int playing, boolean thumbs, int lmx, int lmy) {
		canvas.save();
		canvas.clipRect(Rect.makeXYWH(x, y, w, h));
		int thumb = ROW_H - 4;
		int visible = h / ROW_H;
		for (int i = scroll; i < items.size() && i < scroll + visible + 1; i++) {
			int ry = y + (i - scroll) * ROW_H;
			boolean hover = lmx >= x && lmx < x + w && lmy >= ry && lmy < ry + ROW_H && lmy >= y && lmy < y + h;
			if (i == selected) {
				p.setColor(ROW_SEL);
				canvas.drawRect(Rect.makeXYWH(x, ry, w, ROW_H), p);
			} else if (hover) {
				p.setColor(ROW_HOVER);
				canvas.drawRect(Rect.makeXYWH(x, ry, w, ROW_H), p);
			}
			int textX = x + 5;
			if (thumbs) {
				p.setColor(0x14000000);
				canvas.drawRRect(RRect.makeXYWH(x + 3, ry + 2, thumb, thumb, 2), p);
				textX = x + 3 + thumb + 5;
			}
			int color = i == playing ? ACCENT : i == selected ? ACCENT : TEXT;
			if (i == playing) {
				p.setColor(ACCENT);
				canvas.drawRect(Rect.makeXYWH(x, ry + 3, 2, ROW_H - 6), p);
			}
			drawString(canvas, bodyFont, p, trim(bodyFont, items.get(i), w - (textX - x) - 5), textX, ry + 13, color);
		}
		canvas.restore();
	}

	private void drawField(Canvas canvas, Paint p, Field field, int lmx, int lmy) {
		p.setColor(field == focused ? 0xFFFFFFFF : FIELD_BG);
		canvas.drawRRect(RRect.makeXYWH(field.x, field.y, field.w, field.h, field.h / 2f), p);
		if (field == focused) {
			p.setColor(ACCENT);
			p.setMode(io.github.humbleui.skija.PaintMode.STROKE);
			p.setStrokeWidth(1);
			canvas.drawRRect(RRect.makeXYWH(field.x + 0.5f, field.y + 0.5f, field.w - 1, field.h - 1, (field.h - 1) / 2f), p);
			p.setMode(io.github.humbleui.skija.PaintMode.FILL);
		}
		String shown = field.shown();
		boolean empty = shown.isEmpty();
		String text = empty ? field.hint : shown;
		float baseline = baseline(bodyFont, field.y, field.h);
		drawString(canvas, bodyFont, p, trim(bodyFont, text, field.w - 16), field.x + 8, baseline, empty ? SUBTEXT : TEXT);
		if (field == focused && (System.currentTimeMillis() / 500) % 2 == 0) {
			float cw = bodyFont.measureTextWidth(empty ? "" : trim(bodyFont, shown, field.w - 16));
			p.setColor(TEXT);
			canvas.drawRect(Rect.makeXYWH(field.x + 8 + cw + 1, field.y + 4, 1, field.h - 8), p);
		}
	}

	private void drawButton(Canvas canvas, Paint p, int[] r, String label, boolean active, int lmx, int lmy) {
		boolean hover = lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
		p.setColor(active ? ACCENT : hover ? 0xFFEAEAEC : FIELD_BG);
		canvas.drawRRect(RRect.makeXYWH(r[0], r[1], r[2], r[3], 5), p);
		float tw = bodyFont.measureTextWidth(label);
		drawString(canvas, bodyFont, p, label, r[0] + (r[2] - tw) / 2f, baseline(bodyFont, r[1], r[3]), active ? 0xFFFFFFFF : TEXT);
	}

	private static void drawString(Canvas canvas, Font font, Paint paint, String s, float x, float baseline, int color) {
		paint.setColor(color);
		paint.setAntiAlias(true);
		canvas.drawString(s, x, baseline, font, paint);
	}

	private static float baseline(Font font, float y, float h) {
		FontMetrics m = font.getMetrics();
		return y + h / 2f - (m.getAscent() + m.getDescent()) / 2f;
	}

	private String trim(Font font, String s, float maxWidth) {
		if (s == null) {
			return "";
		}
		if (font.measureTextWidth(s) <= maxWidth) {
			return s;
		}
		while (s.length() > 1 && font.measureTextWidth(s + "…") > maxWidth) {
			s = s.substring(0, s.length() - 1);
		}
		return s + "…";
	}

	// ---------- input ----------

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		int lmx = (int) event.x() - px;
		int lmy = (int) event.y() - py;
		focused = null;

		// the avatar dropdown takes priority while open — only its item logs out
		if (userMenuOpen) {
			userMenuOpen = false;
			if (in(userMenuRect, lmx, lmy)) {
				logout();
			}
			return true;
		}

		// login modal (overlay on the home panel): a click outside the card or on X closes it
		if (loginOpen) {
			if (in(closeBtn, lmx, lmy) || !in(modalCard, lmx, lmy)) {
				loginOpen = false;
				return true;
			}
			if (in(tabPhone, lmx, lmy)) {
				qrTab = false;
				return true;
			}
			if (in(tabQr, lmx, lmy)) {
				qrTab = true;
				if (qrKey == null) {
					startQr();
				}
				return true;
			}
			if (!qrTab) {
				if (phoneField.contains(lmx, lmy)) {
					focused = phoneField;
					return true;
				}
				if (passwordField.contains(lmx, lmy)) {
					focused = passwordField;
					return true;
				}
				if (in(loginBtn, lmx, lmy)) {
					doPhoneLogin();
					return true;
				}
			}
			return true;
		}

		// click the progress bar (thin strip atop the player bar) to seek
		if (lmx >= 0 && lmx <= panelW && lmy >= playerTop - 2 && lmy <= playerTop + 6 && NowPlaying.durationMs > 0) {
			float f = Math.max(0f, Math.min(1f, lmx / (float) panelW));
			MusicPlayer.seek((long) (f * NowPlaying.durationMs));
			return true;
		}
		if (in(avatarBtn, lmx, lmy)) {
			if (loggedIn) {
				userMenuOpen = true;
			} else {
				loginOpen = true;
				if (qrTab && qrKey == null) {
					startQr();
				}
			}
			return true;
		}
		if (searchField.contains(lmx, lmy)) {
			focused = searchField;
			return true;
		}
		if (in(prevBtn, lmx, lmy)) {
			NowPlaying.previous();
			return true;
		}
		if (in(playBtn, lmx, lmy)) {
			playPause();
			return true;
		}
		if (in(nextBtn, lmx, lmy)) {
			NowPlaying.next();
			return true;
		}
		if (in(modeBtn, lmx, lmy)) {
			NowPlaying.cycleMode();
			return true;
		}
		if (in(queueBtn, lmx, lmy)) {
			showQueue = !showQueue;
			trackScroll = 0;
			return true;
		}
		int pl = listIndex(plX, plW, lmx, lmy, playlistScroll, playlistNames.size());
		if (pl >= 0) {
			selectPlaylist(pl);
			showQueue = false;
			return true;
		}
		if (showQueue) {
			int q = listIndex(tkX, tkW, lmx, lmy, trackScroll, NowPlaying.queueSize());
			if (q >= 0) {
				NowPlaying.playIndex(q);
				return true;
			}
			return false;
		}
		int tk = listIndex(tkX, tkW, lmx, lmy, trackScroll, trackLabels.size());
		if (tk >= 0) {
			trackSelected = tk;
			if (doubleClick) {
				playSelected();
			}
			return true;
		}
		return false;
	}

	private void logout() {
		NeteaseApi.logout();
		loggedIn = false;
		playlistNames.clear();
		playlistIds.clear();
		playlistCovers.clear();
		trackLabels.clear();
		trackIds.clear();
		trackTitles.clear();
		trackArtists.clear();
		trackCovers.clear();
		trackDurations.clear();
		selectedPlaylist = -1;
		trackSelected = -1;
		showQueue = false;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (focused != null) {
			focused.type(event.codepoint());
			return true;
		}
		return false;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (focused != null) {
			int key = event.key();
			if (key == GLFW.GLFW_KEY_BACKSPACE) {
				focused.backspace();
				return true;
			}
			if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
				if (focused == searchField) {
					doSearch();
				} else if (focused == phoneField || focused == passwordField) {
					doPhoneLogin();
				}
				return true;
			}
			if (key == GLFW.GLFW_KEY_ESCAPE) {
				focused = null;
				return true;
			}
			return true;
		}
		// Esc closes an open overlay instead of the whole screen
		if (event.key() == GLFW.GLFW_KEY_ESCAPE && (loginOpen || userMenuOpen)) {
			loginOpen = false;
			userMenuOpen = false;
			return true;
		}
		return super.keyPressed(event);
	}

	private boolean in(int[] r, int lmx, int lmy) {
		return lmx >= r[0] && lmx < r[0] + r[2] && lmy >= r[1] && lmy < r[1] + r[3];
	}

	private int listIndex(int x, int w, int lmx, int lmy, int scroll, int count) {
		if (lmx < x || lmx > x + w || lmy < listY || lmy > listY + listH) {
			return -1;
		}
		int row = scroll + (lmy - listY) / ROW_H;
		return row >= 0 && row < count ? row : -1;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		if (!loginOpen) {
			int lmx = (int) x - px;
			int delta = scrollY > 0 ? -1 : 1;
			if (lmx >= plX && lmx <= plX + plW) {
				playlistScroll = clampScroll(playlistScroll + delta, playlistNames.size());
				return true;
			}
			if (lmx >= tkX && lmx <= tkX + tkW) {
				int count = showQueue ? NowPlaying.queueSize() : trackLabels.size();
				trackScroll = clampScroll(trackScroll + delta, count);
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

	// ---------- text field ----------

	private final class Field {
		private final String hint;
		private final boolean password;
		private final int maxLen;
		String text = "";
		int x;
		int y;
		int w;
		int h;

		Field(String hint, boolean password, int maxLen) {
			this.hint = hint;
			this.password = password;
			this.maxLen = maxLen;
		}

		void bounds(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		boolean contains(int lmx, int lmy) {
			return lmx >= x && lmx < x + w && lmy >= y && lmy < y + h;
		}

		void type(int codepoint) {
			if (codepoint >= 32 && text.length() < maxLen) {
				text += new String(Character.toChars(codepoint));
			}
		}

		void backspace() {
			if (!text.isEmpty()) {
				text = text.substring(0, text.length() - 1);
			}
		}

		String shown() {
			return password ? "•".repeat(text.length()) : text;
		}
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

	private static String fmt(long ms) {
		long s = Math.max(0, ms) / 1000;
		long sec = s % 60;
		return (s / 60) + ":" + (sec < 10 ? "0" : "") + sec;
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
