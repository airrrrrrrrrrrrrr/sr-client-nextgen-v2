package me.alpha432.oyvey.features.modules.render;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.event.impl.render.Render2DEvent;
import me.alpha432.oyvey.features.modules.Module;
import me.alpha432.oyvey.features.modules.client.HudModule;
import me.alpha432.oyvey.features.modules.hud.WatermarkHudModule;
import me.alpha432.oyvey.features.settings.Setting;
import me.alpha432.oyvey.util.render.HudFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MusicPlayerModule extends HudModule {
   public final Setting<Integer> refreshSecs = this.register(new Setting<>("RefreshSecs", 3, 1, 30));
   public final Setting<Boolean> showSource = this.register(new Setting<>("ShowSource", true));
   public final Setting<Boolean> matchWatermark = this.register(new Setting<>("MatchWatermark", true));
   public final Setting<Boolean> rainbow = this.register(new Setting<>("Rainbow", true));
   public final Setting<Integer> r = this.register(new Setting<>("Red", 100, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> g = this.register(new Setting<>("Green", 180, 0, 255, v -> !this.rainbow.getValue()));
   public final Setting<Integer> b = this.register(new Setting<>("Blue", 255, 0, 255, v -> !this.rainbow.getValue()));
   private volatile String nowPlaying = "No media";
   private volatile String mediaSource = "";
   private volatile boolean querying = false;
   private long lastRefresh = 0L;

   public MusicPlayerModule() {
      super("MusicPlayer", "Shows the currently playing media.", 200.0F, 14.0F, Module.Category.RENDER);
      this.rainbow.setVisibility(v -> !this.matchWatermark.getValue());
      this.r.setVisibility(v -> !this.matchWatermark.getValue() && !this.rainbow.getValue());
      this.g.setVisibility(v -> !this.matchWatermark.getValue() && !this.rainbow.getValue());
      this.b.setVisibility(v -> !this.matchWatermark.getValue() && !this.rainbow.getValue());
   }

   @Override
   public void onEnable() {
      this.triggerQuery();
   }

   @Override
   public void onDisable() {
      this.nowPlaying = "No media";
      this.mediaSource = "";
   }

   @Override
   protected void render(Render2DEvent e) {
      if (!nullCheck()) {
         long now = System.currentTimeMillis();
         if (now - this.lastRefresh > this.refreshSecs.getValue().intValue() * 1000L) {
            this.lastRefresh = now;
            this.triggerQuery();
         }

         GuiGraphicsExtractor gfx = e.getContext();
         long ms = System.currentTimeMillis();
         int accent = this.accentColor(ms);
         WatermarkHudModule watermark = OyVey.moduleManager.getModuleByClass(WatermarkHudModule.class);
         HudFont selected = this.matchWatermark.getValue() && watermark != null ? watermark.font.getValue() : HudFont.MINECRAFT;
         String note = "\u266a ";
         String title = this.nowPlaying;
         String src = this.showSource.getValue() && !this.mediaSource.isEmpty() ? "  [" + this.mediaSource + "]" : "";
         int aW = 2;
         int padX = 8;
         int padY = 6;
         int noteW = mc.font.width(selected.text(note));
         int titleW = mc.font.width(selected.text(title));
         int srcW = mc.font.width(selected.text(src));
         int totalW = aW + padX + noteW + titleW + srcW + padX;
         int totalH = 20;
         this.setWidth(totalW);
         this.setHeight(totalH);
         super.render(e);
         int px = (int)this.getX();
         int py = (int)this.getY();
         WatermarkHudModule.drawBadge(gfx, px, py, totalW, totalH);
         gfx.fill(px, py + 3, px + aW, py + totalH - 3, accent);
         int tx = px + aW + padX;
         int ty = py + padY;
         gfx.text(mc.font, selected.text(note), tx, ty, accent, true);
         gfx.text(mc.font, selected.text(title), tx + noteW, ty, 0xFFEAEAEA, true);
         gfx.text(mc.font, selected.text(src), tx + noteW + titleW, ty, 0xFF969696, false);
      }
   }

   private void triggerQuery() {
      if (!this.querying) {
         this.querying = true;
         Thread t = new Thread(() -> {
            try {
               this.queryWindowTitles();
            } finally {
               this.querying = false;
            }
         }, "music-player-query");
         t.setDaemon(true);
         t.start();
      }
   }

   private void queryWindowTitles() {
      try {
         String cmd = "Get-Process | Where-Object {$_.MainWindowTitle -ne ''} | Select-Object Name,MainWindowTitle | ConvertTo-Csv -NoTypeInformation";
         Process proc = Runtime.getRuntime().exec(new String[]{"powershell", "-NoProfile", "-NonInteractive", "-Command", cmd});
         BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
         String foundTitle = null;
         String foundSource = "";

         String line;
         while ((line = br.readLine()) != null) {
            line = line.replace("\"", "").trim();
            int comma = line.indexOf(44);
            if (comma >= 0) {
               String name = line.substring(0, comma).toLowerCase().trim();
               String title = line.substring(comma + 1).trim();
               if (!title.isEmpty()) {
                  if (name.equals("spotify") && !title.equalsIgnoreCase("spotify")) {
                     foundTitle = title;
                     foundSource = "Spotify";
                     break;
                  }

                  if (name.equals("vlc") && title.contains("VLC")) {
                     foundTitle = title.replaceAll("\\s*-\\s*VLC.*$", "").trim();
                     foundSource = "VLC";
                     break;
                  }

                  if (name.equals("tidal") && title.contains(" - ")) {
                     foundTitle = title;
                     foundSource = "Tidal";
                     break;
                  }

                  if ((name.equals("wmplayer") || name.contains("groove")) && title.contains(" - ")) {
                     foundTitle = title.replaceAll("\\s*-\\s*Windows Media Player$", "").trim();
                     foundSource = "WMP";
                     break;
                  }

                  if (name.equals("applemusic") && title.contains(" - ")) {
                     foundTitle = title;
                     foundSource = "Apple Music";
                     break;
                  }

                  if (name.equals("itunes") && title.contains(" - ")) {
                     foundTitle = title;
                     foundSource = "iTunes";
                     break;
                  }

                  if (name.contains("foobar") && title.contains(" - ")) {
                     foundTitle = title.replaceAll("\\s*\\[foobar.*?\\]\\s*$", "").trim();
                     foundSource = "foobar2000";
                     break;
                  }

                  if (name.equals("aimp") && title.contains(" - ")) {
                     foundTitle = title.replaceAll("\\s*-\\s*AIMP\\s*$", "").trim();
                     foundSource = "AIMP";
                     break;
                  }

                  if (name.equals("musicbee") && title.contains(" - ")) {
                     foundTitle = title.replaceAll("\\s*-\\s*MusicBee\\s*$", "").trim();
                     foundSource = "MusicBee";
                     break;
                  }

                  boolean isBrowser = name.equals("chrome")
                     || name.equals("msedge")
                     || name.equals("firefox")
                     || name.equals("opera")
                     || name.equals("operagx")
                     || name.contains("brave")
                     || name.equals("vivaldi")
                     || name.equals("arc");
                  if (isBrowser) {
                     String stripped = this.stripBrowser(title);
                     if (title.contains("YouTube Music")) {
                        String clean = stripped.replaceAll("\\s*-\\s*YouTube Music$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "YT Music";
                           break;
                        }
                     }

                     if (title.contains("YouTube")) {
                        String clean = stripped.replaceAll("\\s*-\\s*YouTube$", "").trim();
                        if (!clean.isEmpty() && !clean.equalsIgnoreCase("YouTube")) {
                           foundTitle = clean;
                           foundSource = "YouTube";
                           break;
                        }
                     }

                     if (title.contains("Spotify")) {
                        String clean = stripped.replaceAll("\\s*[-\u2013]\\s*Spotify.*$", "").trim();
                        if (!clean.isEmpty() && !clean.equalsIgnoreCase("Spotify")) {
                           foundTitle = clean;
                           foundSource = "Spotify Web";
                           break;
                        }
                     }

                     if (title.contains("SoundCloud")) {
                        String clean = stripped.replaceAll("\\s*\\|\\s*SoundCloud$", "").replaceAll("\\s*-\\s*SoundCloud$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "SoundCloud";
                           break;
                        }
                     }

                     if (title.contains("Apple Music")) {
                        String noSuffix = stripped.replaceAll("\\s*[-\u2013\u2014\u2012]\\s*Apple Music(\\s*-\\s*Web Player)?$", "").trim();
                        boolean isDefault = noSuffix.isEmpty() || noSuffix.equalsIgnoreCase("Apple Music") || noSuffix.equalsIgnoreCase("Web Player");
                        if (!isDefault) {
                           foundTitle = noSuffix;
                           foundSource = "Apple Music";
                           break;
                        }
                     }

                     if (title.contains("Deezer")) {
                        String clean = stripped.replaceAll("\\s*-\\s*Deezer$", "").trim();
                        if (!clean.isEmpty() && !clean.equalsIgnoreCase("Deezer")) {
                           foundTitle = clean;
                           foundSource = "Deezer";
                           break;
                        }
                     }

                     if (title.contains("TIDAL") || title.contains("Tidal")) {
                        String clean = stripped.replaceAll("\\s*[-|]\\s*TIDAL.*$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "Tidal";
                           break;
                        }
                     }

                     if (title.contains("Twitch")) {
                        String clean = stripped.replaceAll("\\s*-\\s*Twitch$", "").trim();
                        if (!clean.isEmpty() && !clean.equalsIgnoreCase("Twitch")) {
                           foundTitle = clean;
                           foundSource = "Twitch";
                           break;
                        }
                     }

                     if (title.contains("Netflix")) {
                        String clean = stripped.replaceAll("\\s*-\\s*Netflix$", "").trim();
                        if (!clean.isEmpty() && !clean.equalsIgnoreCase("Netflix")) {
                           foundTitle = clean;
                           foundSource = "Netflix";
                           break;
                        }
                     }

                     if (title.contains("Amazon Music")) {
                        String clean = stripped.replaceAll("\\s*-\\s*Amazon Music$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "Amazon Music";
                           break;
                        }
                     }

                     if (title.contains("Prime Video")) {
                        String clean = stripped.replaceAll("\\s*-\\s*Amazon Prime Video$", "").replaceAll("\\s*-\\s*Prime Video$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "Prime Video";
                           break;
                        }
                     }

                     if (title.contains("Bandcamp")) {
                        String clean = stripped.replaceAll("\\s*\\|\\s*Bandcamp$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "Bandcamp";
                           break;
                        }
                     }

                     if (title.contains("Disney+") || title.contains("Disney Plus")) {
                        String clean = stripped.replaceAll("\\s*\\|\\s*Disney\\+$", "").trim();
                        if (!clean.isEmpty()) {
                           foundTitle = clean;
                           foundSource = "Disney+";
                           break;
                        }
                     }
                  }
               }
            }
         }

         proc.destroy();
         this.nowPlaying = foundTitle != null ? foundTitle : "No media playing";
         this.mediaSource = foundTitle != null ? foundSource : "";
      } catch (Exception var14) {
         this.nowPlaying = "No media playing";
         this.mediaSource = "";
      }
   }

   private String stripBrowser(String title) {
      return title.replaceAll("\\s*-\\s*Google Chrome$", "")
         .replaceAll("\\s*-\\s*Mozilla Firefox$", "")
         .replaceAll("\\s*-\\s*Microsoft Edge$", "")
         .replaceAll("\\s*-\\s*Opera GX$", "")
         .replaceAll("\\s*-\\s*Opera$", "")
         .replaceAll("\\s*-\\s*Brave$", "")
         .replaceAll("\\s*-\\s*Vivaldi$", "")
         .replaceAll("\\s*-\\s*Arc$", "")
         .trim();
   }

   private int accentColor(long now) {
      if (this.matchWatermark.getValue() && OyVey.moduleManager != null) {
         WatermarkHudModule watermark = OyVey.moduleManager.getModuleByClass(WatermarkHudModule.class);
         return 0xFF000000 | (watermark == null ? 0x69CDFF : watermark.getAccentColor(now));
      }
      if (this.rainbow.getValue()) {
         float hue = (float)(now % 3600L) / 3600.0F;
         return Color.getHSBColor(hue, 1.0F, 1.0F).getRGB() | 0xFF000000;
      } else {
         return new Color(this.r.getValue(), this.g.getValue(), this.b.getValue()).getRGB() | 0xFF000000;
      }
   }
}
