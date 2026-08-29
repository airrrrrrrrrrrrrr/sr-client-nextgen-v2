package me.alpha432.oyvey.features.script;

public class LuaScriptSetting {
   private final String name;
   private final LuaScriptSetting.Type type;
   private Object value;
   private final double min;
   private final double max;
   private final String[] options;

   public LuaScriptSetting(String name, boolean def) {
      this.name = name;
      this.type = LuaScriptSetting.Type.BOOLEAN;
      this.value = def;
      this.min = 0.0;
      this.max = 1.0;
      this.options = null;
   }

   public LuaScriptSetting(String name, double def, double min, double max) {
      this.name = name;
      this.type = LuaScriptSetting.Type.SLIDER;
      this.value = Math.max(min, Math.min(max, def));
      this.min = min;
      this.max = max;
      this.options = null;
   }

   public LuaScriptSetting(String name, String[] options, int defaultIndex) {
      this.name = name;
      this.type = LuaScriptSetting.Type.ENUM;
      int idx = Math.max(0, Math.min(options.length - 1, defaultIndex));
      this.value = options[idx];
      this.min = 0.0;
      this.max = options.length - 1;
      this.options = options;
   }

   public String getName() {
      return this.name;
   }

   public LuaScriptSetting.Type getType() {
      return this.type;
   }

   public Object getValue() {
      return this.value;
   }

   public double getMin() {
      return this.min;
   }

   public double getMax() {
      return this.max;
   }

   public String[] getOptions() {
      return this.options;
   }

   public boolean getBoolean() {
      return (Boolean)this.value;
   }

   public double getDouble() {
      return ((Number)this.value).doubleValue();
   }

   public String getString() {
      return (String)this.value;
   }

   public void setBoolValue(boolean v) {
      this.value = v;
   }

   public void setDoubleValue(double v) {
      this.value = Math.max(this.min, Math.min(this.max, v));
   }

   public void setStringValue(String v) {
      this.value = v;
   }

   public void cycleEnum() {
      if (this.type == LuaScriptSetting.Type.ENUM && this.options != null) {
         int cur = 0;

         for (int i = 0; i < this.options.length; i++) {
            if (this.options[i].equals(this.value)) {
               cur = i;
               break;
            }
         }

         this.value = this.options[(cur + 1) % this.options.length];
      }
   }

   public enum Type {
      BOOLEAN,
      SLIDER,
      ENUM;
   }
}
