package com.rusthud;

import java.util.Locale;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Rust-style HUD: stacked bars in the corner (health, food, armor, oxygen).
 * No thirst bar.
 */
@SideOnly(Side.CLIENT)
public class HudRenderer {

    private static final int ROW_HEIGHT = 14;
    private static final int ROW_GAP = 2;

    // Rust-like palette
    private static final int COLOR_HEALTH      = 0xFF7FA23A;
    private static final int COLOR_HEALTH_LOW  = 0xFFB8322A;
    private static final int COLOR_HEALTH_LOW2 = 0xFFE5534B;
    private static final int COLOR_POISON      = 0xFF9AA61E;
    private static final int COLOR_WITHER      = 0xFF4A4A4A;
    private static final int COLOR_DAMAGE_TRAIL= 0xCCE8DCC0;
    private static final int COLOR_ABSORPTION  = 0xFFE8C547;
    private static final int COLOR_FOOD        = 0xFFCF7A2B;
    private static final int COLOR_FOOD_HUNGER = 0xFF6E8A2A;
    private static final int COLOR_SATURATION  = 0xFFF5DC6E;
    private static final int COLOR_ARMOR       = 0xFF8EA2B6;
    private static final int COLOR_OXYGEN      = 0xFF4A9BD6;
    private static final int COLOR_TEXT        = 0xFFFFFF;
    private static final int COLOR_TEXT_DIM    = 0xD0D0D0;

    private final Minecraft mc = Minecraft.getMinecraft();

