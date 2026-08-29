package me.alpha432.oyvey.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

public class Bind implements Util {
   private int key;

   public Bind(int key) {
      this.key = key;
   }

   public static Bind none() {
      return new Bind(-1);
   }

   public int getKey() {
      return this.key;
   }

   public void setKey(int key) {
      this.key = key;
   }

   public boolean isEmpty() {
      return this.key == -1;
   }

   @Override
   public String toString() {
      if (this.isEmpty()) {
         return "None";
      } else {
         return this.key < -1 ? "Mouse " + (-this.key - 1) : this.capitalise(InputConstants.getKey(new KeyEvent(this.key, 0, 0)).getName());
      }
   }

   public boolean isDown() {
      if (this.isEmpty()) {
         return false;
      } else {
         return this.key < -1
            ? GLFW.glfwGetMouseButton(mc.getWindow().handle(), -this.key - 2) == 1
            : GLFW.glfwGetKey(mc.getWindow().handle(), this.getKey()) == 1;
      }
   }

   private String capitalise(String str) {
      return str.isEmpty() ? "" : Character.toUpperCase(str.charAt(0)) + (str.length() != 1 ? str.substring(1).toLowerCase() : "");
   }

   public static class BindConverter extends Converter<Bind, JsonElement> {
      public JsonElement doForward(Bind bind) {
         return new JsonPrimitive(bind.toString());
      }

      public Bind doBackward(JsonElement jsonElement) {
         String s = jsonElement.getAsString();
         if (s.equalsIgnoreCase("None")) {
            return Bind.none();
         }

         if (s.toLowerCase().startsWith("mouse ")) {
            try {
               return new Bind(-Integer.parseInt(s.substring(6)) - 1);
            } catch (Exception var6) {
            }
         }

         int key = -1;

         try {
            key = InputConstants.getKey(s.toUpperCase()).getValue();
         } catch (Exception var5) {
         }

         return key == 0 ? Bind.none() : new Bind(key);
      }
   }
}
