package me.alpha432.oyvey.manager;

import com.google.gson.*;
import java.util.*;
import me.alpha432.oyvey.util.traits.Jsonable;

/** Named physical-click intervals, persisted without rounding or CPS randomization. */
public final class RecordedClickManager implements Jsonable {
   public static final int MAX_INTERVALS = 20000;
   public static final long MAX_INTERVAL_NANOS = 600_000_000_000L;
   private final Map<String,List<Long>> patterns = new LinkedHashMap<>();
   private final List<Long> pending = new ArrayList<>();
   private String selected = "";
   private boolean recording;
   private long lastClick;
   private boolean anchored;

   public synchronized boolean start() {
      if(recording) return false;
      pending.clear(); recording=true; anchored=false;
      return true;
   }
   public synchronized void pause() { anchored=false; }
   public synchronized void cancel() { recording=false; anchored=false; pending.clear(); }
   public synchronized boolean isRecording() { return recording; }
   public synchronized int capturedIntervals() { return pending.size(); }
   public synchronized void click(long now) {
      if(!recording || pending.size()>=MAX_INTERVALS) return;
      if(anchored) {
         long interval=now-lastClick;
         if(interval>=0 && interval<=MAX_INTERVAL_NANOS) pending.add(interval);
      }
      lastClick=now; anchored=true;
   }
   public synchronized int stop(String name) {
      name=normalizeName(name);
      if(!recording || !valid(pending)) return -1;
      patterns.put(name,List.copyOf(pending)); selected=name;
      int count=pending.size()+1;
      cancel();
      return count;
   }
   public synchronized boolean use(String name) {
      name=normalizeName(name);
      if(!patterns.containsKey(name)) return false;
      selected=name; return true;
   }
   public synchronized String selectedName() { return selected; }
   public synchronized List<Long> pattern() { return patterns.getOrDefault(selected,List.of()); }
   public synchronized List<String> names() { return List.copyOf(patterns.keySet()); }
   public static String normalizeName(String name) {
      if(name==null || !name.matches("[A-Za-z0-9_-]{1,48}"))
         throw new IllegalArgumentException("Use 1-48 letters, digits, underscores or dashes for the pattern name.");
      return name.toLowerCase(Locale.ROOT);
   }
   private static boolean valid(List<Long> intervals) {
      return !intervals.isEmpty() && intervals.size()<=MAX_INTERVALS
         && intervals.stream().allMatch(v->v!=null && v>=0 && v<=MAX_INTERVAL_NANOS)
         && intervals.stream().anyMatch(v->v>0);
   }
   @Override public synchronized JsonElement toJson() {
      JsonObject root=new JsonObject(), saved=new JsonObject();
      root.addProperty("version",1); root.addProperty("selected",selected);
      patterns.forEach((name,intervals)-> {
         JsonArray values=new JsonArray(); intervals.forEach(values::add);
         saved.add(name,values);
      });
      root.add("patterns",saved); return root;
   }
   @Override public synchronized void fromJson(JsonElement json) {
      if(json==null || !json.isJsonObject()) return;
      JsonObject root=json.getAsJsonObject();
      if(!root.has("patterns") || !root.get("patterns").isJsonObject()) return;
      patterns.clear(); cancel();
      for(var entry:root.getAsJsonObject("patterns").entrySet()) {
         try {
            String name=normalizeName(entry.getKey());
            JsonArray array=entry.getValue().getAsJsonArray();
            if(array.size()>MAX_INTERVALS) continue;
            List<Long> intervals=new ArrayList<>();
            for(JsonElement value:array) intervals.add(value.getAsBigDecimal().longValueExact());
            if(valid(intervals)) patterns.put(name,List.copyOf(intervals));
         } catch(RuntimeException ignored) { }
      }
      selected="";
      try {
         if(root.has("selected")) {
            String name=normalizeName(root.get("selected").getAsString());
            if(patterns.containsKey(name)) selected=name;
         }
      } catch(RuntimeException ignored) { }
      if(selected.isEmpty() && !patterns.isEmpty()) selected=patterns.keySet().iterator().next();
   }
   @Override public String getFileName() { return "recorded-clicks.json"; }
}