    private EntityPlayer trackedPlayer;
    private float shownHealth, lagHealth, prevHealth, shownFood, shownArmor, shownAir;
    private long lastFrame, lastDamage;

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderPre(RenderGameOverlayEvent.Pre event) {
        ElementType type = event.getType();
        if (type == ElementType.HEALTH
                || type == ElementType.FOOD
                || (type == ElementType.ARMOR && RustHudConfig.showArmor)
                || (type == ElementType.AIR && RustHudConfig.showOxygen)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onRenderPost(RenderGameOverlayEvent.Post event) {
        if (event.getType() != ElementType.ALL) return;
        EntityPlayer player = mc.player;
        if (player == null || mc.gameSettings.hideGUI) return;
        if (mc.playerController == null || !mc.playerController.shouldDrawHUD()) return; // creative / spectator
        renderHud(event.getResolution(), player);
    }

    private void renderHud(ScaledResolution res, EntityPlayer p) {
        long now = Minecraft.getSystemTime();
        float dt = lastFrame == 0L ? 1f : MathHelper.clamp((now - lastFrame) / 1000f, 0f, 0.25f);
        lastFrame = now;

        float maxHealth = Math.max(p.getMaxHealth(), 1f);
        float health = MathHelper.clamp(p.getHealth(), 0f, maxHealth);
        float absorption = Math.max(p.getAbsorptionAmount(), 0f);
        int food = p.getFoodStats().getFoodLevel();
        float saturation = p.getFoodStats().getSaturationLevel();
        int armor = p.getTotalArmorValue();
        int air = MathHelper.clamp(p.getAir(), 0, 300);
        boolean underwater = p.isInsideOfMaterial(Material.WATER);

        if (p != trackedPlayer || !RustHudConfig.smoothAnimation) {
            trackedPlayer = p;
            shownHealth = lagHealth = prevHealth = health;
            shownFood = food;
            shownArmor = armor;
            shownAir = air;
        } else {
            if (health < prevHealth) lastDamage = now;
            prevHealth = health;
            shownHealth = approach(shownHealth, health, dt * 14f);
            shownFood = approach(shownFood, food, dt * 10f);
            shownArmor = approach(shownArmor, armor, dt * 10f);
            shownAir = approach(shownAir, air, dt * 10f);
            if (health >= lagHealth) {
                lagHealth = health;
            } else if (now - lastDamage > 450L) {
                lagHealth = Math.max(health, lagHealth - maxHealth * 0.7f * dt);
            }
        }

        int w = RustHudConfig.barWidth;
        int x = RustHudConfig.anchorRight
                ? res.getScaledWidth() - w - RustHudConfig.offsetX
                : RustHudConfig.offsetX;
        int y = res.getScaledHeight() - RustHudConfig.offsetY - ROW_HEIGHT;
        boolean rust = RustHudConfig.rustStyleNumbers;

        // ---- FOOD (bottom row) ----
        boolean hunger = p.isPotionActive(MobEffects.HUNGER);
        drawPanel(x, y, w);
        drawBar(x, y, w, shownFood / 20f, hunger ? COLOR_FOOD_HUNGER : COLOR_FOOD);
        if (RustHudConfig.showSaturation && saturation > 0f) {
            drawStrip(x, y, w, saturation / 20f, COLOR_SATURATION, false);
        }
        drawIcon(x, y, hunger ? 133 : 16, 27, hunger ? 88 : 52, 27);
        drawValue(x, y, String.valueOf(rust ? food * 5 : food));
        y -= ROW_HEIGHT + ROW_GAP;

        // ---- HEALTH ----
        int hpColor = COLOR_HEALTH;
        int heartU = 52;
        if (p.isPotionActive(MobEffects.WITHER)) {
            hpColor = COLOR_WITHER;
            heartU = 124;
        } else if (p.isPotionActive(MobEffects.POISON)) {
            hpColor = COLOR_POISON;
            heartU = 88;
        } else if (health / maxHealth <= 0.25f) {
            float pulse = (MathHelper.sin(now / 120f) + 1f) * 0.5f;
            hpColor = lerpColor(COLOR_HEALTH_LOW, COLOR_HEALTH_LOW2, pulse);
        }
        drawPanel(x, y, w);
        if (lagHealth > shownHealth) drawBar(x, y, w, lagHealth / maxHealth, COLOR_DAMAGE_TRAIL);
        drawBar(x, y, w, shownHealth / maxHealth, hpColor);
        if (absorption > 0f) drawStrip(x, y, w, absorption / maxHealth, COLOR_ABSORPTION, true);
        drawIcon(x, y, 16, 0, heartU, 0);
        drawValue(x, y, rust ? String.valueOf(MathHelper.ceil(health / maxHealth * 100f)) : formatNumber(health));
        if (absorption > 0f) {
            String extra = "+" + (rust ? String.valueOf(MathHelper.ceil(absorption / maxHealth * 100f)) : formatNumber(absorption));
            drawValueRight(x, y, w, extra, COLOR_ABSORPTION & 0xFFFFFF);
        }
        y -= ROW_HEIGHT + ROW_GAP;

        // ---- ARMOR ----
        if (RustHudConfig.showArmor && armor > 0) {
            drawPanel(x, y, w);
            drawBar(x, y, w, shownArmor / 20f, COLOR_ARMOR);
            drawIcon(x, y, -1, -1, 34, 9);
            drawValue(x, y, String.valueOf(rust ? armor * 5 : armor));
            y -= ROW_HEIGHT + ROW_GAP;
        }

        // ---- OXYGEN ----
        if (RustHudConfig.showOxygen && (underwater || air < 300)) {
            drawPanel(x, y, w);
            drawBar(x, y, w, shownAir / 300f, COLOR_OXYGEN);
            drawIcon(x, y, -1, -1, 16, 18);
            drawValue(x, y, (air * 100 / 300) + "%");
        }

        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ------------------------------------------------------------------ helpers

    private int bgAlpha() {
        return (int) (MathHelper.clamp(RustHudConfig.backgroundOpacity, 0.0, 1.0) * 255.0);
    }

    private void drawPanel(int x, int y, int w) {
        int a = bgAlpha() << 24;
        Gui.drawRect(x, y, x + w, y + ROW_HEIGHT, a | 0x1A1A1A);
        Gui.drawRect(x, y, x + ROW_HEIGHT, y + ROW_HEIGHT, a | 0x2C2C2C);
        Gui.drawRect(barLeft(x), y + 2, barRight(x, w), y + ROW_HEIGHT - 2, a | 0x3A3A3A);
    }

    private static int barLeft(int x) {
        return x + ROW_HEIGHT + 2;
    }

    private static int barRight(int x, int w) {
        return x + w - 2;
    }

    private void drawBar(int x, int y, int w, float fraction, int color) {
        int left = barLeft(x);
        int fill = Math.round((barRight(x, w) - left) * MathHelper.clamp(fraction, 0f, 1f));
        if (fill > 0) Gui.drawRect(left, y + 2, left + fill, y + ROW_HEIGHT - 2, color);
    }

    /** Thin 2px strip at the top or bottom of the bar (absorption / saturation). */
    private void drawStrip(int x, int y, int w, float fraction, int color, boolean top) {
        int left = barLeft(x);
        int fill = Math.round((barRight(x, w) - left) * MathHelper.clamp(fraction, 0f, 1f));
        if (fill <= 0) return;
        int y0 = top ? y + 2 : y + ROW_HEIGHT - 4;
        Gui.drawRect(left, y0, left + fill, y0 + 2, color);
    }

    private void drawIcon(int x, int y, int bgU, int bgV, int u, int v) {
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.enableBlend();
        int ix = x + (ROW_HEIGHT - 9) / 2;
        int iy = y + (ROW_HEIGHT - 9) / 2 + 1;
        if (bgU >= 0) mc.ingameGUI.drawTexturedModalRect(ix, iy, bgU, bgV, 9, 9);
        mc.ingameGUI.drawTexturedModalRect(ix, iy, u, v, 9, 9);
    }

    private void drawValue(int x, int y, String text) {
        FontRenderer font = mc.fontRenderer;
        font.drawStringWithShadow(text, barLeft(x) + 3, y + 3, COLOR_TEXT);
    }

    private void drawValueRight(int x, int y, int w, String text, int color) {
        FontRenderer font = mc.fontRenderer;
        font.drawStringWithShadow(text, barRight(x, w) - 3 - font.getStringWidth(text), y + 3, color);
    }

    private static String formatNumber(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) return String.valueOf(Math.round(value));
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static float approach(float current, float target, float k) {
        float next = current + (target - current) * Math.min(1f, k);
        return Math.abs(next - target) < 0.01f ? target : next;
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a = (int) (((c1 >>> 24) & 0xFF) + ((((c2 >>> 24) & 0xFF) - ((c1 >>> 24) & 0xFF)) * t));
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
