package me.alpha432.oyvey.util;

import java.util.HashMap;
import java.util.Map;
import me.alpha432.oyvey.OyVey;
import me.alpha432.oyvey.manager.PreferencesManager;

public final class Lang {
   private static final Map<String, String> EN = new HashMap<>();
   private static final Map<String, String> ES = new HashMap<>();
   private static final Map<String, String> DE = new HashMap<>();

   private static void add(String key, String en, String es, String de) {
      EN.put(key, en);
      ES.put(key, es);
      DE.put(key, de);
   }

   public static String get(String key) {
      if (OyVey.preferences != null) {
         PreferencesManager.Language lang = OyVey.preferences.getLanguage();
         if (lang == PreferencesManager.Language.SPANISH) {
            return ES.getOrDefault(key, EN.getOrDefault(key, key));
         }

         if (lang == PreferencesManager.Language.GERMAN) {
            return DE.getOrDefault(key, EN.getOrDefault(key, key));
         }
      }

      return EN.getOrDefault(key, key);
   }

   private Lang() {
   }

   static {
      add("welcome.step1", "Step 1 of 3  \u2014  Language", "Paso 1 de 3  \u2014  Idioma", "Schritt 1 von 3  \u2014  Sprache");
      add("welcome.step2", "Step 2 of 3  \u2014  Color", "Paso 2 de 3  \u2014  Color", "Schritt 2 von 3  \u2014  Farbe");
      add("welcome.step3", "Step 3 of 3  \u2014  Server", "Paso 3 de 3  \u2014  Servidor", "Schritt 3 von 3  \u2014  Server");
      add("welcome.lang.title", "Choose your language", "Elige tu idioma", "Waehle deine Sprache");
      add("welcome.color.title", "Choose your color theme", "Elige tu color favorito", "Waehle dein Farbthema");
      add(
         "welcome.server.title",
         "What server will you mainly play on?",
         "\u00bfEn qu\u00e9 servidor jugar\u00e1s principalmente?",
         "Auf welchem Server spielst du hauptsaechlich?"
      );
      add("welcome.next", "Continue", "Continuar", "Weiter");
      add("welcome.done", "Get Started", "Empezar", "Loslegen");
      add("menu.singleplayer", "Singleplayer", "Un jugador", "Einzelspieler");
      add("menu.multiplayer", "Multiplayer", "Multijugador", "Mehrspieler");
      add("menu.accounts", "Accounts", "Cuentas", "Konten");
      add("menu.capes", "Capes", "Capas", "Umhaenge");
      add("menu.options", "Options", "Opciones", "Optionen");
      add("menu.proxies", "Proxies", "Proxies", "Proxys");
      add("menu.description.multiplayer", "Play on a server", "Juega en un servidor", "Auf einem Server spielen");
      add("menu.description.singleplayer", "Your saved worlds", "Tus mundos guardados", "Deine gespeicherten Welten");
      add("menu.setup", "Setup on launch", "Configurar al iniciar", "Setup beim Start");
      add("proxy.save", "Save", "Guardar", "Speichern");
      add("proxy.update", "Update", "Actualizar", "Aktualisieren");
      add("proxy.test", "Test", "Probar", "Testen");
      add("proxy.setactive", "Set Active", "Activar", "Aktivieren");
      add("proxy.delete", "Delete", "Eliminar", "Loeschen");
      add("proxy.new", "New", "Nuevo", "Neu");
      add("proxy.disable", "Disable Proxy", "Desactivar Proxy", "Proxy aus");
      add("proxy.back", "Back", "Atras", "Zurueck");
      add("menu.quit", "Quit", "Salir", "Beenden");
      add("menu.hint.explore", "explore", "explorar", "erkunden");
      add("menu.hint.bypass", "bypass", "bypass", "bypass");
      add("menu.hint.alts", "alts", "cuentas", "konten");
      add("menu.hint.custom", "custom", "personalizado", "angepasst");
      add("menu.hint.cya", "cya", "adios", "tschuess");
      add("menu.hint.route", "route", "ruta", "leiten");
      add("menu.vanilla", "Vanilla menu", "Men\u00fa vainilla", "Vanilla-Menue");
      add("menu.preferences", "Preferences", "Preferencias", "Einstellungen");
      add("color.BLACK", "Black my loves favorite colour bannabellpepper", "Negro(aka black people", "Schwarz");
      add("color.WHITE", "White", "Blanco", "Weiss");
      add("color.RED", "Red", "Rojo", "Rot");
      add("color.ORANGE", "Orange", "Naranja", "Orange");
      add("color.YELLOW", "Yellow", "Amarillo", "Gelb");
      add("color.GREEN", "Green", "Verde", "Gruen");
      add("color.CYAN", "Cyan", "Cian", "Cyan");
      add("color.BLUE", "Blue", "Azul", "Blau");
      add("color.PURPLE", "Purple", "Morado", "Lila");
      add("color.PINK", "Pink", "Rosa", "Pink");
      add("color.TEAL", "Teal", "Turquesa", "Blaugruen");
      add("color.LIME", "Lime", "Lima", "Limette");
      add("server.DEATHZONE", "DeathZone", "DeathZone", "DeathZone");
      add("server.HYPIXEL", "Hypixel", "Hypixel", "Hypixel");
      add("server.OTHER", "Other", "Otro", "Andere");
      add("server.NONE", "Other", "Otro", "Andere");
   }
}
