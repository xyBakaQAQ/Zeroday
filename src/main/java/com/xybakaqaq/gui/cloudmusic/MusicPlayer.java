package com.xybakaqaq.gui.cloudmusic;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

/**
 * MP3 stream player: decodes the song URL with JLayer (pure Java) and writes PCM to a
 * {@link SourceDataLine}. No JavaFX. Decoding runs on a daemon thread; the controls are
 * thread-safe.
 */
public final class MusicPlayer {

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();

	/** The thread that owns playback right now. A stream stops as soon as it is no longer current. */
	private static volatile Thread current;
	private static volatile boolean paused;
	private static volatile long positionMs;

	private MusicPlayer() {
	}

	/** @return current playback position in milliseconds. */
	public static long positionMs() {
		return positionMs;
	}

	/** @return whether playback is currently paused. */
	public static boolean isPaused() {
		return paused;
	}

	/** @return whether a track is loaded and not stopped (playing or paused). */
	public static boolean isActive() {
		return current != null;
	}

	/** Toggles between pause and resume. */
	public static void togglePause() {
		paused = !paused;
	}

	public static synchronized void play(String url) {
		stop();
		paused = false;
		positionMs = 0;
		Thread t = new Thread(() -> stream(url), "cloudmusic-audio");
		t.setDaemon(true);
		current = t;
		t.start();
	}

	public static void pause() {
		paused = true;
	}

	public static void resume() {
		paused = false;
	}

	public static synchronized void stop() {
		Thread t = current;
		current = null;
		if (t != null) {
			t.interrupt();
		}
	}

	private static void stream(String url) {
		Thread self = Thread.currentThread();
		SourceDataLine line = null;
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.header("User-Agent", "Mozilla/5.0")
					.GET()
					.build();
			InputStream in = new BufferedInputStream(HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream()).body());

			Bitstream bitstream = new Bitstream(in);
			Decoder decoder = new Decoder();
			Header header;
			long playedFrames = 0;
			while (current == self && (header = bitstream.readFrame()) != null) {
				while (paused && current == self) {
					Thread.sleep(40);
				}
				if (current != self) {
					break;
				}
				SampleBuffer output = (SampleBuffer) decoder.decodeFrame(header, bitstream);
				int rate = output.getSampleFrequency();
				int channels = output.getChannelCount();
				if (line == null) {
					AudioFormat format = new AudioFormat(rate, 16, channels, true, false);
					line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));
					line.open(format);
					line.start();
				}
				short[] samples = output.getBuffer();
				int len = output.getBufferLength();
				playedFrames += len / Math.max(1, channels);
				if (current == self) {
					positionMs = playedFrames * 1000L / Math.max(1, rate);
				}
				byte[] bytes = new byte[len * 2];
				for (int i = 0; i < len; i++) {
					bytes[i * 2] = (byte) (samples[i] & 0xff);
					bytes[i * 2 + 1] = (byte) ((samples[i] >> 8) & 0xff);
				}
				line.write(bytes, 0, bytes.length);
				bitstream.closeFrame();
			}
			bitstream.close();
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
		} catch (Exception ignored) {
			// network / decode error — stop silently
		} finally {
			if (line != null) {
				line.stop();
				line.close();
			}
			// Release ownership if we finished naturally (and weren't already replaced by a new track).
			if (current == self) {
				current = null;
			}
		}
	}
}
