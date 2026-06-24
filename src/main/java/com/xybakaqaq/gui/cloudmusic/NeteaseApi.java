package com.xybakaqaq.gui.cloudmusic;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xybakaqaq.config.Config;

/**
 * Minimal NetEase Cloud Music (music.163.com) login client.
 *
 * <p>Implements the "weapi" request encryption (two AES-128-CBC passes + an RSA-encrypted random
 * key) and the QR-code and cellphone login endpoints. On success the {@code MUSIC_U} cookie is
 * captured into {@link #cookie()}.
 *
 * <p>All methods do blocking HTTP — call them off the render thread.
 */
public final class NeteaseApi {

	private static final String BASE = "https://music.163.com";
	// weapi constants (well-known public values).
	private static final String AES_KEY = "0CoJUm6Qyw8W8jud";
	private static final String AES_IV = "0102030405060708";
	private static final String RSA_PUBKEY = "010001";
	private static final String RSA_MODULUS =
			"00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725"
			+ "152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312"
			+ "ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424"
			+ "d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
	private static final String BASE62 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Gson GSON = new Gson();
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	private static volatile String cookie = "os=pc; appver=8.9.70";

	private NeteaseApi() {
	}

	/** @return the current cookie (includes {@code MUSIC_U} after a successful login). */
	public static String cookie() {
		return cookie;
	}

	/** Persists the current cookie so the next session can skip login. */
	public static void saveCookie() {
		try {
			Path file = cookieFile();
			Files.createDirectories(file.getParent());
			Files.writeString(file, cookie);
		} catch (Exception ignored) {
			// best-effort
		}
	}

	/** Clears the login: resets the cookie to the anonymous default and deletes the saved file. */
	public static void logout() {
		cookie = "os=pc; appver=8.9.70";
		try {
			Files.deleteIfExists(cookieFile());
		} catch (Exception ignored) {
			// best-effort
		}
	}

	/** Loads a previously saved cookie. @return {@code true} if one was found. */
	public static boolean loadCookie() {
		try {
			Path file = cookieFile();
			if (Files.exists(file)) {
				cookie = Files.readString(file).trim();
				return true;
			}
		} catch (Exception ignored) {
			// best-effort
		}
		return false;
	}

	private static Path cookieFile() {
		return Config.cookieFile();
	}

	// ---------- QR login ----------

