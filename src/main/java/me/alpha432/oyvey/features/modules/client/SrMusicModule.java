package me.alpha432.oyvey.features.modules.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.DataLine.Info;
import javax.sound.sampled.FloatControl.Type;
import me.alpha432.oyvey.features.gui.SrMusicScreen;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.settings.Setting;
import net.fabricmc.loader.api.FabricLoader;

public class SrMusicModule extends Module {
   public static volatile SrMusicModule INSTANCE;
   public final Setting<String> ytdlpPath = this.str("YtdlpPath", "yt-dlp");
   public final Setting<String> ffmpegPath = this.str("FfmpegPath", "ffmpeg");
   public final Setting<Float> volumeSetting = this.num("Volume", 0.7F, 0.0F, 1.0F);
   public final Setting<Boolean> shuffleSetting = this.bool("Shuffle", false);
   public final Setting<Boolean> repeatSetting = this.bool("Repeat", false);
   private volatile SrMusicModule.CheckStatus checkStatus = SrMusicModule.CheckStatus.UNCHECKED;
   private volatile String checkDetail = "";
   private volatile SrMusicModule.State state = SrMusicModule.State.IDLE;
   private volatile String nowTitle = "Nothing playing";
   private volatile String nowProgress = "0:00 / 0:00";
   private volatile float progressPct = 0.0F;
   private volatile String errorMsg = "";
   private final CopyOnWriteArrayList<String> queueInputs = new CopyOnWriteArrayList<>();
   private final CopyOnWriteArrayList<String> queueTitles = new CopyOnWriteArrayList<>();
   private volatile int queueIdx = 0;
   private volatile SourceDataLine audioLine = null;
   private volatile Process ffmpegProc = null;
   private volatile boolean stopFlag = false;
   private volatile boolean pauseFlag = false;
   private volatile float appliedVol = 0.7F;
   private final AtomicInteger generation = new AtomicInteger(0);
   private final AtomicLong framesPlayed = new AtomicLong(0L);
   private volatile long totalFrames = -1L;
   private volatile float sampleRate = 44100.0F;
   private volatile Process smtcProcess = null;
   private volatile Path smtcInfoFile = null;
   private static final String SMTC_SCRIPT = "$ErrorActionPreference = 'SilentlyContinue'\ntry {\n    Add-Type -AssemblyName System.Runtime.WindowsRuntime\n    $null = [Windows.Media.Playback.MediaPlayer,Windows.Media.Playback,ContentType=WindowsRuntime]\n    $player = New-Object Windows.Media.Playback.MediaPlayer\n    $smtc   = $player.SystemMediaTransportControls\n    $smtc.IsEnabled       = $true\n    $smtc.IsPlayEnabled   = $true\n    $smtc.IsPauseEnabled  = $true\n    $smtc.IsNextEnabled   = $true\n    $smtc.IsPreviousEnabled = $true\n    $smtc.PlaybackStatus  = [Windows.Media.MediaPlaybackStatus]::Playing\n    $upd  = $smtc.DisplayUpdater\n    $upd.Type             = [Windows.Media.MediaPlaybackType]::Music\n    $upd.MusicProperties.Title  = 'SR Music'\n    $upd.MusicProperties.Artist = 'SR Client'\n    $upd.Update()\n    $f    = $args[0]\n    $last = ''\n    while (Test-Path $f) {\n        $c = [IO.File]::ReadAllText($f)\n        if ($c -ne $last) {\n            $last  = $c\n            $lines = $c -split \"\\n\"\n            $upd.MusicProperties.Title = if ($lines[0]) { $lines[0] } else { 'SR Music' }\n            $upd.Update()\n            $smtc.PlaybackStatus = if ($lines.Count -gt 1 -and $lines[1].Trim() -eq 'paused') {\n                [Windows.Media.MediaPlaybackStatus]::Paused\n            } else {\n                [Windows.Media.MediaPlaybackStatus]::Playing\n            }\n        }\n        Start-Sleep -Milliseconds 400\n    }\n    $smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Stopped\n    $upd.ClearAll(); $upd.Update()\n} catch { }\n";

