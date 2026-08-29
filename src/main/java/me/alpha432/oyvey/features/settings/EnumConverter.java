package me.alpha432.oyvey.features.settings;

import com.google.common.base.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class EnumConverter<T extends Enum<T>> extends Converter<T, JsonElement> {
   private final Class<T> clazz;

   public EnumConverter(Class<T> clazz) {
      this.clazz = clazz;
   }

   public static <T extends Enum<?>> int currentEnum(T value) {
      Object[] constants = value.getDeclaringClass().getEnumConstants();

      for (int i = 0; i < constants.length; i++) {
         if (((Enum)constants[i]).name().equalsIgnoreCase(value.name())) {
            return i;
         }
      }

      return -1;
   }

   public static <T extends Enum<?>> T increaseEnum(T value) {
      Object[] constants = value.getDeclaringClass().getEnumConstants();
      int next = currentEnum(value) + 1;
      if (next >= constants.length) {
         next = 0;
      }

      return (T)constants[next];
   }

   public static <T extends Enum<?>> String getProperName(T clazz) {
      return Character.toUpperCase(clazz.name().charAt(0)) + clazz.name().toLowerCase().substring(1);
   }

   public JsonElement doForward(Enum anEnum) {
      return new JsonPrimitive(anEnum.toString());
   }

   public T doBackward(JsonElement jsonElement) {
      try {
         return Enum.valueOf(this.clazz, jsonElement.getAsString());
      } catch (IllegalArgumentException var3) {
         return null;
      }
   }
}
