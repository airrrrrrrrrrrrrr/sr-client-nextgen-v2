package me.alpha432.oyvey.util.traits;

import com.google.gson.JsonElement;

public interface Jsonable {
   JsonElement toJson();

   void fromJson(JsonElement var1);

   default String getFileName() {
      return "";
   }
}
