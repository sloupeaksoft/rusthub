package com.rusthud;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = RustHud.MODID, name = RustHud.MODID)
@Config.LangKey("rusthud.config.title")
public class RustHudConfig {

    @Config.LangKey("rusthud.config.anchorRight")
    @Config.Comment("true = bottom-right corner (like Rust), false = bottom-left")
    public static boolean anchorRight = true;

    @Config.LangKey("rusthud.config.offsetX")
    @Config.Comment("Horizontal offset from the screen edge")
    @Config.RangeInt(min = 0, max = 1000)
    public static int offsetX = 4;

    @Config.LangKey("rusthud.config.offsetY")
    @Config.Comment("Vertical offset from the bottom of the screen")
    @Config.RangeInt(min = 0, max = 1000)
    public static int offsetY = 4;

    @Config.LangKey("rusthud.config.barWidth")
    @Config.Comment("Total width of each bar panel")
    @Config.RangeInt(min = 60, max = 300)
    public static int barWidth = 110;

    @Config.LangKey("rusthud.config.rustStyleNumbers")
    @Config.Comment("Show values on a 0-100 scale like Rust (20 HP = 100)")
    public static boolean rustStyleNumbers = true;

    @Config.LangKey("rusthud.config.showArmor")
    @Config.Comment("Show the armor bar (replaces vanilla armor icons)")
    public static boolean showArmor = true;

    @Config.LangKey("rusthud.config.showOxygen")
    @Config.Comment("Show the oxygen bar underwater (replaces vanilla bubbles)")
    public static boolean showOxygen = true;

    @Config.LangKey("rusthud.config.showSaturation")
    @Config.Comment("Show a thin saturation strip on the food bar")
    public static boolean showSaturation = true;

    @Config.LangKey("rusthud.config.smoothAnimation")
    @Config.Comment("Smooth bar animation and damage trail")
    public static boolean smoothAnimation = true;

    @Config.LangKey("rusthud.config.backgroundOpacity")
    @Config.Comment("Panel background opacity")
    @Config.RangeDouble(min = 0.0, max = 1.0)
    public static double backgroundOpacity = 0.6;

    @Mod.EventBusSubscriber(modid = RustHud.MODID)
    private static class EventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (RustHud.MODID.equals(event.getModID())) {
                ConfigManager.sync(RustHud.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
