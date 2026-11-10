package dev.frostclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The config is owned by the client only, with no server packets. */
public final class FrostConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String[] MODULES = {
        "Health Indicators", "Hitbox Viewer", "Reach Display", "Reach Circle", "Shield Status Indicators",
        "Target HUD", "Target Potion Effects", "CPS Counter", "Ping Counter", "Blocks Per Second",
        "Sprint Status", "Low Health Warning", "Low Armor Warning", "Armor Warning Sound",
        "Potion Effects Overlay", "Potion Expiry Warning", "Chat Coordinate Detection",
        "Waypoints HUD", "World Waypoint Markers", "Snowflake Crosshair",
        "Accurate Block Placement", "Placement Highlight", "Server Scoreboard"
    };
    public Map<String,Boolean> modules = new LinkedHashMap<>();
    public Map<String,HudPosition> positions = new LinkedHashMap<>();
    public Map<String,Integer> hudScales = new LinkedHashMap<>(); // 50..200% for movable HUD components
    public List<Waypoint> waypoints = new ArrayList<>();
    public double lowHealthHearts = 5.0;
    public int armorDurabilityThreshold = 65;
    public int potionExpirySeconds = 10;
    public int sprintDisplayMode = 0; // 0 text, 1 icon
    public int potionOverlaySize = 30; // potion tile size in GUI pixels: 26 - 52
    public int waypointMarkerSize = 100; // fixed screen-space percent: 50 - 200
    public String placementHighlightColor = "#F4ED73";
    public int placementHighlightThickness = 2; // outline thickness in GUI pixels: 1 - 6
    public String lastSelectedTab = "Modules";

    public static final class HudPosition {
        public int x, y;
        public HudPosition() {}
        public HudPosition(int x, int y) { this.x=x; this.y=y; }
    }
    public static final class Waypoint {
        public String name, dimension, color;
        public int x,y,z;
        public Waypoint() {}
        public Waypoint(String name, String dimension, int x, int y, int z, String color) {
            this.name=name;this.dimension=dimension;this.x=x;this.y=y;this.z=z;this.color=color;
        }
    }
    public boolean enabled(String key) { return modules.getOrDefault(key, false); }
    public void toggle(String key) { modules.put(key, !enabled(key)); save(); }
    public HudPosition pos(String key) { return positions.computeIfAbsent(key, k->new HudPosition(8,8)); }
    public void move(String key,int x,int y) { HudPosition p=pos(key);p.x=x;p.y=y;save(); }
    public int hudScale(String key) { return Math.max(50, Math.min(200, hudScales.getOrDefault(key,100))); }
    public void setHudScale(String key,int percent){ hudScales.put(key,Math.max(50,Math.min(200,percent)));save(); }
    public void save() {
        try {
            Path file=path();Files.createDirectories(file.getParent());
            Files.writeString(file,GSON.toJson(this),StandardCharsets.UTF_8);
        } catch(Exception e) { System.err.println("[Frost Client] Unable to save settings: "+e.getMessage()); }
    }
    public static FrostConfig load() {
        FrostConfig cfg = new FrostConfig();
        try { if(Files.exists(path())) cfg = GSON.fromJson(Files.readString(path(),StandardCharsets.UTF_8), FrostConfig.class); }
        catch(Exception e) { System.err.println("[Frost Client] Unable to load settings: "+e.getMessage()); }
        if (cfg==null) cfg=new FrostConfig();
        if (cfg.modules==null) cfg.modules=new LinkedHashMap<>();
        if (cfg.positions==null) cfg.positions=new LinkedHashMap<>();
        if (cfg.hudScales==null) cfg.hudScales=new LinkedHashMap<>();
        if (cfg.waypoints==null) cfg.waypoints=new ArrayList<>();
        for(String key:MODULES) cfg.modules.putIfAbsent(key, !key.equals("Hitbox Viewer") && !key.equals("Health Indicators") && !key.equals("Accurate Block Placement"));
        cfg.positions.putIfAbsent("CPS Counter",new HudPosition(8,8));
        cfg.positions.putIfAbsent("Ping Counter",new HudPosition(8,26));
        cfg.positions.putIfAbsent("Blocks Per Second",new HudPosition(8,44));
        cfg.positions.putIfAbsent("Reach Display",new HudPosition(8,62));
        cfg.positions.putIfAbsent("Sprint Status",new HudPosition(0,-50));
        cfg.positions.putIfAbsent("Potion Effects Overlay",new HudPosition(8,87));
        cfg.positions.putIfAbsent("Waypoints HUD",new HudPosition(8,195));
        cfg.lowHealthHearts=Math.max(0.5,Math.min(20,cfg.lowHealthHearts));
        cfg.armorDurabilityThreshold=Math.max(1,Math.min(1000,cfg.armorDurabilityThreshold));
        cfg.potionExpirySeconds=Math.max(1,Math.min(120,cfg.potionExpirySeconds));
        cfg.potionOverlaySize=Math.max(26,Math.min(52,cfg.potionOverlaySize));
        cfg.waypointMarkerSize=Math.max(50,Math.min(200,cfg.waypointMarkerSize));
        if(cfg.placementHighlightColor==null || cfg.placementHighlightColor.isBlank()) cfg.placementHighlightColor="#F4ED73";
        cfg.placementHighlightThickness=Math.max(1,Math.min(6,cfg.placementHighlightThickness));
        cfg.hudScales.replaceAll((key,val)->Math.max(50,Math.min(200,val==null?100:val)));
        return cfg;
    }
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("frostclient.json"); }
}
