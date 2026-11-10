package com.rusthud;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = RustHud.MODID, name = RustHud.NAME, version = RustHud.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.12.2]")
public class RustHud {
    public static final String MODID = "rusthud";
    public static final String NAME = "Rust HUD";
    public static final String VERSION = "1.0.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new HudRenderer());
    }
}