	/** Requests a new QR {@code unikey}. */
	public static String qrKey() throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("type", 1);
		return post("/weapi/login/qrcode/unikey", body, false).get("unikey").getAsString();
	}

	/** The URL the QR code should encode (rendered locally by the UI). */
	public static String qrUrl(String key) {
		return BASE + "/login?codekey=" + key;
	}

	/**
	 * Polls the QR login state.
	 *
	 * @return the response {@code code}: 800 = expired, 801 = waiting for scan, 802 = scanned
	 *         (awaiting confirm), 803 = authorized (login complete, cookie captured).
	 */
	public static int qrCheck(String key) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("key", key);
		body.addProperty("type", 1);
		return post("/weapi/login/qrcode/client/login", body, true).get("code").getAsInt();
	}

	// ---------- cellphone login ----------

	/** Logs in with phone + password; returns the response {@code code} (200 = success). */
	public static int loginCellphone(String phone, String password) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("phone", phone);
		body.addProperty("countrycode", "86");
		body.addProperty("password", md5(password));
		body.addProperty("rememberLogin", "true");
		return post("/weapi/login/cellphone", body, true).get("code").getAsInt();
	}

	// ---------- account / playlists / search / playback ----------

	/** @return the logged-in user id, or 0 if not logged in. */
	public static long userId() throws Exception {
		JsonObject res = post("/weapi/w/nuser/account/get", new JsonObject(), false);
		if (res.has("account") && res.get("account").isJsonObject()) {
			JsonObject account = res.getAsJsonObject("account");
			if (account.has("id")) {
				return account.get("id").getAsLong();
			}
		}
		return 0;
	}

	/** The user's playlists (each has {@code id}, {@code name}, {@code trackCount}). */
	public static JsonArray userPlaylists(long uid) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("uid", uid);
		body.addProperty("limit", 100);
		body.addProperty("offset", 0);
		JsonObject res = post("/weapi/user/playlist", body, false);
		return res.has("playlist") ? res.getAsJsonArray("playlist") : new JsonArray();
	}

	/** A playlist's tracks (each has {@code id}, {@code name}, {@code ar}[].name). */
	public static JsonArray playlistTracks(long id) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("id", id);
		body.addProperty("n", 1000);
		JsonObject res = post("/weapi/v6/playlist/detail", body, false);
		if (res.has("playlist") && res.getAsJsonObject("playlist").has("tracks")) {
			return res.getAsJsonObject("playlist").getAsJsonArray("tracks");
		}
		return new JsonArray();
	}

	/** Song search (each result has {@code id}, {@code name}, {@code artists}[].name). */
	public static JsonArray search(String keyword) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("s", keyword);
		body.addProperty("type", 1);
		body.addProperty("limit", 30);
		body.addProperty("offset", 0);
		JsonObject res = post("/weapi/search/get", body, false);
		if (res.has("result") && res.get("result").isJsonObject()) {
			JsonObject result = res.getAsJsonObject("result");
			if (result.has("songs")) {
				return result.getAsJsonArray("songs");
			}
		}
		return new JsonArray();
	}

	/** @return the raw LRC lyric text for a song, or {@code null} if none. */
	public static String lyric(long id) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("id", id);
		body.addProperty("lv", -1);
		body.addProperty("tv", -1);
		JsonObject res = post("/weapi/song/lyric", body, false);
		if (res.has("lrc") && res.get("lrc").isJsonObject()) {
			JsonObject lrc = res.getAsJsonObject("lrc");
			if (lrc.has("lyric") && !lrc.get("lyric").isJsonNull()) {
				return lrc.get("lyric").getAsString();
			}
		}
		return null;
	}

	/** Plain GET returning raw bytes (used for cover images). */
	public static byte[] httpBytes(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.header("User-Agent", "Mozilla/5.0")
				.header("Referer", BASE)
				.GET()
				.build();
		return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
	}

	/** @return a playable MP3 URL for the song, or {@code null} (e.g. VIP-only without rights). */
	public static String songUrl(long id) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("ids", "[" + id + "]");
		body.addProperty("br", 320000);
		JsonObject res = post("/weapi/song/enhance/player/url", body, false);
		JsonArray data = res.has("data") ? res.getAsJsonArray("data") : null;
		if (data != null && data.size() > 0) {
			JsonObject first = data.get(0).getAsJsonObject();
			if (first.has("url") && !first.get("url").isJsonNull()) {
				return first.get("url").getAsString();
			}
		}
		return null;
	}

	// ---------- HTTP ----------

	private static JsonObject post(String path, JsonObject body, boolean captureCookie) throws Exception {
		body.addProperty("csrf_token", "");
		Map<String, String> enc = weapi(GSON.toJson(body));
		String form = "params=" + enc(enc.get("params")) + "&encSecKey=" + enc(enc.get("encSecKey"));

		HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + path))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36")
				.header("Referer", BASE)
				.header("Cookie", cookie)
				.POST(HttpRequest.BodyPublishers.ofString(form))
				.build();
		HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

		if (captureCookie) {
			for (String setCookie : response.headers().allValues("set-cookie")) {
				if (setCookie.startsWith("MUSIC_U=")) {
					cookie = "os=pc; " + setCookie.split(";", 2)[0];
				}
			}
		}
		return JsonParser.parseString(response.body()).getAsJsonObject();
	}

	// ---------- weapi encryption ----------

	private static Map<String, String> weapi(String text) throws Exception {
		String secretKey = randomKey();
		String params = aes(aes(text, AES_KEY), secretKey);
		String encSecKey = rsa(new StringBuilder(secretKey).reverse().toString());
		Map<String, String> result = new LinkedHashMap<>();
		result.put("params", params);
		result.put("encSecKey", encSecKey);
		return result;
	}

	private static String aes(String text, String key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE,
				new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES"),
				new IvParameterSpec(AES_IV.getBytes(StandardCharsets.UTF_8)));
		return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static String rsa(String text) {
		StringBuilder hex = new StringBuilder();
		for (byte b : text.getBytes(StandardCharsets.UTF_8)) {
			hex.append(String.format("%02x", b & 0xff));
		}
		BigInteger result = new BigInteger(hex.toString(), 16)
				.modPow(new BigInteger(RSA_PUBKEY, 16), new BigInteger(RSA_MODULUS, 16));
		String out = result.toString(16);
		return "0".repeat(Math.max(0, 256 - out.length())) + out;
	}

	private static String randomKey() {
		StringBuilder sb = new StringBuilder(16);
		for (int i = 0; i < 16; i++) {
			sb.append(BASE62.charAt(RANDOM.nextInt(BASE62.length())));
		}
		return sb.toString();
	}

	private static String md5(String text) throws Exception {
		byte[] digest = MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	private static String enc(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