   public SrMusicModule() {
      super("SrMusic", "YouTube music player powered by yt-dlp + ffmpeg.", Module.Category.CLIENT);
      INSTANCE = this;
   }

   private void startSmtc(String title) {
      try {
         this.smtcInfoFile = Path.of(System.getProperty("java.io.tmpdir"), "sr_music_now.txt");
         Files.writeString(this.smtcInfoFile, title + "\nplaying\n");
         Path scriptPath = binDir().resolve("smtc.ps1");
         Files.writeString(
            scriptPath,
            "$ErrorActionPreference = 'SilentlyContinue'\ntry {\n    Add-Type -AssemblyName System.Runtime.WindowsRuntime\n    $null = [Windows.Media.Playback.MediaPlayer,Windows.Media.Playback,ContentType=WindowsRuntime]\n    $player = New-Object Windows.Media.Playback.MediaPlayer\n    $smtc   = $player.SystemMediaTransportControls\n    $smtc.IsEnabled       = $true\n    $smtc.IsPlayEnabled   = $true\n    $smtc.IsPauseEnabled  = $true\n    $smtc.IsNextEnabled   = $true\n    $smtc.IsPreviousEnabled = $true\n    $smtc.PlaybackStatus  = [Windows.Media.MediaPlaybackStatus]::Playing\n    $upd  = $smtc.DisplayUpdater\n    $upd.Type             = [Windows.Media.MediaPlaybackType]::Music\n    $upd.MusicProperties.Title  = 'SR Music'\n    $upd.MusicProperties.Artist = 'SR Client'\n    $upd.Update()\n    $f    = $args[0]\n    $last = ''\n    while (Test-Path $f) {\n        $c = [IO.File]::ReadAllText($f)\n        if ($c -ne $last) {\n            $last  = $c\n            $lines = $c -split \"\\n\"\n            $upd.MusicProperties.Title = if ($lines[0]) { $lines[0] } else { 'SR Music' }\n            $upd.Update()\n            $smtc.PlaybackStatus = if ($lines.Count -gt 1 -and $lines[1].Trim() -eq 'paused') {\n                [Windows.Media.MediaPlaybackStatus]::Paused\n            } else {\n                [Windows.Media.MediaPlaybackStatus]::Playing\n            }\n        }\n        Start-Sleep -Milliseconds 400\n    }\n    $smtc.PlaybackStatus = [Windows.Media.MediaPlaybackStatus]::Stopped\n    $upd.ClearAll(); $upd.Update()\n} catch { }\n"
         );
         this.smtcProcess = new ProcessBuilder(
               "powershell", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", scriptPath.toString(), this.smtcInfoFile.toString()
            )
            .start();
         Thread drain = new Thread(() -> {
            try {
               this.smtcProcess.getInputStream().transferTo(OutputStream.nullOutputStream());
               this.smtcProcess.getErrorStream().transferTo(OutputStream.nullOutputStream());
            } catch (Exception var2x) {
            }
         }, "smtc-drain");
         drain.setDaemon(true);
         drain.start();
      } catch (Exception var4) {
      }
   }

   public void updateSmtc(String title, String status) {
      Path f = this.smtcInfoFile;
      if (f != null) {
         try {
            Files.writeString(f, title + "\n" + status + "\n");
         } catch (Exception var5) {
         }
      }
   }

   private void stopSmtc() {
      Path f = this.smtcInfoFile;
      if (f != null) {
         try {
            Files.deleteIfExists(f);
         } catch (Exception var3) {
         }

         this.smtcInfoFile = null;
      }

      Process p = this.smtcProcess;
      if (p != null) {
         p.destroy();
         this.smtcProcess = null;
      }
   }

   @Override
   public void toggle() {
      if (!this.isEnabled() || mc.screen instanceof SrMusicScreen) {
         super.toggle();
      } else if (mc.player != null) {
         mc.setScreen(SrMusicScreen.getInstance());
      }
   }

   @Override
   public void onEnable() {
      if (this.checkStatus == SrMusicModule.CheckStatus.UNCHECKED) {
         this.runCheck();
      }

      if (mc.player != null) {
         mc.setScreen(SrMusicScreen.getInstance());
      }
   }

   @Override
   public void onDisable() {
      this.stopPlayback();
      this.stopSmtc();
      if (mc.screen instanceof SrMusicScreen) {
         mc.setScreen(null);
      }
   }

   @Override
   public void onUnload() {
      this.stopPlayback();
      this.stopSmtc();
   }

   public void runCheck() {
      if (this.checkStatus != SrMusicModule.CheckStatus.CHECKING) {
         this.checkStatus = SrMusicModule.CheckStatus.CHECKING;
         this.checkDetail = "Checking\u2026";
         Thread t = new Thread(() -> {
            try {
               Path binDir = binDir();
               Files.createDirectories(binDir);
               String ytVer = runForLine(this.ytdlpPath.getValue(), "--version");
               if (ytVer == null) {
                  Path local = binDir.resolve("yt-dlp.exe");
                  if (Files.exists(local)) {
                     this.ytdlpPath.setValue(local.toString());
                     ytVer = runForLine(local.toString(), "--version");
                  }

                  if (ytVer == null) {
                     this.checkDetail = "Downloading yt-dlp\u2026";
                     this.downloadFile("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp.exe", local);
                     this.ytdlpPath.setValue(local.toString());
                     ytVer = runForLine(local.toString(), "--version");
                  }

                  if (ytVer == null) {
                     this.checkStatus = SrMusicModule.CheckStatus.MISSING_YTDLP;
                     this.checkDetail = "yt-dlp download failed \u2014 install manually";
                     return;
                  }
               }

               String ffVer = runForLine(this.ffmpegPath.getValue(), "-version");
               if (ffVer == null) {
                  Path localx = binDir.resolve("ffmpeg.exe");
                  if (Files.exists(localx)) {
                     this.ffmpegPath.setValue(localx.toString());
                     ffVer = runForLine(localx.toString(), "-version");
                  }

                  if (ffVer == null) {
                     this.checkDetail = "Downloading ffmpeg (this may take a moment)\u2026";
                     Path zipPath = binDir.resolve("ffmpeg-tmp.zip");
                     this.downloadFile("https://github.com/yt-dlp/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip", zipPath);
                     this.checkDetail = "Extracting ffmpeg.exe\u2026";
                     extractFromZip(zipPath, "ffmpeg.exe", localx);
                     Files.deleteIfExists(zipPath);
                     this.ffmpegPath.setValue(localx.toString());
                     ffVer = runForLine(localx.toString(), "-version");
                  }

                  if (ffVer == null) {
                     this.checkStatus = SrMusicModule.CheckStatus.MISSING_FFMPEG;
                     this.checkDetail = "ffmpeg download failed \u2014 install manually";
                     return;
                  }
               }

               this.checkDetail = "Testing live fetch\u2026";
               String title = runForLine(this.ytdlpPath.getValue(), "--no-playlist", "--print", "title", "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
               if (title == null || title.isBlank()) {
                  this.checkStatus = SrMusicModule.CheckStatus.ERROR;
                  this.checkDetail = "Live fetch failed \u2014 check network";
                  return;
               }

               this.checkStatus = SrMusicModule.CheckStatus.OK;
               this.checkDetail = "yt-dlp " + ytVer.trim() + " / ffmpeg ok  \u2714 \"" + title.trim() + "\"";
            } catch (Exception var6) {
               this.checkStatus = SrMusicModule.CheckStatus.ERROR;
               this.checkDetail = "Check failed: " + var6.getMessage();
            }
         }, "sr-music-check");
         t.setDaemon(true);
         t.start();
      }
   }

   private static Path binDir() {
      return FabricLoader.getInstance().getGameDir().resolve("sr_music").resolve("bin");
   }

   private void downloadFile(String url, Path dest) throws Exception {
      for (int hop = 0; hop < 10; hop++) {
         HttpURLConnection conn = (HttpURLConnection)new URI(url).toURL().openConnection();
         conn.setInstanceFollowRedirects(false);
         conn.setConnectTimeout(15000);
         conn.setReadTimeout(60000);
         conn.setRequestProperty("User-Agent", "Mozilla/5.0 SrMusicAutoDownload/1.0");
         conn.connect();
         int code = conn.getResponseCode();
         if (code != 301 && code != 302 && code != 303 && code != 307 && code != 308) {
            long total = conn.getContentLengthLong();
            long received = 0L;
            byte[] buf = new byte[65536];

            try {
               int read;
               try (
                  InputStream in = new BufferedInputStream(conn.getInputStream());
                  OutputStream out = Files.newOutputStream(dest, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
               ) {
                  while ((read = in.read(buf)) != -1) {
                     out.write(buf, 0, read);
                     received += read;
                     if (total > 0L) {
                        int pct = (int)(received * 100L / total);
                        this.checkDetail = String.format("Downloading\u2026 %d%%  (%d / %d MB)", pct, received >> 20, total >> 20);
                     } else {
                        this.checkDetail = String.format("Downloading\u2026 %d MB", received >> 20);
                     }
                  }
               }
            } finally {
               conn.disconnect();
            }

            return;
         }

         url = conn.getHeaderField("Location");
         conn.disconnect();
      }

      throw new IOException("Too many redirects for: " + url);
   }

   private static void extractFromZip(Path zipPath, String filename, Path dest) throws IOException {
      ZipEntry entry;
      try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
         while ((entry = zis.getNextEntry()) != null) {
            String name = entry.getName().replace('\\', '/');
            if (!entry.isDirectory() && name.endsWith("/" + filename)) {
               Files.copy(zis, dest, StandardCopyOption.REPLACE_EXISTING);
               return;
            }

            zis.closeEntry();
         }
      }

      throw new IOException("Could not find " + filename + " inside zip");
   }

   public SrMusicModule.CheckStatus getCheckStatus() {
      return this.checkStatus;
   }

   public String getCheckDetail() {
      return this.checkDetail;
   }

   public void addToQueue(String raw) {
      String in = raw.trim();
      if (!in.isEmpty()) {
         String display = quickDisplayName(in);
         this.queueInputs.add(in);
         this.queueTitles.add(display);
         if (this.state == SrMusicModule.State.IDLE) {
            this.queueIdx = this.queueInputs.size() - 1;
            this.startTrack(in, display);
         }
      }
   }

   public void addPlaylist(String url) {
      this.checkDetail = "Expanding playlist\u2026";
      Thread t = new Thread(() -> {
         try {
            Process p = buildProcess(this.ytdlpPath.getValue(), "--yes-playlist", "--flat-playlist", "--print", "webpage_url", url);
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor(30L, TimeUnit.SECONDS);

            for (String line : out.split("\n")) {
               String v = line.trim();
               if (!v.isEmpty()) {
                  this.addToQueue(v);
               }
            }
         } catch (Exception var9) {
            this.errorMsg = "Playlist expand failed: " + var9.getMessage();
         }
      }, "sr-music-playlist");
      t.setDaemon(true);
      t.start();
   }

   public void play() {
      if (this.state == SrMusicModule.State.PAUSED) {
         this.pauseFlag = false;
         this.state = SrMusicModule.State.PLAYING;
      } else if (this.state == SrMusicModule.State.IDLE && !this.queueInputs.isEmpty()) {
         this.queueIdx = Math.min(this.queueIdx, this.queueInputs.size() - 1);
         this.startTrack(this.queueInputs.get(this.queueIdx), this.queueTitles.get(this.queueIdx));
      }
   }

   public void pause() {
      if (this.state == SrMusicModule.State.PLAYING) {
         this.pauseFlag = true;
         this.state = SrMusicModule.State.PAUSED;
         SourceDataLine l = this.audioLine;
         if (l != null) {
            l.stop();
         }

         this.updateSmtc(this.nowTitle, "paused");
      }
   }

   public void togglePlayPause() {
      if (this.state == SrMusicModule.State.PLAYING) {
         this.pause();
      } else {
         if (this.state == SrMusicModule.State.PAUSED && this.audioLine != null) {
            this.audioLine.start();
         }

         this.play();
         this.updateSmtc(this.nowTitle, "playing");
      }
   }

   public void next() {
      if (!this.queueInputs.isEmpty()) {
         int n = this.shuffleSetting.getValue() ? (int)(Math.random() * this.queueInputs.size()) : (this.queueIdx + 1) % this.queueInputs.size();
         this.queueIdx = n;
         this.startTrack(this.queueInputs.get(this.queueIdx), this.queueTitles.get(this.queueIdx));
      }
   }

   public void prev() {
      if (!this.queueInputs.isEmpty()) {
         this.queueIdx = (this.queueIdx - 1 + this.queueInputs.size()) % this.queueInputs.size();
         this.startTrack(this.queueInputs.get(this.queueIdx), this.queueTitles.get(this.queueIdx));
      }
   }

   public void playAt(int i) {
      if (i >= 0 && i < this.queueInputs.size()) {
         this.queueIdx = i;
         this.startTrack(this.queueInputs.get(i), this.queueTitles.get(i));
      }
   }

   public void removeAt(int i) {
      if (i >= 0 && i < this.queueInputs.size()) {
         boolean active = i == this.queueIdx;
         this.queueInputs.remove(i);
         this.queueTitles.remove(i);
         if (this.queueInputs.isEmpty()) {
            this.stopPlayback();
         } else if (active) {
            this.queueIdx = Math.min(this.queueIdx, this.queueInputs.size() - 1);
            this.startTrack(this.queueInputs.get(this.queueIdx), this.queueTitles.get(this.queueIdx));
         } else if (i < this.queueIdx) {
            this.queueIdx--;
         }
      }
   }

   public void setVolume(float vol) {
      this.appliedVol = Math.max(0.0F, Math.min(1.0F, vol));
      this.volumeSetting.setValue(this.appliedVol);
      this.applyGain();
   }

   public void stopPlayback() {
      this.generation.incrementAndGet();
      this.stopFlag = true;
      Process p = this.ffmpegProc;
      if (p != null) {
         p.destroyForcibly();
         this.ffmpegProc = null;
      }

      SourceDataLine l = this.audioLine;
      if (l != null) {
         try {
            l.stop();
            l.close();
         } catch (Exception var4) {
         }

         this.audioLine = null;
      }

      this.state = SrMusicModule.State.IDLE;
      this.nowTitle = "Nothing playing";
      this.progressPct = 0.0F;
      this.nowProgress = "0:00 / 0:00";
      this.framesPlayed.set(0L);
      this.totalFrames = -1L;
   }

   public SrMusicModule.State getState() {
      return this.state;
   }

   public String getNowTitle() {
      return this.nowTitle;
   }

   public String getNowProgress() {
      return this.nowProgress;
   }

   public float getProgressPct() {
      return this.progressPct;
   }

   public String getErrorMsg() {
      return this.errorMsg;
   }

   public int getQueueSize() {
      return this.queueInputs.size();
   }

   public int getQueueIdx() {
      return this.queueIdx;
   }

   public String getQueueTitle(int i) {
      return i < this.queueTitles.size() ? this.queueTitles.get(i) : "";
   }

   public boolean isShuffle() {
      return this.shuffleSetting.getValue();
   }

   public boolean isRepeat() {
      return this.repeatSetting.getValue();
   }

   public float getVolume() {
      return this.appliedVol;
   }

   private void startTrack(String input, String displayName) {
      this.stopPlayback();
      int myGen = this.generation.get();
      this.stopFlag = false;
      this.pauseFlag = false;
      this.appliedVol = this.volumeSetting.getValue();
      this.state = SrMusicModule.State.LOADING;
      this.nowTitle = "Loading: " + displayName;
      this.progressPct = 0.0F;
      this.nowProgress = "--:-- / --:--";
      this.framesPlayed.set(0L);
      this.totalFrames = -1L;
      Thread t = new Thread(() -> this.runTrack(input, displayName, myGen), "sr-music-play");
      t.setDaemon(true);
      t.start();
   }

   private void runTrack(String input, String displayName, int myGen) {
      try {
         String queryUrl = input.startsWith("http") ? input : "ytsearch1:" + input;
         this.nowTitle = "Resolving: " + displayName;
         String cdnUrl = this.resolveStreamUrl(queryUrl);
         if (cdnUrl == null || this.stopFlag || myGen != this.generation.get()) {
            if (myGen == this.generation.get()) {
               this.setError("yt-dlp: no stream URL returned \u2014 check yt-dlp/network");
            }

            return;
         }

         Thread metaThread = new Thread(() -> this.fetchMeta(input.startsWith("http") ? input : queryUrl), "sr-music-meta");
         metaThread.setDaemon(true);
         metaThread.start();
         if (this.stopFlag || myGen != this.generation.get()) {
            return;
         }

         Process ff = buildProcess(this.ffmpegPath.getValue(), "-loglevel", "quiet", "-i", cdnUrl, "-f", "wav", "-ar", "44100", "-ac", "2", "pipe:1");
         this.ffmpegProc = ff;
         Thread errDrain = new Thread(() -> {
            try {
               ff.getErrorStream().transferTo(OutputStream.nullOutputStream());
            } catch (IOException var2x) {
            }
         }, "sr-music-ffmpeg-err");
         errDrain.setDaemon(true);
         errDrain.start();

         AudioInputStream ais;
         try {
            ais = AudioSystem.getAudioInputStream(new BufferedInputStream(ff.getInputStream(), 131072));
         } catch (UnsupportedAudioFileException var32) {
            this.setError("AudioSystem: bad WAV from ffmpeg \u2014 is ffmpeg installed?");
            return;
         }

         AudioFormat fmt = ais.getFormat();
         if (fmt.getEncoding() != Encoding.PCM_SIGNED) {
            AudioFormat pcmFmt = new AudioFormat(
               Encoding.PCM_SIGNED, fmt.getSampleRate(), 16, fmt.getChannels(), fmt.getChannels() * 2, fmt.getSampleRate(), false
            );
            ais = AudioSystem.getAudioInputStream(pcmFmt, ais);
            fmt = pcmFmt;
         }

         this.sampleRate = fmt.getSampleRate();
         this.totalFrames = ais.getFrameLength();
         Info info = new Info(SourceDataLine.class, fmt);
         if (!AudioSystem.isLineSupported(info)) {
            this.setError("No PCM audio output available on this system");
            return;
         }

         SourceDataLine line = (SourceDataLine)AudioSystem.getLine(info);
         line.open(fmt, 65536);
         line.start();
         this.audioLine = line;
         this.applyGain();
         this.state = SrMusicModule.State.PLAYING;
         if (this.smtcProcess != null && this.smtcProcess.isAlive()) {
            this.updateSmtc(this.nowTitle, "playing");
         } else {
            this.startSmtc(this.nowTitle);
         }

         byte[] buf = new byte[8192];
         int frameSize = fmt.getFrameSize();

         while (!this.stopFlag && myGen == this.generation.get()) {
            if (this.pauseFlag) {
               Thread.sleep(40L);
            } else {
               int read = ais.read(buf);
               if (read <= 0) {
                  break;
               }

               line.write(buf, 0, read);
               this.framesPlayed.addAndGet(read / frameSize);
               this.updateProgress();
            }
         }

         if (this.stopFlag || myGen != this.generation.get()) {
            return;
         }

         line.drain();
         if (this.repeatSetting.getValue()) {
            this.startTrack(input, displayName);
            return;
         }

         if (this.queueInputs.size() > 1) {
            this.next();
         } else {
            this.state = SrMusicModule.State.IDLE;
            this.nowTitle = "Queue finished";
         }
      } catch (InterruptedException var35) {
         return;
      } catch (Exception var34) {
         if (this.stopFlag || myGen != this.generation.get()) {
            return;
         }

         this.setError(var34.getClass().getSimpleName() + ": " + var34.getMessage());
         return;
      } finally {
         SourceDataLine l = this.audioLine;
         if (l != null) {
            try {
               l.stop();
               l.close();
            } catch (Exception var33) {
            }

            this.audioLine = null;
         }

         Process p = this.ffmpegProc;
         if (p != null) {
            p.destroyForcibly();
            this.ffmpegProc = null;
         }
      }
   }

   private String resolveStreamUrl(String query) {
      String line = runForLine(
         this.ytdlpPath.getValue(), "--no-playlist", "--get-url", "--format", "bestaudio[ext=m4a]/bestaudio[ext=webm]/bestaudio/best", query
      );
      return line != null && !line.isBlank() ? line.split("\n")[0].trim() : null;
   }

   private void fetchMeta(String url) {
      try {
         Process p = buildProcess(this.ytdlpPath.getValue(), "--no-playlist", "--print", "%(title)s\t%(duration)s", url);
         String out = new String(p.getInputStream().readAllBytes()).trim();
         p.waitFor(20L, TimeUnit.SECONDS);
         if (out.isBlank()) {
            return;
         }

         String[] parts = out.split("\t");
         if (parts.length >= 1 && !parts[0].isBlank() && !this.stopFlag) {
            this.nowTitle = parts[0].trim();
            int idx = this.queueInputs.indexOf(url);
            if (idx >= 0) {
               this.queueTitles.set(idx, this.nowTitle);
            }

            this.updateSmtc(this.nowTitle, this.state == SrMusicModule.State.PAUSED ? "paused" : "playing");
         }

         if (parts.length >= 2 && !parts[1].isBlank()) {
            try {
               this.totalFrames = (long)(Double.parseDouble(parts[1].trim()) * this.sampleRate);
            } catch (NumberFormatException var6) {
            }
         }
      } catch (Exception var7) {
      }
   }

   private void updateProgress() {
      long played = this.framesPlayed.get();
      float playedSec = (float)played / this.sampleRate;
      if (this.totalFrames > 0L) {
         this.progressPct = Math.min(1.0F, (float)played / (float)this.totalFrames);
         this.nowProgress = fmtSec(playedSec) + " / " + fmtSec((float)this.totalFrames / this.sampleRate);
      } else {
         this.progressPct = 0.0F;
         this.nowProgress = fmtSec(playedSec) + " / ?:??";
      }
   }

   private void applyGain() {
      SourceDataLine l = this.audioLine;
      if (l != null) {
         try {
            if (l.isControlSupported(Type.MASTER_GAIN)) {
               FloatControl gain = (FloatControl)l.getControl(Type.MASTER_GAIN);
               float dB = this.appliedVol < 0.001F ? gain.getMinimum() : (float)(20.0 * Math.log10(this.appliedVol));
               gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
            }
         } catch (Exception var4) {
         }
      }
   }

   private void setError(String msg) {
      this.errorMsg = msg;
      this.state = SrMusicModule.State.ERROR;
      this.nowTitle = "Error \u2014 see panel";
   }

   private static String runForLine(String... cmd) {
      try {
         Process p = buildProcess(cmd);
         String out = new String(p.getInputStream().readAllBytes()).trim();
         p.waitFor(10L, TimeUnit.SECONDS);
         return out.isBlank() ? null : out.split("\n")[0].trim();
      } catch (Exception var3) {
         return null;
      }
   }

   private static Process buildProcess(String... cmd) throws IOException {
      return new ProcessBuilder(cmd).redirectErrorStream(false).start();
   }

   private static String fmtSec(float sec) {
      int s = Math.max(0, (int)sec);
      return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
   }

   private static String quickDisplayName(String in) {
      if (in.startsWith("http")) {
         if (in.contains("watch?v=")) {
            String id = in.substring(in.indexOf("watch?v=") + 8);
            if (id.contains("&")) {
               id = id.substring(0, id.indexOf("&"));
            }

            return "YT:" + id;
         } else if (!in.contains("list=") && !in.contains("/playlist")) {
            return in.length() > 36 ? in.substring(0, 36) + "\u2026" : in;
         } else {
            return "Playlist\u2026";
         }
      } else {
         return in.length() > 36 ? in.substring(0, 36) + "\u2026" : in;
      }
   }

   @Override
   public String getDisplayInfo() {
      return switch (this.state) {
         case LOADING -> "\u2026";
         case PLAYING -> "\u25b6";
         case PAUSED -> "\u23f8";
         case ERROR -> "\u2717";
         default -> null;
      };
   }

   public enum CheckStatus {
      UNCHECKED,
      CHECKING,
      OK,
      MISSING_YTDLP,
      MISSING_FFMPEG,
      ERROR;
   }

   public enum State {
      IDLE,
      LOADING,
      PLAYING,
      PAUSED,
      ERROR;
   }
}
